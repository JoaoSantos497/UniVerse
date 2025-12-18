package com.universe;

import java.util.ArrayList;
import java.util.List;

public class Post {
    private String userId;
    private String userName;
    private String content;
    private String date;
    private long timestamp;
    private List<String> likes;
    private String imageUrl;
    private String universityDomain;
    private String postType;

    // REMOVIDO O @Exclude PARA O FIREBASE CONSEGUIR LER O ID
    private String postId;

    // Construtor vazio obrigatório para o Firebase
    public Post() { }

    public Post(String userId, String userName, String content, String date, long timestamp, String imageUrl, String universityDomain, String postType) {
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.date = date;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
        this.universityDomain = universityDomain;
        this.postType = postType;
        this.likes = new ArrayList<>();
    }

    // --- GETTERS E SETTERS ---

    // Importante para o mapeamento automático do Firestore
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getUniversityDomain() { return universityDomain; }
    public void setUniversityDomain(String universityDomain) { this.universityDomain = universityDomain; }

    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }

    public List<String> getLikes() {
        if (likes == null) return new ArrayList<>();
        return likes;
    }
    public void setLikes(List<String> likes) { this.likes = likes; }
}