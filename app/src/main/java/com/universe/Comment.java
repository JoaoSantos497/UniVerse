package com.universe;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Comment {
    @Exclude
    private String commentId; // ID do documento (não gravado dentro do documento)

    private String userId;
    private String userName;
    private String userPhotoUrl;
    private String content;
    private long timestamp;
    private String commentImageUrl; // URL da imagem anexada ao comentário

    // Construtor vazio necessário para o Firebase
    public Comment() { }

    public Comment(String userId, String userName, String userPhotoUrl, String content, long timestamp, String commentImageUrl) {
        this.userId = userId;
        this.userName = userName;
        this.userPhotoUrl = userPhotoUrl;
        this.content = content;
        this.timestamp = timestamp;
        this.commentImageUrl = commentImageUrl;
    }

    // Getters e Setters

    @Exclude // O ID não precisa ser gravado no Firestore, pois já é o nome do documento
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhotoUrl() { return userPhotoUrl; }
    public void setUserPhotoUrl(String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCommentImageUrl() { return commentImageUrl; }
    public void setCommentImageUrl(String commentImageUrl) { this.commentImageUrl = commentImageUrl; }
}