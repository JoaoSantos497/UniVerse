package com.universe;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class Notification {
    private String fromUserId;
    private String fromUserName;
    private String fromUserPhoto;
    private String targetUserId;
    private NotificationType type;
    private String message;
    private String postId;
    private Timestamp timestamp;
    private boolean read;

    @Exclude
    private String notificationId;

    public Notification() { }

    public Notification(String fromUserId, String fromUserName, String fromUserPhoto, String targetUserId, NotificationType type, String message, String postId, Timestamp timestamp) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserPhoto = fromUserPhoto;
        this.targetUserId = targetUserId;
        this.type = type;
        this.message = message;
        this.postId = postId;
        this.timestamp = timestamp;
        this.read = false;
    }

    // --- GETTERS E SETTERS ---

    @Exclude
    public String getNotificationId() { return notificationId; }
    @Exclude
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }
    public String getFromUserPhoto() { return fromUserPhoto; }
    public void setFromUserPhoto(String fromUserPhoto) { this.fromUserPhoto = fromUserPhoto; }
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}