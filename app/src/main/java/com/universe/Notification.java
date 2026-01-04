package com.universe;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Optional;

public class Notification {

    private String fromUserId;
    private String targetUserId;
    private NotificationType type;
    private String message;
    private boolean read;
    private String fromUserName;
    private String fromUserPhoto;
    private String postId;

    @ServerTimestamp
    private Timestamp timestamp;

    @Exclude
    private String notificationId;

    public Notification() {}

    public Notification(
            String fromUserId,
            String fromUserName,
            String fromUserPhoto,
            String targetUserId,
            NotificationType type
    ) {
        this(fromUserId, fromUserName, fromUserPhoto, targetUserId, type, null);
    }

    public Notification(
            String fromUserId,
            String fromUserName,
            String fromUserPhoto,
            String targetUserId,
            NotificationType type,
            String postId
    ) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserPhoto = fromUserPhoto;
        this.targetUserId = targetUserId;
        this.type = type;
        this.message = type.getMessage();
        this.postId = postId;
        this.read = false;
    }

    @Exclude
    public boolean hasPost() {
        return postId != null && !postId.isEmpty();
    }

    @Exclude
    public Optional<String> getPostIdOptional() {
        return Optional.ofNullable(postId);
    }

    @Exclude
    public long getTimestampMillis() {
        return timestamp != null ? timestamp.toDate().getTime() : 0L;
    }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) {
        this.type = type;
        this.message = type != null ? type.getMessage() : null;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getFromUserPhoto() { return fromUserPhoto; }
    public void setFromUserPhoto(String fromUserPhoto) { this.fromUserPhoto = fromUserPhoto; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    @Exclude
    public String getNotificationId() { return notificationId; }

    @Exclude
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }
}
