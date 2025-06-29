package com.example.vibefitapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PostDetailActivity extends AppCompatActivity implements CommentAdapter.OnCommentActionListener {

    private ImageView avatar, likeIcon, favoriteIcon;
    private TextView username, title, description, likeCount, pageIndicator, favouriteCount, commentCount;
    private ViewPager2 mediaViewPager;
    private Post post;
    private boolean isLiked = false;
    private boolean isFavorited = false;
    private FirebaseFirestore db;
    private String uid;
    // Comment functionality
    private RecyclerView commentsRecyclerView;
    private CommentAdapter commentAdapter;
    private EditText commentEditText;
    private ImageButton sendCommentButton;
    private ListenerRegistration commentsListenerRegistration;
    private String replyToCommentId = null;
    private String replyToCommentAuthorId = null;
    private String currentUserProfileImageUrl;
    private String currentUsernameForComments;
    private boolean isLikingInProgress = false;
    private TextView forumTopicLabel, forumTopicText;
    private TextView forumDetailsLabel, forumStepsText;
    private TextView forumDifficultyLabel, forumDifficultyText;
    private FrameLayout mediaViewPagerContainer;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        post = getIntent().getParcelableExtra("post");
        if (post == null) {
            Log.e("PostDetail", "Post is null");
            Toast.makeText(this, "Post data is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = auth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        avatar = findViewById(R.id.detail_user_avatar);
        username = findViewById(R.id.detail_username);
        title = findViewById(R.id.detail_title);
        description = findViewById(R.id.detail_description);
        likeCount = findViewById(R.id.detail_like_count);
        likeIcon = findViewById(R.id.detail_like_icon);
        favoriteIcon = findViewById(R.id.detail_fav_icon);
        favouriteCount = findViewById(R.id.detail_fav_count);
        TextView timestampView = findViewById(R.id.detail_timestamp);
        ImageButton moreActions = findViewById(R.id.detail_more_actions);
        mediaViewPager = findViewById(R.id.media_viewpager);
        pageIndicator = findViewById(R.id.page_indicator);
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        mediaViewPagerContainer = findViewById(R.id.media_viewpager_container);

        // Initialize comment views
        commentsRecyclerView = findViewById(R.id.comments_recycler_view);
        commentEditText = findViewById(R.id.comment_edit_text);
        sendCommentButton = findViewById(R.id.send_comment_button);
        commentCount = findViewById(R.id.detail_comment_count);
        LinearLayout commentActionContainer = findViewById(R.id.comment_action_container);

        forumTopicLabel = findViewById(R.id.forum_topic_label);
        forumTopicText = findViewById(R.id.forum_topic_text);
        forumDetailsLabel = findViewById(R.id.forum_details_label);
        forumStepsText = findViewById(R.id.forum_steps_text);
        forumDifficultyLabel = findViewById(R.id.forum_difficulty_label);
        forumDifficultyText = findViewById(R.id.forum_difficulty_text);

        // Setup comments RecyclerView
        commentAdapter = new CommentAdapter(this, post.getUserId(), this);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentAdapter);

        // Fetch user's current avatar and username for commenting
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserProfileImageUrl = documentSnapshot.getString("profileImageUrl");
                        currentUsernameForComments = documentSnapshot.getString("username");

                        if (currentUserProfileImageUrl == null || currentUserProfileImageUrl.isEmpty()) {
                            currentUserProfileImageUrl = null;
                        }

                        if (currentUsernameForComments == null || currentUsernameForComments.isEmpty()) {
                            currentUsernameForComments = "Anonymous";
                        }
                    }  else {
                        Log.e("PostDetail", "Current user data not found for comments.");
                    }
                })
                .addOnFailureListener(e -> Log.e("PostDetail", "Error fetching current user data for comments: " + e.getMessage()));


        // Handle sending comments
        sendCommentButton.setOnClickListener(v -> postComment());
        commentEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                postComment();
                return true;
            }
            return false;
        });

        // Click listener for the comment icon to scroll to comments section
        commentActionContainer.setOnClickListener(v -> {
            ScrollView scrollView = findViewById(R.id.scroll_container);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });


        // user avatar
        String avatarUrl = post.getUserAvatar();
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            avatar.setImageResource(R.drawable.ic_avatar_placeholder);
        } else {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(avatar);
        }

        username.setText(post.getUsername());
        title.setText(post.getTitle());
        description.setText(post.getContent());
        likeCount.setText(String.valueOf(post.getLikeCount()));
        favouriteCount.setText(String.valueOf(post.getFavouriteCount()));

        if (!"forum".equalsIgnoreCase(post.getCategory())) {
            if (post.getMediaUrls() == null || post.getMediaUrls().isEmpty()) {
                Toast.makeText(this, "This post doesn't contain any media.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        if (post.getTimestamp() != null) {
            String timeText = android.text.format.DateFormat.format("yyyy-MM-dd hh:mm a", post.getTimestamp().toDate()).toString();
            timestampView.setText(getString(R.string.posted_on, timeText));
        }

        setupLikeAndFavorite();

        if ("forum".equalsIgnoreCase(post.getCategory())) {
            // Hide tutorial/pattern specific content
            mediaViewPagerContainer.setVisibility(View.GONE);
            mediaViewPager.setVisibility(View.GONE);
            pageIndicator.setVisibility(View.GONE);
            title.setVisibility(View.GONE);
            description.setVisibility(View.GONE);

            // Show forum specific content and populate
            forumTopicLabel.setVisibility(View.VISIBLE);
            forumTopicText.setVisibility(View.VISIBLE);
            forumTopicText.setText(post.getForumTopic());

            forumDetailsLabel.setVisibility(View.VISIBLE);
            forumStepsText.setVisibility(View.VISIBLE);
            forumStepsText.setText(post.getForumSteps()); // Use getForumSteps() for details

            forumDifficultyLabel.setVisibility(View.VISIBLE);
            forumDifficultyText.setVisibility(View.VISIBLE);
            forumDifficultyText.setText(post.getForumDifficulty());

        } else { // This is a "tutorial" or "pattern" post
            // Show tutorial/pattern specific content and populate
            mediaViewPagerContainer.setVisibility(View.VISIBLE);
            title.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);

            title.setText(post.getTitle());
            description.setText(post.getContent()); // Use getContent() for description

            // Hide forum specific content
            forumTopicLabel.setVisibility(View.GONE);
            forumTopicText.setVisibility(View.GONE);
            forumDetailsLabel.setVisibility(View.GONE);
            forumStepsText.setVisibility(View.GONE);
            forumDifficultyLabel.setVisibility(View.GONE);
            forumDifficultyText.setVisibility(View.GONE);

            // Handle media display for tutorial/pattern posts
            if (post.getMediaUrls() == null || post.getMediaUrls().isEmpty()) {
                Toast.makeText(this, "This post doesn't contain any media.", Toast.LENGTH_SHORT).show();
                mediaViewPager.setVisibility(View.GONE);
                pageIndicator.setVisibility(View.GONE);
            } else {
                MediaPagerAdapter adapter = new MediaPagerAdapter(post.getMediaUrls(), this);
                mediaViewPager.setAdapter(adapter);
                mediaViewPager.setVisibility(View.VISIBLE);
                pageIndicator.setVisibility(View.VISIBLE);

                int currentPage = 1;
                int totalPages = post.getMediaUrls().size();
                String indicator = getString(R.string.media_page_indicator, currentPage, totalPages);
                pageIndicator.setText(indicator);

                mediaViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        int currentPage = position + 1;
                        int totalPages = post.getMediaUrls().size();
                        String indicator = getString(R.string.media_page_indicator, currentPage, totalPages);
                        pageIndicator.setText(indicator);
                    }
                });
            }
        }

        if (!uid.equals(post.getUserId())) {
            moreActions.setVisibility(View.GONE);
        } else {
            moreActions.setVisibility(View.VISIBLE);
        }

        moreActions.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(PostDetailActivity.this, moreActions);
            popupMenu.getMenuInflater().inflate(R.menu.post_actions_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_edit) {
                    Intent intent = new Intent(PostDetailActivity.this, UploadPostActivity.class);
                    intent.putExtra("editing_post", post);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.action_delete) {
                    new AlertDialog.Builder(PostDetailActivity.this)
                            .setTitle("Delete Post")
                            .setMessage("Are you sure you want to delete this post?")
                            .setPositiveButton("Delete", (dialog, which) -> deletePost())
                            .setNegativeButton("Cancel", null)
                            .show();
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });

        // Load comments
        loadComments();
        listenForCommentCountChanges();

        View commentInputLayout = findViewById(R.id.comment_input_layout);
        ViewCompat.setOnApplyWindowInsetsListener(commentInputLayout, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
            return insets;
        });

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshPostDetail();
            swipeRefreshLayout.setRefreshing(false);
        });


    }

    private void postComment() {
        String commentContent = commentEditText.getText().toString().trim();
        if (TextUtils.isEmpty(commentContent)) {
            Toast.makeText(this, "Comment cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUsernameForComments == null) {
            Toast.makeText(this, "User data not loaded, please try again.", Toast.LENGTH_SHORT).show();
            Log.e("PostDetail", "User profile data not available for commenting.");
            return;
        }

        commentEditText.setEnabled(false);

        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("postId", post.getId());
        commentData.put("userId", currentUserId);
        commentData.put("username", currentUsernameForComments);
        commentData.put("userAvatarUrl", currentUserProfileImageUrl);
        commentData.put("content", commentContent);
        commentData.put("timestamp", FieldValue.serverTimestamp());

        final String capturedReplyToCommentId = this.replyToCommentId;
        final String capturedReplyToCommentAuthorId = this.replyToCommentAuthorId;

        if (capturedReplyToCommentId != null && !capturedReplyToCommentId.isEmpty()) {
            commentData.put("parentCommentId", capturedReplyToCommentId);
        } else {
            commentData.put("parentCommentId", null);
        }

        db.collection("posts").document(post.getId())
                .collection("comments")
                .add(commentData)
                .addOnSuccessListener(documentReference -> {
                    commentEditText.setText("");
                    commentEditText.setEnabled(true);

                    // Reset reply state
                    replyToCommentId = null;
                    replyToCommentAuthorId = null;

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(commentEditText.getWindowToken(), 0);

                    Log.d("CommentCountDebug", "Comment added successfully. Incrementing commentCount for post: " + post.getId());

                    db.collection("posts").document(post.getId())
                            .update("commentCount", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid -> Log.d("Comment", "Comment count updated."))
                            .addOnFailureListener(e -> Log.e("Comment", "Error updating comment count", e));

                    handleCommentNotification(currentUserId, commentContent, documentReference.getId(),
                            capturedReplyToCommentId, capturedReplyToCommentAuthorId);


                    commentsRecyclerView.post(() -> {
                        if (commentAdapter.getItemCount() > 0) {
                            commentsRecyclerView.smoothScrollToPosition(commentAdapter.getItemCount() - 1);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PostDetailActivity.this, "Couldn't post your comment. Try again in a moment.", Toast.LENGTH_SHORT).show();
                    Log.e("Comment", "Error adding comment", e);
                    commentEditText.setEnabled(true);
                });
    }

    private void handleCommentNotification(String currentUserId, String commentContent, String newCommentId,
                                           String actualReplyToCommentId, String actualReplyToCommentAuthorId) {

        List<Map<String, Object>> notificationsToSend = new ArrayList<>();

        // Reply to a comment
        if (actualReplyToCommentId != null && actualReplyToCommentAuthorId != null) {
            // Notify the original comment author if it's not the current user
            if (!currentUserId.equals(actualReplyToCommentAuthorId)) {
                Map<String, Object> replyNotification = createNotification(
                        actualReplyToCommentAuthorId, currentUserId, commentContent, "reply", newCommentId, actualReplyToCommentId
                );
                notificationsToSend.add(replyNotification);
                Log.d("Notification", "Reply notification prepared for original comment author: " + actualReplyToCommentAuthorId);
            }

            // Additionally notify the post author if the reply is on their post
            // AND the post author is different from both the current user and the original comment author
            if (!currentUserId.equals(post.getUserId()) && !post.getUserId().equals(actualReplyToCommentAuthorId)) {
                Map<String, Object> postAuthorReplyNotification = createNotification(
                        post.getUserId(), currentUserId, commentContent, "reply_on_post", newCommentId, actualReplyToCommentId
                );
                notificationsToSend.add(postAuthorReplyNotification);
                Log.d("Notification", "Reply-on-post notification prepared for post author: " + post.getUserId());
            }
        }
        // New comment on post (not a reply)
        else if (!currentUserId.equals(post.getUserId())) { // Only notify if current user is not the post author
            Map<String, Object> commentNotification = createNotification(
                    post.getUserId(), currentUserId, commentContent, "comment", newCommentId, null
            );
            notificationsToSend.add(commentNotification);
            Log.d("Notification", "New comment notification prepared for post author: " + post.getUserId());
        } else {
            Log.d("Notification", "No notification generated (self-comment/reply or no target user).");
        }

        for (Map<String, Object> data : notificationsToSend) {
            db.collection("notifications")
                    .add(data)
                    .addOnSuccessListener(docRef ->
                            Log.d("Notification", "Notification added successfully for user: " + data.get("toUserId") + " Type: " + data.get("type"))
                    )
                    .addOnFailureListener(e ->
                            Log.e("Notification", "Error adding notification: " + e.getMessage())
                    );
        }
    }

    private Map<String, Object> createNotification(String toUserId, String fromUserId, String commentContent,
                                                   String type, String commentId, String originalCommentId) {
        String message;
        switch (type) {
            case "reply":
                message = currentUsernameForComments + " replied to your comment: \"" + commentContent + "\"";
                break;
            case "reply_on_post":
                message = currentUsernameForComments + " replied to a comment on your post: \"" + commentContent + "\"";
                break;
            case "comment":
            default:
                message = currentUsernameForComments + " commented on your post: \"" + commentContent + "\"";
                break;
        }

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("toUserId", toUserId);
        notificationData.put("fromUserId", fromUserId);
        notificationData.put("fromUsername", currentUsernameForComments);
        notificationData.put("fromUserAvatarUrl", currentUserProfileImageUrl);
        notificationData.put("type", type);
        notificationData.put("postId", post.getId());
        notificationData.put("postTitle", post.getTitle());
        notificationData.put("commentId", commentId);
        notificationData.put("originalCommentId", originalCommentId);
        notificationData.put("content", message);
        notificationData.put("timestamp", FieldValue.serverTimestamp());
        notificationData.put("read", false);

        return notificationData;
    }


    private void loadComments() {
        Log.d("CommentDebug", "loadComments() called. Post ID: " + post.getId());
        commentsListenerRegistration = db.collection("posts").document(post.getId())
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.w("CommentDebug", "Listen failed or snapshot null", e);
                        return;
                    }

                    Map<String, List<Comment>> replyMap = new HashMap<>();
                    List<Comment> topLevelComments = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment == null) continue;
                        Log.d("CommentDebug", "Comment: " + comment.getContent() + ", parentId: " + comment.getParentCommentId());

                        comment.setId(doc.getId());

                        String parentId = comment.getParentCommentId();
                        if (parentId == null || parentId.isEmpty()) {
                            topLevelComments.add(comment);
                        } else {
                            replyMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(comment);
                        }
                    }

                    List<Comment> flatCommentList = new ArrayList<>();
                    // Sort top-level comments by timestamp ascending to maintain order
                    topLevelComments.sort((c1, c2) -> {
                        if (c1.getTimestamp() == null && c2.getTimestamp() == null) return 0;
                        if (c1.getTimestamp() == null) return -1;
                        if (c2.getTimestamp() == null) return 1;
                        return c1.getTimestamp().compareTo(c2.getTimestamp());
                    });

                    for (Comment parent : topLevelComments) {
                        parent.setIndentLevel(0);
                        flatCommentList.add(parent);
                        // Sort replies for consistent ordering
                        List<Comment> replies = replyMap.get(parent.getId());
                        if (replies != null) {
                            replies.sort((r1, r2) -> {
                                if (r1.getTimestamp() == null && r2.getTimestamp() == null)
                                    return 0;
                                if (r1.getTimestamp() == null) return -1;
                                if (r2.getTimestamp() == null) return 1;
                                return r1.getTimestamp().compareTo(r2.getTimestamp());
                            });
                        }
                        addRepliesRecursively(parent, replyMap, flatCommentList, 1);
                    }

                    commentAdapter.updateComments(flatCommentList);

                    commentsRecyclerView.post(() -> {
                        if (commentAdapter.getItemCount() > 0) {
                            commentsRecyclerView.smoothScrollToPosition(commentAdapter.getItemCount() - 1);
                        }
                    });
                });
    }

    private void addRepliesRecursively(Comment parent, Map<String, List<Comment>> replyMap, List<Comment> flatList, int level) {
        List<Comment> replies = replyMap.get(parent.getId());
        if (replies == null) return;
        Log.d("ReplyMapping", "Looking for replies for: " + parent.getId());

        for (Comment reply : replies) {
            Log.d("ReplyMapping", "  → Reply found: " + reply.getContent() + " (parentId: " + reply.getParentCommentId() + ")");

            reply.setIndentLevel(level);
            flatList.add(reply);
            addRepliesRecursively(reply, replyMap, flatList, level + 1);
        }
    }

    private void listenForCommentCountChanges() {
        db.collection("posts").document(post.getId())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("PostDetail", "Listen for comment count failed: " + error.getMessage());
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Long count = snapshot.getLong("commentCount");
                        if (count != null) {
                            post.setCommentCount(count.intValue());
                            commentCount.setText(String.valueOf(count));
                        } else {
                            post.setCommentCount(0);
                            commentCount.setText("0");
                        }
                    } else {
                        post.setCommentCount(0);
                        commentCount.setText("0");
                    }
                });
    }


    @Override
    public void onReplyClick(Comment comment) {
        replyToCommentId = comment.getId();
        replyToCommentAuthorId = comment.getUserId();

        String hintText = getString(R.string.replying_to, comment.getUsername());
        commentEditText.setText(hintText);
        commentEditText.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(commentEditText, InputMethodManager.SHOW_IMPLICIT);

        commentEditText.setSelection(commentEditText.getText().length());
    }

    @Override
    public void onDeleteClick(Comment comment) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> deleteComment(comment))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteComment(Comment commentToDelete) {
        String commentId = commentToDelete.getId();
        String postId = post.getId();

        if (commentId == null || postId == null) {
            Toast.makeText(this, "Error: Comment or Post ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();

        // 1. Delete parent comment
        batch.delete(db.collection("posts").document(postId).collection("comments").document(commentId));
        Log.d("CommentDelete", "Added parent comment " + commentId + " to batch for deletion.");

        // 2. Query and delete child comments
        db.collection("posts").document(postId)
                .collection("comments")
                .whereEqualTo("parentCommentId", commentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> childCommentIds = new ArrayList<>();
                    for (QueryDocumentSnapshot childDoc : queryDocumentSnapshots) {
                        batch.delete(childDoc.getReference());
                        childCommentIds.add(childDoc.getId());
                        Log.d("CommentDelete", "Added child comment " + childDoc.getId() + " to batch for deletion.");
                    }

                    // 3. Commit comment deletions
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(PostDetailActivity.this, "Comment(s) deleted successfully.", Toast.LENGTH_SHORT).show();
                                Log.d("CommentDelete", "Batch deletion successful.");

                                // 4. Recount comment count
                                db.collection("posts").document(postId)
                                        .collection("comments")
                                        .get()
                                        .addOnSuccessListener(snapshot -> {
                                            long count = snapshot.size();
                                            db.collection("posts").document(postId)
                                                    .update("commentCount", count)
                                                    .addOnSuccessListener(unused -> Log.d("CommentDelete", "Comment count updated to " + count))
                                                    .addOnFailureListener(e -> Log.e("CommentDelete", "Failed to update comment count", e));
                                        });

                                // 5. Delete related notifications
                                List<String> commentIdsToRemove = new ArrayList<>(childCommentIds);
                                commentIdsToRemove.add(commentId);

                                for (String cId : commentIdsToRemove) {
                                    db.collection("notifications")
                                            .whereEqualTo("commentId", cId)
                                            .get()
                                            .addOnSuccessListener(notifSnapshots -> {
                                                WriteBatch notifBatch = db.batch();
                                                for (QueryDocumentSnapshot notifDoc : notifSnapshots) {
                                                    notifBatch.delete(notifDoc.getReference());
                                                    Log.d("CommentDelete", "Deleted notification with commentId: " + cId);
                                                }
                                                notifBatch.commit()
                                                        .addOnSuccessListener(unused -> Log.d("CommentDelete", "Notification batch committed"))
                                                        .addOnFailureListener(e -> Log.e("CommentDelete", "Failed to delete notifications for commentId " + cId, e));
                                            })
                                            .addOnFailureListener(e -> Log.e("CommentDelete", "Failed to query notifications for commentId " + cId, e));
                                }

                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(PostDetailActivity.this, "Failed to delete comments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("CommentDelete", "Error committing batch", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PostDetailActivity.this, "Failed to query child comments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("CommentDelete", "Error querying child comments", e);
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentsListenerRegistration != null) {
            commentsListenerRegistration.remove();
            Log.d("CommentDebug", "commentsListenerRegistration removed in onDestroy.");
        }
    }

    private void setupLikeAndFavorite() {
        String postId = post.getId();
        if (postId == null) return;

        likeIcon.setEnabled(false);
        favoriteIcon.setEnabled(false);

        db.collection("posts").document(postId)
                .collection("likedBy").document(uid).get()
                .addOnSuccessListener(doc -> {
                    isLiked = doc.exists();
                    likeIcon.setImageResource(isLiked ? R.drawable.ic_heart_red : R.drawable.ic_like_outline);
                    likeIcon.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("LikeStatus", "Error loading like status", e);
                    Toast.makeText(this, "Failed to load like status.", Toast.LENGTH_SHORT).show();
                    likeIcon.setEnabled(true);
                });

        db.collection("users").document(uid)
                .collection("favourites").document(postId).get()
                .addOnSuccessListener(doc -> {
                    isFavorited = doc.exists();
                    favoriteIcon.setImageResource(isFavorited ? R.drawable.ic_star_yellow : R.drawable.ic_star_outline);
                    favoriteIcon.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("Favourite Status", "Error loading favourite status", e);
                    Toast.makeText(this, "Failed to load favourite status.", Toast.LENGTH_SHORT).show();
                    favoriteIcon.setEnabled(true);
                });

        likeIcon.setOnClickListener(v -> {
            if (isLikingInProgress) return;
            isLikingInProgress = true;
            likeIcon.setEnabled(false);

            final boolean newLikeStatus = !isLiked;
            final int delta = newLikeStatus ? 1 : -1;
            DocumentReference postRef = db.collection("posts").document(postId);

            postRef.update("likeCount", FieldValue.increment(delta))
                    .addOnSuccessListener(aVoid -> {
                        if (newLikeStatus) {
                            postRef.collection("likedBy").document(uid)
                                    .set(Collections.singletonMap("likedAt", FieldValue.serverTimestamp()))
                                    .addOnSuccessListener(aVoid2 -> {
                                        isLiked = true;
                                        likeIcon.setImageResource(R.drawable.ic_heart_red);
                                        likeIcon.setEnabled(true);
                                        isLikingInProgress = false;
                                    })
                                    .addOnFailureListener(e -> {
                                        postRef.update("likeCount", FieldValue.increment(-delta)); // rollback
                                        Toast.makeText(this, "Failed to like post.", Toast.LENGTH_SHORT).show();
                                        likeIcon.setEnabled(true);
                                        isLikingInProgress = false;
                                    });
                        } else {
                            postRef.collection("likedBy").document(uid)
                                    .delete()
                                    .addOnSuccessListener(aVoid2 -> {
                                        isLiked = false;
                                        likeIcon.setImageResource(R.drawable.ic_like_outline);
                                        likeIcon.setEnabled(true);
                                        isLikingInProgress = false;
                                    })
                                    .addOnFailureListener(e -> {
                                        postRef.update("likeCount", FieldValue.increment(-delta)); // rollback
                                        Toast.makeText(this, "Failed to unlike post.", Toast.LENGTH_SHORT).show();
                                        likeIcon.setEnabled(true);
                                        isLikingInProgress = false;
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update like count.", Toast.LENGTH_SHORT).show();
                        likeIcon.setEnabled(true);
                        isLikingInProgress = false;
                    });
        });

        favoriteIcon.setOnClickListener(v -> {
            favoriteIcon.setEnabled(false);
            final boolean newFavStatus = !isFavorited;

            if (newFavStatus) {
                db.collection("users").document(uid)
                        .collection("favourites").document(postId)
                        .set(Collections.singletonMap("favouritedAt", FieldValue.serverTimestamp()))
                        .addOnSuccessListener(aVoid -> {
                            isFavorited = true;
                            favoriteIcon.setImageResource(R.drawable.ic_star_yellow);
                            db.collection("posts").document(postId)
                                    .update("favouriteCount", FieldValue.increment(1));
                            favoriteIcon.setEnabled(true);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to add favourite", Toast.LENGTH_SHORT).show();
                            favoriteIcon.setEnabled(true);
                        });
            } else {
                db.collection("users").document(uid)
                        .collection("favourites").document(postId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            isFavorited = false;
                            favoriteIcon.setImageResource(R.drawable.ic_star_outline);
                            db.collection("posts").document(postId)
                                    .update("favouriteCount", FieldValue.increment(-1));
                            favoriteIcon.setEnabled(true);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to remove favourite", Toast.LENGTH_SHORT).show();
                            favoriteIcon.setEnabled(true);
                        });
            }
        });

        db.collection("posts").document(postId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;
                    if (snapshot != null && snapshot.exists()) {
                        Long likeCnt = snapshot.getLong("likeCount");
                        Long favCnt = snapshot.getLong("favouriteCount");
                        if (likeCnt != null) {
                            likeCount.setText(String.valueOf(likeCnt));
                        }
                        if (favCnt != null) {
                            favouriteCount.setText(String.valueOf(favCnt));
                        }
                    }
                });
    }

    private void deletePost() {
        String postId = post.getId();
        if (postId == null) return;

        db.collection("posts").document(postId)
                .collection("likedBy")
                .get()
                .addOnSuccessListener(likedSnapshot -> {
                    WriteBatch likedBatch = db.batch();
                    for (DocumentSnapshot doc : likedSnapshot.getDocuments()) {
                        likedBatch.delete(doc.getReference());
                    }
                    likedBatch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("PostDelete", "LikedBy documents deleted.");

                                db.collection("posts").document(postId)
                                        .collection("comments")
                                        .get()
                                        .addOnSuccessListener(commentsSnapshot -> {
                                            WriteBatch commentsBatch = db.batch();
                                            for (QueryDocumentSnapshot commentDoc : commentsSnapshot) {
                                                commentsBatch.delete(commentDoc.getReference());
                                            }
                                            commentsBatch.commit()
                                                    .addOnSuccessListener(unused -> {
                                                        Log.d("PostDelete", "Comments deleted.");

                                                        db.collection("notifications")
                                                                .whereEqualTo("postId", postId)
                                                                .get()
                                                                .addOnSuccessListener(notifSnapshot -> {
                                                                    WriteBatch notifBatch = db.batch();
                                                                    for (DocumentSnapshot notifDoc : notifSnapshot.getDocuments()) {
                                                                        notifBatch.delete(notifDoc.getReference());
                                                                    }
                                                                    notifBatch.commit()
                                                                            .addOnSuccessListener(n -> {
                                                                                Log.d("PostDelete", "Notifications deleted.");
                                                                                continuePostDeletion(postId);
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                Log.e("PostDelete", "Failed to delete notifications", e);
                                                                                Toast.makeText(this, "Failed to delete post notifications", Toast.LENGTH_SHORT).show();
                                                                            });
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e("PostDelete", "Error getting notifications", e);
                                                                    Toast.makeText(this, "Failed to get notifications", Toast.LENGTH_SHORT).show();
                                                                });

                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("PostDelete", "Error deleting comments", e);
                                                        Toast.makeText(this, "Failed to delete post comments", Toast.LENGTH_SHORT).show();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("PostDelete", "Error getting comments", e);
                                            Toast.makeText(this, "Failed to fetch comments", Toast.LENGTH_SHORT).show();
                                        });

                            })
                            .addOnFailureListener(e -> {
                                Log.e("PostDelete", "Error deleting likedBy", e);
                                Toast.makeText(this, "Failed to delete post likes", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("PostDelete", "Error getting likedBy", e);
                    Toast.makeText(this, "Failed to fetch post likes", Toast.LENGTH_SHORT).show();
                });
    }

    private void continuePostDeletion(String postId) {
        finish();

        deletePostFromUserFavourites(postId, () -> db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(unused -> {

                    for (String mediaUrl : post.getMediaUrls()) {
                        if (mediaUrl != null && mediaUrl.startsWith("https://firebasestorage.googleapis.com")) {
                            try {
                                StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(mediaUrl);
                                ref.delete()
                                        .addOnSuccessListener(unused2  -> Log.d("PostDelete", "Deleted media: " + mediaUrl))
                                        .addOnFailureListener(e -> Log.w("PostDelete", "Failed to delete media: " + mediaUrl, e));
                            } catch (IllegalArgumentException e) {
                                Log.w("PostDelete", "Invalid Firebase Storage URL: " + mediaUrl, e);
                            }
                        } else {
                            Log.w("PostDelete", "Skipping non-storage media URL: " + mediaUrl);
                        }
                    }
                    Toast.makeText(PostDetailActivity.this, "Post deleted successfully!", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Log.e("PostDelete", "Error deleting post document", e);
                    Toast.makeText(PostDetailActivity.this, "Failed to delete post document", Toast.LENGTH_SHORT).show();
                }));
    }

    private void deletePostFromUserFavourites(String postId, Runnable onComplete) {
        db.collection("users")
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    var batch = db.batch();
                    for (var userDoc : usersSnapshot.getDocuments()) {
                        batch.delete(userDoc.getReference().collection("favourites").document(postId));
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("PostDelete", "Attempted to delete favorite entries for post $postId for all users.");
                                onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.w("PostDelete", "Failed to delete favourite batch for post $postId", e);
                                onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("PostDelete", "Error querying users for favourites deletion", e);
                    onComplete.run();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (post != null && post.getId() != null) {
            db.collection("posts").document(post.getId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Post updatedPost = documentSnapshot.toObject(Post.class);
                            if (updatedPost != null) {
                                updatedPost.setId(post.getId());
                                this.post = updatedPost;
                                updatePostUI();
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("PostDetail", "Failed to fetch updated post", e));
        }
    }

    private void updatePostUI() {
        title.setText(post.getTitle());
        description.setText(post.getContent());
        likeCount.setText(String.valueOf(post.getLikeCount()));
        favouriteCount.setText(String.valueOf(post.getFavouriteCount()));

        String avatarUrl = post.getUserAvatar();
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            avatar.setImageResource(R.drawable.ic_avatar_placeholder);
        } else {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(avatar);
        }

        username.setText(post.getUsername());

        if (post.getTimestamp() != null) {
            String timeText = android.text.format.DateFormat.format("yyyy-MM-dd hh:mm a", post.getTimestamp().toDate()).toString();
            TextView timestampView = findViewById(R.id.detail_timestamp);
            timestampView.setText(getString(R.string.posted_on, timeText));
        }

        if (post.getMediaUrls() != null && !post.getMediaUrls().isEmpty()) {
            mediaViewPager.setAdapter(new MediaPagerAdapter(post.getMediaUrls(), this));
            pageIndicator.setText(getString(R.string.media_page_indicator, 1, post.getMediaUrls().size()));
        }

        commentCount.setText(String.valueOf(post.getCommentCount()));
    }

    private void refreshPostDetail() {
        if (post != null && post.getId() != null) {
            db.collection("posts").document(post.getId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Post updatedPost = documentSnapshot.toObject(Post.class);
                            if (updatedPost != null) {
                                updatedPost.setId(post.getId());
                                this.post = updatedPost;
                                updatePostUI();
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("PostDetail", "Failed to refresh post", e));
        }
    }
}