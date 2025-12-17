package com.universe;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private ImageButton btnBack;

    // UI para estado vazio
    private LinearLayout emptyView;
    // Removi os TextViews (txtTitle, txtEmptyMessage) porque agora o XML já tem os textos certos!

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- ATUALIZAÇÃO 1: Usar o layout específico ---
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        // --- ATUALIZAÇÃO 2: Usar os IDs corretos do activity_notifications.xml ---
        recyclerView = findViewById(R.id.recyclerNotifications);
        btnBack = findViewById(R.id.btnBackNotifications);
        emptyView = findViewById(R.id.emptyViewNotifications);

        // Configurar Lista
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        carregarNotificacoes();
    }

    private void carregarNotificacoes() {
        db.collection("notifications")
                .whereEqualTo("targetUserId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Toast.makeText(this, "Erro ao carregar.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        notificationList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notification n = doc.toObject(Notification.class);
                            if (n != null) {
                                n.setNotificationId(doc.getId());
                                notificationList.add(n);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        // Gerir estado vazio
                        if (notificationList.isEmpty()) {
                            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            if (emptyView != null) emptyView.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            marcarComoLidas();
                        }
                    }
                });
    }

    private void marcarComoLidas() {
        for (Notification n : notificationList) {
            if (!n.isRead() && n.getNotificationId() != null) {
                db.collection("notifications").document(n.getNotificationId())
                        .update("read", true);
            }
        }
    }
}