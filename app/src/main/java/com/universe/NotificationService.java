package com.universe;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class NotificationService {
    private FirebaseFirestore db;
    private String myUid;
    private String myName; // <--- NOVA VARIÁVEL: Guarda o nome para usar no Follow

    public NotificationService() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.myUid = user.getUid();
            // Tenta pegar o nome diretamente do Auth (sem ir ao banco de dados, é instantâneo)
            this.myName = user.getDisplayName();
        }
        this.db = FirebaseFirestore.getInstance();
    }

    // --- 1. SEGUIR (BATCH) ---
    // Agora este método já vai ter acesso ao 'myName' através do método 'notification()'
    public WriteBatch sendNotification(WriteBatch batch, String tUid, NotificationType type) {
        if (tUid.equals(myUid)) return batch;

        DocumentReference notifRef = db.collection("notifications").document();
        Map<String, Object> notif = notification(tUid, type); // O segredo está aqui dentro agora

        batch.set(notifRef, notif);

        return batch;
    }

    // --- 2. NOTIFICAÇÃO SIMPLES (Geral) ---
    public void sendNotification(String targetId, NotificationType type) {
        if (targetId.equals(myUid)) return;

        // Aqui mantemos a busca ao banco para garantir (caso o Auth falhe)
        db.collection("users").document(myUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Map<String, Object> n = notification(targetId, type);

                // Se o método 'notification' falhou em pegar o nome do Auth, tentamos do Firestore
                if (!n.containsKey("fromUserName")) {
                    // Verifica as tuas chaves: "nome", "name", "username"
                    if (doc.contains("nome")) n.put("fromUserName", doc.getString("nome"));
                    else if (doc.contains("userName")) n.put("fromUserName", doc.getString("userName"));
                }

                db.collection("notifications").add(n);
            }
        });
    }

    // --- 3. LIKES E COMENTÁRIOS (Com objeto User) ---
    public void sendNotification(User user, String targetId, String postId, NotificationType type) {
        if (targetId.equals(myUid)) return;

        Map<String, Object> n = notification(targetId, type);

        // Adicionamos o nome explicitamente vindo do objeto User
        if (user.getNome() != null && !user.getNome().isEmpty()) {
            n.put("fromUserName", user.getNome());
        }

        if (user.getPhotoUrl() != null) {
            n.put("fromUserPhoto", user.getPhotoUrl());
        }

        // Passa o postId (e garante que não é null)
        n.put("postId", postId != null ? postId : "");

        db.collection("notifications").add(n);
    }

    // --- Metodo auxiliar ---
    private Map<String, Object> notification (String tUid, NotificationType type) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("targetUserId", tUid);
        notif.put("fromUserId", myUid);
        notif.put("message", type.getMessage());
        notif.put("type", type.getType());
        notif.put("timestamp", FieldValue.serverTimestamp());
        notif.put("read", false);

        // --- Passar o nome do Follow ---
        if (myName != null && !myName.isEmpty()) {
            notif.put("fromUserName", myName);
        }

        return notif;
    }
}