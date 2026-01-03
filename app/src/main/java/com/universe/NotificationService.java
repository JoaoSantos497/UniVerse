package com.universe;

import static java.util.Collections.emptyMap;

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
    public WriteBatch sendNotification(WriteBatch batch, User user, String tUid, NotificationType type) {
        if (tUid.equals(myUid)) return batch;

        DocumentReference notifRef = db.collection("notifications").document();
        Map<String, Object> notif = notification(user, tUid, type, emptyMap()); // O segredo está aqui dentro agora

        batch.set(notifRef, notif);

        return batch;
    }

    // --- 2. NOTIFICAÇÃO SIMPLES (Geral) ---
    public Task<Boolean> sendNotification(String targetId, NotificationType type) {
        return sendNotification(targetId, type, emptyMap());
    }

    // --- 3. LIKES E COMENTÁRIOS (Com objeto User) ---
    public Task<Boolean> sendNotification(String targetId, NotificationType type, String postId) {
        return sendNotification(targetId, type, Map.of("postId", postId != null ? postId : ""));
    }

    private Task<Boolean> sendNotification(String targetId, NotificationType type, Map<String, Object> additional) {
        if (targetId.equals(myUid)) Tasks.forResult(false);
        return userService.getUser(myUid).onSuccessTask(userOpt ->
                userOpt.map(user -> sendNotification(user, targetId, type, additional)
                .onSuccessTask(it -> Tasks.forResult(true)))
                        .orElseGet(() -> Tasks.forResult(false)));
    }

    private Task<DocumentReference> sendNotification(User user, String targetId, NotificationType type, Map<String, Object> additional) {
        Map<String, Object> n = notification(user, targetId, type, additional);
        return db.collection("notifications").add(n);
    }

    // --- Metodo auxiliar ---
    private Map<String, Object> notification(User user, String tUid, NotificationType type, Map<String, Object> additional) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("targetUserId", tUid);
        notif.put("fromUserId", user.getUid());
        notif.put("message", type.getMessage());
        notif.put("type", type.getType());
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