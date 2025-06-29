package com.example.vibefitapp;

import com.google.firebase.Timestamp;
import java.util.Objects;

public class Comment {
    private String id;
    private String postId;
    private String userId;
    private String username;
    private String userAvatarUrl;
    private String content;
    private Timestamp timestamp;
    private String parentCommentId;
    private int indentLevel = 0;


    public Comment() {
        // Required for Firebase
    }

    public Comment(String id, String postId, String userId, String username, String userAvatarUrl, String content, Timestamp timestamp, String parentCommentId) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.username = username;
        this.userAvatarUrl = userAvatarUrl;
        this.content = content;
        this.timestamp = timestamp;
        this.parentCommentId = parentCommentId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
    public int getIndentLevel() {
        return indentLevel;
    }

    public void setIndentLevel(int indentLevel) {
        this.indentLevel = indentLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        // Compare all fields that define a comment's "content"
        return Objects.equals(id, comment.id) &&
                Objects.equals(postId, comment.postId) &&
                Objects.equals(userId, comment.userId) &&
                Objects.equals(username, comment.username) &&
                Objects.equals(userAvatarUrl, comment.userAvatarUrl) &&
                Objects.equals(content, comment.content) &&
                Objects.equals(timestamp, comment.timestamp) &&
                Objects.equals(parentCommentId, comment.parentCommentId);
    }

    @Override
    public int hashCode() {
        // Include all fields used in equals()
        return Objects.hash(id, postId, userId, username, userAvatarUrl, content, timestamp, parentCommentId);
    }
}
