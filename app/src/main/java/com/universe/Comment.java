package com.universe;

public class Comment {
    private String userId;   // <--- NOVO: O ID de quem comentou
    private String userName;
    private String content;
    private long timestamp;

    public Comment() {
        // Construtor vazio obrigatório para o Firebase
    }

    // Atualizei o construtor para receber o userId
    public Comment(String userId, String userName, String content, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; } // <--- NOVO GETTER
    public String getUserName() { return userName; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}