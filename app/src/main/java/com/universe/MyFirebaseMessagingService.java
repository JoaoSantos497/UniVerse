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

    /**
     * Chamado quando um novo token é gerado (instalação nova, limpeza de dados, etc).
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Novo token gerado: " + token);

        // Envia para o Firestore para garantir que o servidor tem o "endereço" atualizado
        atualizarTokenNoFirestore(token);
    }

    /**
     * Chamado quando uma mensagem FCM é recebida com a app em primeiro plano ou background.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Mensagem recebida de: " + remoteMessage.getFrom());

        String titulo = "UniVerse";
        String corpo = "";
        String postId = "";

        // 1. Tentar obter dados do objeto de Notificação (Título e Corpo)
        if (remoteMessage.getNotification() != null) {
            titulo = remoteMessage.getNotification().getTitle();
            corpo = remoteMessage.getNotification().getBody();
        }

        // 2. Tentar obter dados do mapa "data" (importante para Cloud Functions)
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            // Se o título/corpo não vieram na "notification", podem vir na "data"
            if (corpo == null || corpo.isEmpty()) {
                titulo = data.get("title") != null ? data.get("title") : titulo;
                corpo = data.get("body") != null ? data.get("body") : "";
            }
            // Extrair o ID do post para abrir ao clicar
            postId = data.get("postId");
        }

        if (corpo != null && !corpo.isEmpty()) {
            enviarNotificacaoLocal(titulo, corpo, postId);
        }
    }

    private void enviarNotificacaoLocal(String titulo, String mensagem, String postId) {
        String channelId = "notificacoes_universe";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 1. Criar o Canal com IMPORTANCE_HIGH (Crucial para o pop-up)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Alertas UniVerse",
                    NotificationManager.IMPORTANCE_HIGH); // <--- IMPORTANTE
            channel.setDescription("Notificações de interações sociais");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // 2. Configurar a Intent
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("postId", postId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // 3. Builder com Prioridade Máxima
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // <--- Para versões antigas
                .setDefaults(NotificationCompat.DEFAULT_ALL)   // Ativa som e vibração padrão
                .setFullScreenIntent(pendingIntent, false)     // Às vezes ajuda a forçar o pop-up
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
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