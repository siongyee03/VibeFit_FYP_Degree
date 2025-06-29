package com.example.vibefitapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class UserPostsActivity extends AppCompatActivity {

    private TextView myPostsTitle;
    private FrameLayout notificationContainer;
    private RecyclerView recyclerViewMyPosts;
    private PostAdapter adapter;
    private List<Post> postList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final String TAG = "UserPostsActivity";

    private View notificationDot;
    private ListenerRegistration notificationListenerRegistration;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_posts);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            if (notificationContainer.getVisibility() == View.VISIBLE) {
                hideNotificationFragment();
            } else {
                finish();
            }
        });

        ImageButton btnNotification = findViewById(R.id.btn_notification);
        notificationDot = findViewById(R.id.notification_dot);
        notificationContainer = findViewById(R.id.notification_container);

        myPostsTitle = findViewById(R.id.my_posts_title);
        recyclerViewMyPosts = findViewById(R.id.recyclerViewMyPosts);
        recyclerViewMyPosts.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        adapter = new PostAdapter(this, postList, postId -> {});
        recyclerViewMyPosts.setAdapter(adapter);

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::loadUserPosts);

        loadUserPosts();

        setupUnreadNotificationListener();

        btnNotification.setOnClickListener(v -> {
            notificationDot.setVisibility(View.GONE);
            myPostsTitle.setText(R.string.notification);
            recyclerViewMyPosts.setVisibility(View.GONE);
            notificationContainer.setVisibility(View.VISIBLE);
            btnNotification.setVisibility(View.GONE);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.notification_container, new NotificationFragment());
            transaction.commit();
        });
    }

    private void hideNotificationFragment() {
        myPostsTitle.setText(R.string.my_posts);
        recyclerViewMyPosts.setVisibility(View.VISIBLE);
        notificationContainer.setVisibility(View.GONE);
    }

    private void setupUnreadNotificationListener() {
        if (auth.getCurrentUser() == null) {
            Log.d(TAG, "No current user, cannot set up notification listener.");
            notificationDot.setVisibility(View.GONE); // Ensure dot is hidden if not logged in
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();

        // Listen for real-time changes in unread notifications
        notificationListenerRegistration = db.collection("notifications")
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("read", false) // Only listen for unread notifications
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen for unread notifications failed.", e);
                        notificationDot.setVisibility(View.GONE); // Hide dot on error
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Log.d(TAG, "Unread notifications found: " + snapshots.size());
                        notificationDot.setVisibility(View.VISIBLE); // Show the dot
                    } else {
                        Log.d(TAG, "No unread notifications.");
                        notificationDot.setVisibility(View.GONE); // Hide the dot
                    }
                });
    }

    private void loadUserPosts() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("posts")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> newPosts = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setId(doc.getId());
                            newPosts.add(post);
                        }
                    }

                    if (newPosts.isEmpty()) {
                        Toast.makeText(this, "Looks like you haven't posted anything. Start sharing your vibe!", Toast.LENGTH_SHORT).show();
                    }

                    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PostDiffCallback(postList, newPosts));
                    postList.clear();
                    postList.addAll(newPosts);
                    diffResult.dispatchUpdatesTo(adapter);

                    SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
                    swipeRefreshLayout.setRefreshing(false);
                })

                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user posts", e);
                    Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show();

                    SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.remove();
            Log.d(TAG, "Notification listener removed in onDestroy.");
        }
    }

    public static class PostDiffCallback extends DiffUtil.Callback {
        private final List<Post> oldList, newList;

        public PostDiffCallback(List<Post> oldList, List<Post> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
