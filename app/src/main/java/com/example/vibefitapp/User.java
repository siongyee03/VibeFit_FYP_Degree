package com.example.vibefitapp;

public class User {
    private String username;
    private String email;
    private String profileImageUrl;
    private String gender;
    private String role;

    public User() {
        // Default constructor required for Firebase Firestore
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public User(String username, String email, String profileImageUrl) {
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    public User(String username, String email, String profileImageUrl, String gender, String role) {
        this(username, email, profileImageUrl);
        this.gender = gender;
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isDefaultAvatar() {
        return profileImageUrl == null || profileImageUrl.isEmpty();
    }
}


