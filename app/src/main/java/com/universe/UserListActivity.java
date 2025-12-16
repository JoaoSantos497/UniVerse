package com.universe;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout; // Importante para o emptyView
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

    // --- NOVAS VARIÁVEIS PARA O ESTADO VAZIO ---
    private LinearLayout emptyView;
    private TextView txtEmptyMessage;

    private FirebaseFirestore db;
    private String userId;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Garante que o layout activity_user_list.xml tem o LinearLayout com id emptyView
        setContentView(R.layout.activity_user_list);

        userId = getIntent().getStringExtra("userId");
        type = getIntent().getStringExtra("type");

        db = FirebaseFirestore.getInstance();

        // Ligar UI
        recyclerView = findViewById(R.id.recyclerUserList);
        txtTitle = findViewById(R.id.txtUserListTitle);
        btnBack = findViewById(R.id.btnBackUserList);

        // Ligar componentes do estado vazio
        emptyView = findViewById(R.id.emptyView);
        txtEmptyMessage = findViewById(R.id.txtEmptyMessage);

        // Configurar Título e Mensagem Vazia
        if ("followers".equals(type)) {
            txtTitle.setText("Seguidores");
            if (txtEmptyMessage != null) txtEmptyMessage.setText("Ainda não tens seguidores.");
        } else {
            txtTitle.setText("A Seguir");
            if (txtEmptyMessage != null) txtEmptyMessage.setText("Ainda não segues ninguém.");
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList);
        recyclerView.setAdapter(userAdapter);

        btnBack.setOnClickListener(v -> finish());

        carregarLista();
    }

    private void carregarLista() {
        db.collection("users").document(userId).collection(type)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Verifica se a sub-coleção está vazia logo no início
                    if (queryDocumentSnapshots.isEmpty()) {
                        mostrarVazio(true);
                        return;
                    }

                    mostrarVazio(false); // Tem dados, mostra a lista

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String idEncontrado = doc.getId();

                        db.collection("users").document(idEncontrado).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        if (user != null) {
                                            user.setUid(userDoc.getId());
                                            userList.add(user);
                                            userAdapter.notifyDataSetChanged();
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao carregar lista.", Toast.LENGTH_SHORT).show();
                    mostrarVazio(true); // Em caso de erro, assume vazio ou mostra erro
                });
    }

    // Método auxiliar para alternar entre Lista e Aviso Vazio
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