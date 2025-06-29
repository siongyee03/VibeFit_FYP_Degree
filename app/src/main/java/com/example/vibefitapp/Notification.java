package com.example.vibefitapp;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Notification {
    private String id;
    private String toUserId;
    private String fromUserId;
    private String fromUsername;
    private String fromUserAvatarUrl;
    private String type;
    private String postId;
    private String postTitle;
    private String commentId;
    private String originalCommentId;
    private String content;
    private @ServerTimestamp Date timestamp;
    private boolean read;

    public Notification() {
        // Required for Firestore
    }

    public Notification(String toUserId, String fromUserId, String fromUsername, String fromUserAvatarUrl, String type, String postId, String postTitle, String commentId, String originalCommentId, String content, boolean read) {
        this.toUserId = toUserId;
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.fromUserAvatarUrl = fromUserAvatarUrl;
        this.type = type;
        this.postId = postId;
        this.postTitle = postTitle;
        this.commentId = commentId;
        this.originalCommentId = originalCommentId;
        this.content = content;
        this.read = read;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getFromUserAvatarUrl() { return fromUserAvatarUrl; }
    public void setFromUserAvatarUrl(String fromUserAvatarUrl) { this.fromUserAvatarUrl = fromUserAvatarUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getOriginalCommentId() { return originalCommentId; }
    public void setOriginalCommentId(String originalCommentId) { this.originalCommentId = originalCommentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Notification that = (Notification) o;

        return read == that.read &&
                id != null && id.equals(that.id) &&
                toUserId != null && toUserId.equals(that.toUserId) &&
                fromUserId != null && fromUserId.equals(that.fromUserId) &&
                fromUsername != null && fromUsername.equals(that.fromUsername) &&
                fromUserAvatarUrl != null && fromUserAvatarUrl.equals(that.fromUserAvatarUrl) &&
                type != null && type.equals(that.type) &&
                postId != null && postId.equals(that.postId) &&
                postTitle != null && postTitle.equals(that.postTitle) &&
                commentId != null && commentId.equals(that.commentId) &&
                originalCommentId != null && originalCommentId.equals(that.originalCommentId) &&
                content != null && content.equals(that.content) &&
                timestamp != null && timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (toUserId != null ? toUserId.hashCode() : 0);
        result = 31 * result + (fromUserId != null ? fromUserId.hashCode() : 0);
        result = 31 * result + (fromUsername != null ? fromUsername.hashCode() : 0);
        result = 31 * result + (fromUserAvatarUrl != null ? fromUserAvatarUrl.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (postId != null ? postId.hashCode() : 0);
        result = 31 * result + (postTitle != null ? postTitle.hashCode() : 0);
        result = 31 * result + (commentId != null ? commentId.hashCode() : 0);
        result = 31 * result + (originalCommentId != null ? originalCommentId.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (read ? 1 : 0);
        return result;
    }

}