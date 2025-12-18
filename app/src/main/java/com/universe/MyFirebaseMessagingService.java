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

        Log.d(TAG, "Mensagem recebida de: " + remoteMessage.getFrom());

        String titulo = "UniVerse";
        String corpo = "";
        String postId = "";

        // 1. Prioridade para dados do objeto "data" (enviados pela Cloud Function)
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            postId = data.get("postId");
            // Se a Cloud Function enviar título/corpo dentro do data:
            if (data.containsKey("title")) titulo = data.get("title");
            if (data.containsKey("body")) corpo = data.get("body");
        }

        // 2. Se não houver no data, tenta o objeto de Notificação padrão
        if (remoteMessage.getNotification() != null) {
            if (titulo.equals("UniVerse")) titulo = remoteMessage.getNotification().getTitle();
            if (corpo == null || corpo.isEmpty()) corpo = remoteMessage.getNotification().getBody();
        }

        if (corpo != null && !corpo.isEmpty()) {
            enviarNotificacaoLocal(titulo, corpo, postId);
        }
    }

    private void enviarNotificacaoLocal(String titulo, String mensagem, String postId) {
        // MUDAR O ID DO CANAL força o Android a criar novas definições (resolve o problema do pop-up não aparecer)
        String channelId = "universe_v2";
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
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500}); // Padrão de vibração ajuda no pop-up
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // 2. Configurar a Intent para abrir a CommentsActivity
        Intent intent = new Intent(this, CommentsActivity.class);
        if (postId != null) intent.putExtra("postId", postId);

        // FLAG_UPDATE_CURRENT é vital para que o postId seja atualizado se receberes 2 notificações
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 3. Builder configurado para "Heads-up" (Pop-up no topo)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)   // Som e vibração padrão do sistema
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