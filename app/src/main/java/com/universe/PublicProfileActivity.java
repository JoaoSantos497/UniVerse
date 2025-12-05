package com.universe;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class PublicProfileActivity extends AppCompatActivity {

    private TextView txtAvatar, txtName, txtCourse, txtUni;
    private Button btnBack;

    // --- NOVAS VARIÁVEIS PARA A LISTA ---
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    // ------------------------------------

    private FirebaseFirestore db;
    private String targetUserId; // ID do utilizador que estamos a ver

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        db = FirebaseFirestore.getInstance();

        txtAvatar = findViewById(R.id.publicProfileAvatar);
        txtName = findViewById(R.id.publicProfileName);
        txtCourse = findViewById(R.id.publicProfileCourse);
        txtUni = findViewById(R.id.publicProfileUni);
        btnBack = findViewById(R.id.btnBack);

        // Configurar a Lista
        recyclerView = findViewById(R.id.recyclerProfilePosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // Receber o ID
        targetUserId = getIntent().getStringExtra("targetUserId");

        if (targetUserId != null) {
            carregarDadosPerfil(targetUserId);
            carregarPostsDoUtilizador(targetUserId); // <--- NOVA FUNÇÃO
        } else {
            Toast.makeText(this, "Erro ao carregar perfil", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void carregarDadosPerfil(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                txtName.setText(user.getNome());
                                txtCourse.setText(user.getCurso());
                                txtUni.setText(user.getUniversidade());

                                // Cor do Avatar
                                String nome = user.getNome();
                                if(!nome.isEmpty()) {
                                    txtAvatar.setText(nome.substring(0, 1).toUpperCase());
                                    int hash = nome.hashCode();
                                    txtAvatar.getBackground().setTint(Color.rgb(
                                            Math.abs(hash * 25) % 255,
                                            Math.abs(hash * 80) % 255,
                                            Math.abs(hash * 13) % 255
                                    ));
                                }
                            }
                        }
                    }
                });
    }

    // --- NOVA FUNÇÃO: Carrega posts filtrados pelo ID ---
    private void carregarPostsDoUtilizador(String uid) {
        db.collection("posts")
                .whereEqualTo("userId", uid) // FILTRO: Só posts deste user
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        postList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Post post = doc.toObject(Post.class);
                            post.setPostId(doc.getId()); // Importante para likes funcionarem
                            postList.add(post);
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                });
    }
}