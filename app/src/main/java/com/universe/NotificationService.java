package com.universe;

import static java.util.Collections.emptyMap;

import android.content.res.Resources.NotFoundException;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class NotificationService {
    private final UserService userService;
    private FirebaseFirestore db;
    private String myUid;

    public NotificationService(UserService userService) {
        this.userService = userService;
        this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.db = FirebaseFirestore.getInstance();
    }

    // --- 1. SEGUIR (BATCH) ---
    // Agora este metodo já vai ter acesso ao 'myName' através do metodo 'notification()'
    public Task<WriteBatch> sendNotification(WriteBatch batch, String tUid, NotificationType type) {
        return notification(tUid, type, emptyMap()).onSuccessTask(n -> {
            DocumentReference notifRef = db.collection("notifications").document();
            batch.set(notifRef, n);
            return Tasks.forResult(batch);
        });
    }

    // --- 2. NOTIFICAÇÃO SIMPLES (Geral) ---
    public Task<DocumentReference> sendNotification(String targetId, NotificationType type) {
        return sendNotification(targetId, type, emptyMap());
    }

    // --- 3. LIKES E COMENTÁRIOS (Com objeto User) ---
    public Task<DocumentReference> sendNotification(String targetId, NotificationType type, String postId) {
        return sendNotification(targetId, type, Map.of("postId", postId != null ? postId : ""));
    }

    private Task<DocumentReference> sendNotification(String targetId, NotificationType type, Map<String, Object> additional) {
        return notification(targetId, type, additional)
                .onSuccessTask(n -> db.collection("notifications").add(n));
    }

    private Task<Map<String, Object>> notification(String targetId, NotificationType type, Map<String, Object> additional) {
        if (targetId.equals(myUid)) return Tasks.forException(new IllegalArgumentException("User cannot notify themselves"));
        return userService.getUser(myUid)
                .onSuccessTask(userOpt ->
                        userOpt.map(user -> Tasks.forResult(notification(user, targetId, type, additional)))
                                .orElseGet(() -> Tasks.forException(new NotFoundException())));
    }

    // --- Metodo auxiliar ---
    private Map<String, Object> notification(User user, String tUid, NotificationType type, Map<String, Object> additional) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("targetUserId", tUid);
        notif.put("fromUserId", user.getUid());
        notif.put("message", type.getMessage());
        notif.put("type", type);
        notif.put("timestamp", FieldValue.serverTimestamp());
        notif.put("read", false);

        if (user.getNome() != null && !user.getNome().isEmpty()) {
            notif.put("fromUserName", user.getNome());
        }

        if (user.getPhotoUrl() != null) {
            notif.put("fromUserPhoto", user.getPhotoUrl());
        }

        notif.putAll(additional);

        return notif;
    }
}