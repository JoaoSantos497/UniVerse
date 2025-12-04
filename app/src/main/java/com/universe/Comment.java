package com.universe;

public class Comment {
    private String userName; // Quem comentou
    private String content;  // O texto do comentário
    private long timestamp;  // Para ordenar

    public Comment() { }

    public Comment(String userName, String content, long timestamp) {
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getUserName() { return userName; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}