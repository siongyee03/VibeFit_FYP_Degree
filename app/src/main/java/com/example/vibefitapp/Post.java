package com.example.vibefitapp;

import java.security.Timestamp;

public class Post {
    private String username;
    private String userAvatar;
    private String mediaUrl;
    private String category;
    private Timestamp timestamp;

    public Post() {}

    public String getUsername() { return username; }
    public String getUserAvatar() { return userAvatar; }
    public String getMediaUrl() { return mediaUrl; }
    public String getCategory() { return category; }
    public Timestamp getTimestamp() { return timestamp; }

    public void setUsername(String username) { this.username = username; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setCategory(String category) { this.category = category; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
