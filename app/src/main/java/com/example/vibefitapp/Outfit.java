package com.example.vibefitapp;

public class Outfit {
    private String id;
    private String imageUrl;
    private String uid;
    private String storagePath;
    private boolean userUploaded;


    public Outfit() {
    }

    public Outfit(String imageUrl, String uid) {
        this.imageUrl = imageUrl;
        this.uid = uid;
    }

    public String getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getUid() {
        return uid;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Outfit)) return false;
        Outfit other = (Outfit) o;
        return id != null && id.equals(other.id) &&
                imageUrl != null && imageUrl.equals(other.imageUrl) &&
                uid != null && uid.equals(other.uid);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public boolean isUserUploaded() {
        return userUploaded;
    }

    public void setUserUploaded(boolean userUploaded) {
        this.userUploaded = userUploaded;
    }
}

