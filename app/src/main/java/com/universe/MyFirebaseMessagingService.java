package com.universe;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_SERVICE";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Novo token gerado: " + token);
        atualizarTokenNoFirestore(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "message recebida de: " + remoteMessage.getFrom());

        String title = "UniVerse";
        String body = "";
        String postId = null;
        NotificationType notificationType = null;
        String fromUserId = null;

        // 1. Prioridade para dados do objeto "data" (enviados pela Cloud Function)
        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            postId = data.getOrDefault("postId", null);
            // Se a Cloud Function enviar título/body dentro do data:
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("body")) body = data.get("body");
            if (data.containsKey("type")) notificationType = NotificationType.valueOf(data.get("type"));
            if (data.containsKey("fromUserId")) fromUserId = data.get("fromUserId");

        }

        // 2. Se não houver no data, tenta o objeto de Notificação padrão
        if (remoteMessage.getNotification() != null) {
            if (title.equals("UniVerse")) title = remoteMessage.getNotification().getTitle();
            if (body == null || body.isEmpty()) body = remoteMessage.getNotification().getBody();
        }

        if (body != null && !body.isEmpty()) {
            enviarNotificacaoLocal(title, body, postId, fromUserId, notificationType) ;
        }
    }

    private void enviarNotificacaoLocal(String title, String body, String postId, String fromUserId, NotificationType notificationType) {
        String channelId = "universe_v3";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 1. Criar o Canal com IMPORTANCE_HIGH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Alertas UniVerse",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notificações de interações sociais");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // 2. Configurar a Intent para abrir a Activity correta
        Intent intent = null;

        switch (notificationType) {
            case FOLLOW:
                if (fromUserId != null && !fromUserId.isEmpty()) {
                    intent = new Intent(this, PublicProfileActivity.class);
                    intent.putExtra("targetUserId", fromUserId);
                }
                break;

            case LIKE:
            case COMMENT:
                if (postId != null && !postId.isEmpty()) {
                    intent = new Intent(this, CommentsActivity.class);
                    intent.putExtra("postId", postId);
                    intent.putExtra("authorId", fromUserId);
                }
                break;
            case POST:
                if (postId != null && !postId.isEmpty()) {
                    intent = new Intent(this, PostDetailsActivity.class);
                    intent.putExtra("postId", postId);
                    intent.putExtra("authorId", fromUserId);
                }
                break;
        }

        // Se o intent continuar null (porque o tipo é desconhecido ou faltavam dados), paramos aqui
        if (intent == null) {
            return;
        }


        // FLAG_UPDATE_CURRENT é vital para que o postId seja atualizado se receberes 2 notificações
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 3. Builder configurado para "Heads-up" (Pop-up no topo)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(new long[]{100, 200, 300, 400, 500})
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            // ID único usando o tempo atual evita que uma notificação apague a outra
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void atualizarTokenNoFirestore(String novoToken) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update("fcmToken", novoToken)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token atualizado no Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Erro ao atualizar token", e));
        }
    }
}