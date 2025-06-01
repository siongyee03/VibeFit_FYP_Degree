package com.example.vibefitapp;

public class User {
    private String username;
    private String email;
    private String profileImageUrl;
    private String gender;

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

    public User(String username, String email, String profileImageUrl, String gender) {
        this(username, email, profileImageUrl);
        this.gender = gender;
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
        try {
            Integer.parseInt(profileImageUrl);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}


