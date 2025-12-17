package com.universe;

import com.google.firebase.firestore.Exclude;
import java.util.ArrayList;
import java.util.List;

public class Post {
    // --- ALTERADO PARA userId ---
    private String userId;

    private String userName;
    private String content;
    private String date;
    private long timestamp;
    private List<String> likes;
    private String imageUrl;
    private String universityDomain;
    private String postType;

    @Exclude
    private String postId;

    public Post() { }

    // Construtor atualizado
    public Post(String userId, String userName, String content, String date, long timestamp, String imageUrl, String universityDomain, String postType) {
        this.userId = userId; // <--- Aqui
        this.userName = userName;
        this.content = content;
        this.date = date;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
        this.universityDomain = universityDomain;
        this.postType = postType;
        this.likes = new ArrayList<>();
    }

    // --- GETTERS E SETTERS COM O NOME userId ---
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // Resto do código mantém-se igual
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

    @Exclude public String getPostId() { return postId; }
    @Exclude public void setPostId(String postId) { this.postId = postId; }
}