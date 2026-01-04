package com.universe;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

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

    private UserService userService;
    private NotificationService notificationService;
    private ListenerRegistration notificationListener;

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
        userService = new UserService();
        notificationService = new NotificationService(userService);

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
        notificationListener =
                notificationService.listenToNotifications(
                        currentUserId,
                        new DataListener<>() {
                            @Override
                            public void onData(List<Notification> notifications) {
                                notificationList.clear();
                                notificationList.addAll(notifications);
                                adapter.notifyDataSetChanged();

                                if (notificationList.isEmpty()) {
                                    if (emptyView != null) emptyView.setVisibility(VISIBLE);
                                    recyclerView.setVisibility(GONE);
                                } else {
                                    if (emptyView != null) emptyView.setVisibility(GONE);
                                    recyclerView.setVisibility(VISIBLE);
                                    marcarComoLidas();
                                }
                            }
                            @Override
                            public void onError(Exception e) {

                            }
                        }
                );
    }

    private void marcarComoLidas() {
        notificationService.markAsRead(notificationList);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}