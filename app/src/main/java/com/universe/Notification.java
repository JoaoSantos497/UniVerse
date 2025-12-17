package com.universe;

import com.google.firebase.firestore.Exclude;

public class Notification {
    private String fromUserId;
    private String fromUserName;
    private String fromUserPhoto;
    private String targetUserId;
    private String type; // "like", "follow", "comment"
    private String message;
    private String postId;
    private long timestamp;
    private boolean read;

    @Exclude
    private String notificationId;

    public Notification() { }

    public Notification(String fromUserId, String fromUserName, String fromUserPhoto, String targetUserId, String type, String message, String postId, long timestamp) {
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
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}