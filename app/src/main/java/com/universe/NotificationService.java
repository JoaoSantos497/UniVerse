package com.universe;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class NotificationService {
    private FirebaseFirestore db;
    private String myUid;
    public NotificationService() {
            this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            this.db = FirebaseFirestore.getInstance();
    }

    public WriteBatch sendNotification(WriteBatch batch, String tUid, NotificationType type) {
        if (tUid.equals(myUid)) return batch;
        DocumentReference notifRef = db.collection("notifications").document();
        Map<String, Object> notif = notification(tUid, type);

        batch.set(notifRef, notif);

        return batch;
    }

    public void sendNotification(String targetId, NotificationType type) {
        if (targetId.equals(myUid)) return;
        db.collection("users").document(myUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Map<String, Object> n = notification(targetId, type);
                db.collection("notifications").add(n);
            }
        });
    }

    public void sendNotification(User user, String targetId, String postId, NotificationType type) {
        if (targetId.equals(myUid)) return;
        Map<String, Object> n = notification(targetId, type);
        n.put("fromUserName", user.getNome());
        n.put("fromUserPhoto", user.getPhotoUrl());
        n.put("postId", postId);
        db.collection("notifications").add(n);

    }

    private Map<String, Object> notification (String tUid, NotificationType type) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("targetUserId", tUid);
        notif.put("fromUserId", myUid);
        notif.put("message", type.getMessage());
        notif.put("type", type.getType());
        notif.put("timestamp", FieldValue.serverTimestamp());
        notif.put("read", false);
        return notif;

    }
}
