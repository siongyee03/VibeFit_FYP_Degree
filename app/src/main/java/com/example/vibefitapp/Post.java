package com.example.vibefitapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Post implements Parcelable {
    private String username;
    private String userAvatar;
    private String category;
    private long timestampSeconds;
    private String title;

    private String content;
    private int likeCount;
    private List<String> mediaUrls;
    private String id;
    private int favouriteCount;
    private String mediaType;
    private String userId;
    private Timestamp timestamp;
    private int commentCount;
    private String forumTopic;
    private String forumSteps;
    private String forumDifficulty;

    public Post() {
        mediaUrls = new ArrayList<>();
        this.commentCount = 0;
    }

    protected Post(Parcel in) {
        username = in.readString();
        userAvatar = in.readString();
        category = in.readString();
        timestampSeconds = in.readLong();
        title = in.readString();
        likeCount = in.readInt();
        mediaUrls = in.createStringArrayList();
        id = in.readString();
        favouriteCount = in.readInt();
        mediaType = in.readString();
        userId = in.readString();
        content = in.readString();
        timestamp = in.readParcelable(Timestamp.class.getClassLoader());
        commentCount = in.readInt();

        forumTopic = in.readString();
        forumSteps = in.readString();
        forumDifficulty = in.readString();
    }

    public static final Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(username);
        parcel.writeString(userAvatar);
        parcel.writeString(category);
        parcel.writeLong(timestampSeconds);
        parcel.writeString(title);
        parcel.writeInt(likeCount);
        parcel.writeStringList(mediaUrls);
        parcel.writeString(id);
        parcel.writeInt(favouriteCount);

        parcel.writeString(mediaType);
        parcel.writeString(userId);
        parcel.writeString(content);
        parcel.writeParcelable(timestamp, i);
        parcel.writeInt(commentCount);

        parcel.writeString(forumTopic);
        parcel.writeString(forumSteps);
        parcel.writeString(forumDifficulty);
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getTimestampSeconds() { return timestampSeconds; }
    public void setTimestampSeconds(long timestampSeconds) { this.timestampSeconds = timestampSeconds; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getFavouriteCount() { return favouriteCount; }
    public void setFavouriteCount(int favouriteCount) { this.favouriteCount = favouriteCount; }

    public List<String> getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public String getForumTopic() {
        return forumTopic;
    }
    public void setForumTopic(String forumTopic) {
        this.forumTopic = forumTopic;
    }

    public String getForumSteps() {
        return forumSteps;
    }
    public void setForumSteps(String forumSteps) {
        this.forumSteps = forumSteps;
    }

    public String getForumDifficulty() {
        return forumDifficulty;
    }
    public void setForumDifficulty(String forumDifficulty) {
        this.forumDifficulty = forumDifficulty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return id != null && id.equals(post.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
