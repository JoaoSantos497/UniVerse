package com.universe;

public class Notification {
    private String notificationId;
    private String fromUserId;    // Quem fez a ação (quem deu like)
    private String fromUserName;  // Nome de quem fez
    private String fromUserPhoto; // Foto de quem fez
    private String targetUserId;  // Para quem é a notificação (eu)
    private String type;          // "like", "comment", "follow"
    private String message;       // "gostou da tua publicação"
    private String postId;        // ID do post (se aplicável)
    private long timestamp;

    public Notification() {} // Obrigatório para o Firebase

    public Notification(String fromUserId, String fromUserName, String fromUserPhoto, String targetUserId, String type, String message, String postId, long timestamp) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserPhoto = fromUserPhoto;
        this.targetUserId = targetUserId;
        this.type = type;
        this.message = message;
        this.postId = postId;
        this.timestamp = timestamp;
    }

    // Getters e Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getFromUserId() { return fromUserId; }
    public String getFromUserName() { return fromUserName; }
    public String getFromUserPhoto() { return fromUserPhoto; }
    public String getTargetUserId() { return targetUserId; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getPostId() { return postId; }
    public long getTimestamp() { return timestamp; }
}