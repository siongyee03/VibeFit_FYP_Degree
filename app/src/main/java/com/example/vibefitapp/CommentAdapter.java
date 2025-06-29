package com.example.vibefitapp;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<Comment> commentList = new ArrayList<>();
    private final OnCommentActionListener listener;
    private final String currentUserId;
    private final String postAuthorId;

    public interface OnCommentActionListener {
        void onReplyClick(Comment comment);
        void onDeleteClick(Comment comment);
    }

    public CommentAdapter(Context context, String postAuthorId, OnCommentActionListener listener) {
        this.context = context;
        this.postAuthorId = postAuthorId;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        Log.d("ViewBind", "Binding comment: " + comment.getContent() + ", indent: " + comment.getIndentLevel());


        Glide.with(context).load(comment.getUserAvatarUrl())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(holder.avatar);

        holder.username.setText(comment.getUsername());
        holder.content.setText(comment.getContent());

        if (comment.getTimestamp() != null) {
            String timeText = DateFormat.format("MMM d, yyyy hh:mm a", comment.getTimestamp().toDate()).toString();
            holder.timestamp.setText(timeText);
        }

        // Show "Author" tag if the comment user ID matches the post author ID
        if (comment.getUserId().equals(postAuthorId)) {
            holder.authorTag.setVisibility(View.VISIBLE);
        } else {
            holder.authorTag.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReplyClick(comment);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {

                if (currentUserId != null &&
                        (currentUserId.equals(comment.getUserId()) || currentUserId.equals(postAuthorId))) {
                    listener.onDeleteClick(comment);
                    return true;
                } else {
                    Toast.makeText(holder.itemView.getContext(), "You can only delete your own comments or comments on your post.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            return false;
        });

        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (!(lp instanceof ViewGroup.MarginLayoutParams)) {
            lp = new ViewGroup.MarginLayoutParams(lp != null ? lp.width : ViewGroup.LayoutParams.MATCH_PARENT, lp != null ? lp.height : ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
        marginParams.leftMargin = (int) (comment.getIndentLevel() * context.getResources().getDisplayMetrics().density * 20);
        holder.itemView.setLayoutParams(marginParams);


    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public void updateComments(List<Comment> newComments) {
        Log.d("CommentDebug", "CommentAdapter: updateComments() called. New comments size: " + (newComments != null ? newComments.size() : "null"));

        if (newComments == null) {
            newComments = new ArrayList<>();
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CommentDiffCallback(this.commentList, newComments));
        this.commentList.clear();
        this.commentList.addAll(newComments);
        Log.d("CommentDebug", "CommentAdapter: commentList updated. Current size: " + this.commentList.size());

        diffResult.dispatchUpdatesTo(this);

        Log.d("CommentDebug", "CommentAdapter: Updates dispatched.");

    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView username, authorTag, content, timestamp;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.comment_user_avatar);
            username = itemView.findViewById(R.id.comment_username);
            authorTag = itemView.findViewById(R.id.comment_author_tag);
            content = itemView.findViewById(R.id.comment_content);
            timestamp = itemView.findViewById(R.id.comment_timestamp);
        }
    }

    public static class CommentDiffCallback extends DiffUtil.Callback {
        private final List<Comment> oldList;
        private final List<Comment> newList;

        public CommentDiffCallback(List<Comment> oldList, List<Comment> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

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
