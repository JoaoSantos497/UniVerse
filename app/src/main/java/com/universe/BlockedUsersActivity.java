package com.universe;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BlockedUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BlockedUserAdapter adapter;
    private List<User> blockedList;
    private FirebaseFirestore db;
    private TextView txtEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_users);

        db = FirebaseFirestore.getInstance();
        String myId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.recyclerBlockedUsers);
        txtEmpty = findViewById(R.id.txtEmptyBlocked); // Adiciona este ID no teu XML se quiseres aviso de lista vazia

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        blockedList = new ArrayList<>();
        adapter = new BlockedUserAdapter(blockedList);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBackBlocked).setOnClickListener(v -> finish());

        carregarBloqueados(myId);
    }

    private void carregarBloqueados(String myId) {
        db.collection("users").document(myId).collection("blocked")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    blockedList.clear();
                    if (querySnapshot.isEmpty()) {
                        if(txtEmpty != null) txtEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String blockedId = doc.getId();

                        // Buscar detalhes de quem está bloqueado
                        db.collection("users").document(blockedId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        if (user != null) {
                                            user.setUid(userDoc.getId());
                                            blockedList.add(user);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                });
                    }
                });
    }
}