package com.universe;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
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

    private FirebaseFirestore db;
    private String userId; // De quem é esta lista?
    private String type;   // É "followers" ou "following"?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        // 1. Receber dados da Intent
        userId = getIntent().getStringExtra("userId");
        type = getIntent().getStringExtra("type"); // "followers" ou "following"

        db = FirebaseFirestore.getInstance();

        // 2. Ligar UI
        recyclerView = findViewById(R.id.recyclerUserList);
        txtTitle = findViewById(R.id.txtUserListTitle);
        btnBack = findViewById(R.id.btnBackUserList);

        // 3. Configurar Título
        if ("followers".equals(type)) {
            txtTitle.setText("Seguidores");
        } else {
            txtTitle.setText("A Seguir");
        }

        // 4. Configurar Lista (Reutilizando UserAdapter da Pesquisa)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList);
        recyclerView.setAdapter(userAdapter);

        // 5. Botão Voltar
        btnBack.setOnClickListener(v -> finish());

        // 6. Carregar a Lista
        carregarLista();
    }

    private void carregarLista() {
        // Vai à sub-coleção correta (followers ou following)
        db.collection("users").document(userId).collection(type)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        // Para cada ID encontrado na lista...
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String idEncontrado = doc.getId();

                            // ... vamos buscar os detalhes completos na coleção 'users'
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
                    } else {
                        Toast.makeText(this, "Ainda não há ninguém aqui.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}