package com.universe;

public class Comment {
    private String userId;
    private String userName;
    private String content;
    private long timestamp;
    private String commentImageUrl;

    public Comment() {
        // Construtor vazio obrigatório para o Firebase
    }

    // Construtor Atualizado (recebe agora o commentImageUrl no fim)
    public Comment(String userId, String userName, String content, long timestamp, String commentImageUrl) {
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
        this.commentImageUrl = commentImageUrl;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }

    // Novo Getter e Setter para a imagem
    public String getCommentImageUrl() { return commentImageUrl; }
    public void setCommentImageUrl(String commentImageUrl) { this.commentImageUrl = commentImageUrl; }
}