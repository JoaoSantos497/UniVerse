package com.universe;

import com.google.firebase.firestore.Exclude;
import java.util.ArrayList;
import java.util.List;

public class Post {
    private String userId;
    private String userName;
    private String content;
    private String date;
    private long timestamp;
    private List<String> likes;

    @Exclude
    private String postId;

    public Post() { }

    // Construtor atualizado com userId
    public Post(String userId, String userName, String content, String date, long timestamp) {
        this.userId = userId; // Save ID
        this.userName = userName;
        this.content = content;
        this.date = date;
        this.timestamp = timestamp;
        this.likes = new ArrayList<>();
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public long getTimestamp() { return timestamp; }

    public List<String> getLikes() {
        if (likes == null) return new ArrayList<>();
        return likes;
    }

    @Exclude
    public String getPostId() { return postId; }
    @Exclude
    public void setPostId(String postId) { this.postId = postId; }
}