package com.universe;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

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
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list); // Reutilizamos este layout!

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerUserList);
        ImageButton btnBack = findViewById(R.id.btnBackUserList);
        TextView title = findViewById(R.id.txtUserListTitle);

        title.setText("Notificações");

        btnBack.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(this, notificationList);
        recyclerView.setAdapter(adapter);

        lerNotificacoes();
    }

    private void lerNotificacoes() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        db.collection("notifications")
                .whereEqualTo("targetUserId", myId) // Só as minhas
                .orderBy("timestamp", Query.Direction.DESCENDING) // Mais recentes primeiro
                .addSnapshotListener((value, error) -> {
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
                    }
                });
    }
}