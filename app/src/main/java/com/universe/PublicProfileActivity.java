package com.universe;

import android.content.Intent; // <--- Importante
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class PublicProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private TextView txtName, txtCourse, txtFollowers, txtFollowing;
    private Button btnFollow;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;


    private String targetUserId;
    private String currentUserId;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        // 1. Receber ID do utilizador alvo
        targetUserId = getIntent().getStringExtra("targetUserId");

        if (targetUserId == null) {
            Toast.makeText(this, "Erro: Utilizador não encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        // 3. Ligar UI
        imgProfile = findViewById(R.id.publicProfileImage);
        txtName = findViewById(R.id.publicProfileName);
        txtCourse = findViewById(R.id.publicProfileCourse);
        txtFollowers = findViewById(R.id.publicFollowersCount);
        txtFollowing = findViewById(R.id.publicFollowingCount);
        btnFollow = findViewById(R.id.btnFollowProfile);

        // 4. Carregar Dados do Perfil
        carregarDadosPerfil();

        // 5. Verificar se é o próprio utilizador
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            // Se estou a ver o meu próprio perfil, escondo o botão "Seguir"
            btnFollow.setVisibility(View.GONE);
        } else {
            // Se for outra pessoa, verifico se já sigo
            verificarSeJaSegue();

            // Configurar clique do botão
            btnFollow.setOnClickListener(v -> {
                if (isFollowing) {
                    deixarDeSeguir();
                } else {
                    seguirUtilizador();
                }
            });
        }

        // 6. Contar Seguidores e A Seguir (Em tempo real)
        contarSeguidoresESeguindo();

        // Clique no número de seguidores
        txtFollowers.setOnClickListener(v -> abrirLista("followers"));
        // Clique na caixa toda (pai) dos seguidores
        ((View) txtFollowers.getParent()).setOnClickListener(v -> abrirLista("followers"));

        // Clique no número de A Seguir
        txtFollowing.setOnClickListener(v -> abrirLista("following"));
        // Clique na caixa toda (pai) do A Seguir
        ((View) txtFollowing.getParent()).setOnClickListener(v -> abrirLista("following"));
    }

    private void abrirLista(String type) {
        Intent intent = new Intent(this, UserListActivity.class);
        intent.putExtra("userId", targetUserId); // ID da pessoa que estamos a ver
        intent.putExtra("type", type); // "followers" ou "following"
        startActivity(intent);
    }

    private void carregarDadosPerfil() {
        db.collection("users").document(targetUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            txtName.setText(user.getNome());
                            txtCourse.setText(user.getCurso());

                            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(imgProfile);
                            }
                        }
                    }
                });
    }

    // --- LÓGICA DO FOLLOW SYSTEM ---

    private void verificarSeJaSegue() {
        // Verifica na MINHA lista de 'following' se o targetUserId está lá
        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) return;

                        if (value != null && value.exists()) {
                            // JÁ SIGO
                            isFollowing = true;
                            btnFollow.setText("A Seguir");
                            btnFollow.setBackgroundColor(Color.GRAY); // Botão cinzento
                            btnFollow.setTextColor(Color.BLACK);
                        } else {
                            // NÃO SIGO
                            isFollowing = false;
                            btnFollow.setText("Seguir");
                            btnFollow.setBackgroundColor(Color.parseColor("#6200EE")); // Botão Roxo
                            btnFollow.setTextColor(Color.WHITE);
                        }
                    }
                });
    }

    private void seguirUtilizador() {
        // Mapa vazio só para criar o documento
        Map<String, Object> dados = new HashMap<>();

        // 1. Adicionar à minha lista de "Following"
        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId)
                .set(dados);

        // 2. Adicionar à lista de "Followers" dele
        db.collection("users").document(targetUserId)
                .collection("followers").document(currentUserId)
                .set(dados);

        Toast.makeText(this, "A seguir!", Toast.LENGTH_SHORT).show();

        enviarNotificacao(targetUserId, "follow", "começou a seguir-te", null);

        Toast.makeText(this, "A seguir!", Toast.LENGTH_SHORT).show();
    }

    private void deixarDeSeguir() {
        // 1. Remover da minha lista "Following"
        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId)
                .delete();

        // 2. Remover da lista "Followers" dele
        db.collection("users").document(targetUserId)
                .collection("followers").document(currentUserId)
                .delete();

        Toast.makeText(this, "Deixaste de seguir.", Toast.LENGTH_SHORT).show();
    }

    private void contarSeguidoresESeguindo() {
        // Contar quantos Seguidores ele tem
        db.collection("users").document(targetUserId).collection("followers")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        txtFollowers.setText(String.valueOf(value.size()));
                    }
                });

        // Contar quantos ele está A Seguir
        db.collection("users").document(targetUserId).collection("following")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        txtFollowing.setText(String.valueOf(value.size()));
                    }
                });
    }

    // Metodo auxiliar para criar notificação no Firebase
    private void enviarNotificacao(String receiverId, String type, String msg, String postId) {
        if (receiverId.equals(currentUserId)) return; // Não notificar a mim próprio

        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            User eu = doc.toObject(User.class);
            if (eu != null) {
                // Cria o objeto Notificação
                Notification notif = new Notification(
                        currentUserId,
                        eu.getNome(),
                        eu.getPhotoUrl(),
                        receiverId,
                        type,
                        msg,
                        postId,
                        System.currentTimeMillis()
                );

                // Grava no Firebase
                db.collection("notifications").add(notif);
            }
        });
    }

}