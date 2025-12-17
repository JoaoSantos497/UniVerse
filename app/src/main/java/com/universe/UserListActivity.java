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

    // Componentes do Estado Vazio
    private LinearLayout emptyView;
    private TextView txtEmptyMessage;

    private FirebaseFirestore db;
    private String userId;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        // Receber dados da Intent
        userId = getIntent().getStringExtra("userId");
        type = getIntent().getStringExtra("type"); // "followers" ou "following"

        db = FirebaseFirestore.getInstance();

        // Ligar UI
        recyclerView = findViewById(R.id.recyclerUserList);
        txtTitle = findViewById(R.id.txtUserListTitle);
        btnBack = findViewById(R.id.btnBackUserList);

        // Estado Vazio
        emptyView = findViewById(R.id.emptyView);
        txtEmptyMessage = findViewById(R.id.txtEmptyMessage);

        // Configurar Textos
        if ("followers".equals(type)) {
            txtTitle.setText("Seguidores");
            if (txtEmptyMessage != null) txtEmptyMessage.setText("Ainda não tens seguidores.");
        } else {
            txtTitle.setText("A Seguir");
            if (txtEmptyMessage != null) txtEmptyMessage.setText("Ainda não segues ninguém.");
        }

        // Configurar Lista
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList);
        recyclerView.setAdapter(userAdapter);

        // Botão Voltar
        btnBack.setOnClickListener(v -> finish());

        // Carregar Dados
        carregarLista();
    }

    private void carregarLista() {
        if (userId == null || type == null) return;

        // IMPORTANTE: Limpar a lista antes de carregar para evitar duplicados
        userList.clear();
        userAdapter.notifyDataSetChanged();

        db.collection("users").document(userId).collection(type)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Verifica se está vazio
                    if (queryDocumentSnapshots.isEmpty()) {
                        mostrarVazio(true);
                        return;
                    }

                    mostrarVazio(false);

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String idEncontrado = doc.getId();

                        // Buscar os detalhes de cada utilizador
                        db.collection("users").document(idEncontrado).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        if (user != null) {
                                            // Define o ID manualmente para garantir que o clique funciona
                                            user.setUid(userDoc.getId());

                                            userList.add(user);
                                            // Atualiza apenas o item inserido (mais eficiente)
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