package com.example.vibefitapp;

public class ChatMessage {
    private String sender;               // "User" or "AI"
    private String text;                 // Message text (can be null if only image)
    private boolean isUser;             // True if it's from user
    private String imageUrl;             // AI recommended image URL (can be null or empty)
    private String localImagePath;       // Local path if image is downloaded and saved
    private String uploadedImagePath;    // Local path of user-uploaded clothing image (if any)

    // Constructor for text-only message
    public ChatMessage(String sender, String text, boolean isUser) {
        this.sender = sender;
        this.text = text;
        this.isUser = isUser;
    }

    // Constructor for message with image URL
    public ChatMessage(String sender, String text, boolean isUser, String imageUrl) {
        this(sender, text, isUser);
        this.imageUrl = imageUrl;
    }

    public ChatMessage(String sender, String text, boolean isUser, String imageUrl, String localImagePath, String uploadedImagePath) {
        this(sender, text, isUser, imageUrl);
        this.localImagePath = localImagePath;
        this.uploadedImagePath = uploadedImagePath;
    }

    // Getters
    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLocalImagePath() {
        return localImagePath;
    }

    public String getUploadedImagePath() {
        return uploadedImagePath;
    }

    // Setters
    public void setText(String text) {
        this.text = text;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLocalImagePath(String localImagePath) {
        this.localImagePath = localImagePath;
    }

    public void setUploadedImagePath(String uploadedImagePath) {
        this.uploadedImagePath = uploadedImagePath;
    }
}