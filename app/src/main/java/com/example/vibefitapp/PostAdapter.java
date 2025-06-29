package com.example.vibefitapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import android.content.Intent;
import android.widget.Toast;

import java.util.HashMap;


public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final Context context;
    private final List<Post> postList;
    private final OnPostVisibilityChangedListener visibilityListener;


    public PostAdapter(Context context, List<Post> postList, OnPostVisibilityChangedListener listener) {
        this.context = context;
        this.postList = postList;
        this.visibilityListener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);

        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.username.setText(post.getUsername());
        Glide.with(context).load(post.getUserAvatar())
                .load(post.getUserAvatar())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(holder.avatar);

        if ("forum".equalsIgnoreCase(post.getCategory())) {
            // Hide tutorial/pattern specific elements
            holder.media.setVisibility(View.GONE);
            holder.title.setVisibility(View.GONE);
            holder.description.setVisibility(View.GONE);

            // Show forum specific elements and populate
            holder.forumTopicPreview.setVisibility(View.VISIBLE);
            holder.forumDifficultyPreview.setVisibility(View.VISIBLE);

            holder.forumTopicPreview.setText(context.getString(R.string.forum_topic_prefix, post.getForumTopic()));
            holder.forumDifficultyPreview.setText(context.getString(R.string.forum_difficulty_prefix, post.getForumDifficulty()));

        } else {
            holder.media.setVisibility(View.VISIBLE);
            holder.title.setVisibility(View.VISIBLE);
            holder.description.setVisibility(View.VISIBLE);

            String previewUrl = post.getMediaUrls() != null && !post.getMediaUrls().isEmpty() ? post.getMediaUrls().get(0) : null;
            Glide.with(context)
                    .load(previewUrl != null ? previewUrl : R.drawable.image_placeholder)
                    .into(holder.media);

            holder.title.setText(post.getTitle());
            holder.description.setText(post.getContent());

            // Hide forum specific elements
            holder.forumTopicPreview.setVisibility(View.GONE);
            holder.forumDifficultyPreview.setVisibility(View.GONE);
        }

        holder.likeCount.setText(String.valueOf(post.getLikeCount()));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("post", post);
            context.startActivity(intent);
        });

        if (post.getId() == null || post.getId().isEmpty()) {
            Log.e("PostAdapter", "post.getId() is null or empty — cannot bind Firestore doc.");
            holder.likeIcon.setVisibility(View.GONE);
            holder.likeCount.setVisibility(View.GONE);
            return;
        }

        if (auth.getCurrentUser() == null) {
            holder.likeIcon.setImageResource(R.drawable.ic_like_outline);
            holder.likeIcon.setTag("not_logged_in");
            holder.likeCount.setVisibility(View.GONE);
            holder.likeIcon.setOnClickListener(v -> Toast.makeText(context, "Please login to like this post.", Toast.LENGTH_SHORT).show());
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        DocumentReference postRef = db.collection("posts").document(post.getId());
        DocumentReference likedByRef = postRef.collection("likedBy").document(currentUserId);

        likedByRef.get().addOnSuccessListener(doc -> {
            boolean isLiked = doc.exists();
            holder.likeIcon.setImageResource(isLiked ? R.drawable.ic_heart_red : R.drawable.ic_like_outline);
            holder.likeIcon.setTag(isLiked ? "liked" : "not_liked");
        });

        holder.likeIcon.setOnClickListener(v -> {
            holder.likeIcon.setEnabled(false);

            likedByRef.get().addOnSuccessListener(doc -> {
                boolean isLiked = doc.exists();
                if (isLiked) {
                    likedByRef.delete().addOnSuccessListener(aVoid -> {
                        postRef.update("likeCount", FieldValue.increment(-1));
                        holder.likeIcon.setImageResource(R.drawable.ic_like_outline);
                        int newCount = Math.max(0, post.getLikeCount() - 1);
                        post.setLikeCount(newCount);
                        holder.likeCount.setText(String.valueOf(newCount));
                        holder.likeIcon.setEnabled(true);
                    });
                } else {
                    likedByRef.set(new HashMap<String, Object>() {{
                        put("likedAt", FieldValue.serverTimestamp());
                    }}).addOnSuccessListener(aVoid -> {
                        postRef.update("likeCount", FieldValue.increment(1));
                        holder.likeIcon.setImageResource(R.drawable.ic_heart_red);
                        int newCount = post.getLikeCount() + 1;
                        post.setLikeCount(newCount);
                        holder.likeCount.setText(String.valueOf(newCount));
                        holder.likeIcon.setEnabled(true);
                    });
                }
            }).addOnFailureListener(e -> holder.likeIcon.setEnabled(true));
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar, media, likeIcon;
        TextView username, title, description, likeCount;
        TextView forumTopicPreview, forumDifficultyPreview;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.user_avatar);
            media = itemView.findViewById(R.id.post_media);
            username = itemView.findViewById(R.id.username);
            title = itemView.findViewById(R.id.post_title);
            description = itemView.findViewById(R.id.post_description);
            likeCount = itemView.findViewById(R.id.like_count);
            likeIcon = itemView.findViewById(R.id.like_icon);
            forumTopicPreview = itemView.findViewById(R.id.forum_topic_preview);
            forumDifficultyPreview = itemView.findViewById(R.id.forum_difficulty_preview);
        }
    }

    public interface OnPostVisibilityChangedListener {
        void onPostInvisible(String postId);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull PostViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getBindingAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            Post post = postList.get(position);
            if (post.getId() != null && visibilityListener != null) {
                visibilityListener.onPostInvisible(post.getId());
            }
        }
    }
}