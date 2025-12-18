package com.universe;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private TextView txtTitle;
    private ImageButton btnBack;

    private LinearLayout emptyView;
    private TextView txtEmptyMessage;

    private FirebaseFirestore db;
    private String userId;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        userId = getIntent().getStringExtra("userId");
        type = getIntent().getStringExtra("type");

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerUserList);
        txtTitle = findViewById(R.id.txtUserListTitle);
        btnBack = findViewById(R.id.btnBackUserList);

        emptyView = findViewById(R.id.emptyView);
        txtEmptyMessage = findViewById(R.id.txtEmptyMessage);

        if ("followers".equals(type)) {
            txtTitle.setText("Seguidores");
            if (txtEmptyMessage != null) txtEmptyMessage.setText("Ainda não tens seguidores.");
        } else {
            txtTitle.setText("A Seguir");
            if (txtEmptyMessage != null) txtEmptyMessage.setText("Ainda não segues ninguém.");
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();

        // --- CORREÇÃO AQUI: Passamos 'true' para mostrar o botão de Seguir nesta lista ---
        userAdapter = new UserAdapter(userList, true);

        recyclerView.setAdapter(userAdapter);

        btnBack.setOnClickListener(v -> finish());

        carregarLista();
    }

    private void carregarLista() {
        if (userId == null || type == null) return;

        userList.clear();
        userAdapter.notifyDataSetChanged();

        db.collection("users").document(userId).collection(type)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        mostrarVazio(true);
                        return;
                    }

                    mostrarVazio(false);

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // O ID do documento na sub-coleção é o UID do utilizador
                        String idEncontrado = doc.getId();

                        db.collection("users").document(idEncontrado).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        if (user != null) {
                                            user.setUid(userDoc.getId());
                                            userList.add(user);
                                            userAdapter.notifyItemInserted(userList.size() - 1);
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao carregar lista.", Toast.LENGTH_SHORT).show();
                    mostrarVazio(true);
                });
    }

    private void mostrarVazio(boolean vazio) {
        if (emptyView == null) return;
        if (vazio) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}