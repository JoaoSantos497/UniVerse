package com.universe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class PublicProfileActivity extends AppCompatActivity {

    // UI
    private ImageView imgProfile;
    private TextView txtName, txtCourse, txtUni, txtFollowers, txtFollowing;
    private Button btnFollow, btnMessage;
    private ImageButton btnBack, btnOptions;

    // Feed do Utilizador
    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private PostAdapter postAdapter;
    private List<Post> postList;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Dados
    private String targetUserId;
    private String currentUserId;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        // 1. Receber ID
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

        // 3. Ligar UI (Atualizado com os IDs novos)
        initViews();

        // 4. Configurar Botões
        btnBack.setOnClickListener(v -> finish());
        setupOptionsButton(); // Menu dos 3 pontinhos

        btnMessage.setOnClickListener(v ->
                Toast.makeText(this, "Chat em breve!", Toast.LENGTH_SHORT).show()
        );

        // 5. Carregar Dados
        carregarDadosPerfil();
        carregarPostsDoUtilizador(); // <--- NOVO: Carrega os posts
        contarSeguidoresESeguindo();

        // 6. Lógica de Seguir
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            // É o meu próprio perfil
            btnFollow.setVisibility(View.GONE);
            btnMessage.setVisibility(View.GONE);
        } else {
            verificarSeJaSegue();
            btnFollow.setOnClickListener(v -> {
                if (isFollowing) deixarDeSeguir();
                else seguirUtilizador();
            });
        }

        // 7. Cliques nas Listas de Seguidores
        setupClickListenersCount();
    }

    private void initViews() {
        imgProfile = findViewById(R.id.publicProfileImage);
        txtName = findViewById(R.id.publicProfileName);
        txtCourse = findViewById(R.id.publicProfileCourse);
        txtUni = findViewById(R.id.publicProfileUni); // Novo campo
        txtFollowers = findViewById(R.id.publicFollowersCount);
        txtFollowing = findViewById(R.id.publicFollowingCount);

        btnFollow = findViewById(R.id.btnFollowProfile);
        btnMessage = findViewById(R.id.btnMessageProfile);
        btnBack = findViewById(R.id.btnBackPublic);
        btnOptions = findViewById(R.id.btnProfileOptions);

        // RecyclerView para os posts
        recyclerView = findViewById(R.id.recyclerPublicPosts);
        emptyView = findViewById(R.id.emptyViewPublic);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // Melhorar performance do scroll dentro do ScrollView principal se houver
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void setupOptionsButton() {
        btnOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(0, 1, 0, "Denunciar");
            popup.getMenu().add(0, 2, 0, "Bloquear");

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 1) {
                    Toast.makeText(this, "Denúncia enviada.", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == 2) {
                    bloquearUtilizador(); // <--- CHAMA O NOVO MÉTODO
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    // --- NOVO MeTODO PARA BLOQUEAR REALMENTE ---
    private void bloquearUtilizador() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());

        // Grava na sub-coleção "blocked" do teu utilizador
        db.collection("users").document(currentUserId)
                .collection("blocked").document(targetUserId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Utilizador bloqueado.", Toast.LENGTH_SHORT).show();

                    // Opcional: Deixar de seguir automaticamente
                    deixarDeSeguir();
                    finish(); // Fecha o perfil porque o bloqueaste
                });
    }

    private void carregarDadosPerfil() {
        db.collection("users").document(targetUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            txtName.setText(user.getNome());
                            txtCourse.setText(user.getCurso());
                            txtUni.setText(user.getUniversidade());

                            // Tenta carregar a foto (vários nomes possíveis)
                            String photoUrl = user.getPhotoUrl();
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(this).load(photoUrl).circleCrop().into(imgProfile);
                            }
                        }
                    }
                });
    }

    private void carregarPostsDoUtilizador() {
        // Carrega posts onde userId == targetUserId
        db.collection("posts")
                .whereEqualTo("userId", targetUserId) // <--- Usa 'userId' como pediste
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null && !value.isEmpty()) {
                        postList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                postList.add(post);
                            }
                        }
                        postAdapter.notifyDataSetChanged();

                        // Mostrar lista, esconder aviso de vazio
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    } else {
                        // Mostrar aviso de vazio, esconder lista
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                });
    }

    // --- LÓGICA DE SEGUIR ---

    private void verificarSeJaSegue() {
        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null && value.exists()) {
                        isFollowing = true;
                        btnFollow.setText("A Seguir");
                        btnFollow.setBackgroundColor(Color.GRAY);
                        btnFollow.setTextColor(Color.BLACK);
                    } else {
                        isFollowing = false;
                        btnFollow.setText("Seguir");
                        // Roxo do tema (ou usa ContextCompat.getColor)
                        btnFollow.setBackgroundColor(Color.parseColor("#6200EE"));
                        btnFollow.setTextColor(Color.WHITE);
                    }
                });
    }

    private void seguirUtilizador() {
        Map<String, Object> dados = new HashMap<>();
        // Adicionar timestamp para saber quando começou a seguir
        dados.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId).set(dados);

        db.collection("users").document(targetUserId)
                .collection("followers").document(currentUserId).set(dados);

        Toast.makeText(this, "A seguir!", Toast.LENGTH_SHORT).show();

        // Enviar notificação (apenas se for seguir)
        enviarNotificacao(targetUserId, "follow", "começou a seguir-te", null);
    }

    private void deixarDeSeguir() {
        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId).delete();

        db.collection("users").document(targetUserId)
                .collection("followers").document(currentUserId).delete();

        Toast.makeText(this, "Deixaste de seguir.", Toast.LENGTH_SHORT).show();
    }

    private void contarSeguidoresESeguindo() {
        db.collection("users").document(targetUserId).collection("followers")
                .addSnapshotListener((value, error) -> {
                    if (value != null) txtFollowers.setText(String.valueOf(value.size()));
                });

        db.collection("users").document(targetUserId).collection("following")
                .addSnapshotListener((value, error) -> {
                    if (value != null) txtFollowing.setText(String.valueOf(value.size()));
                });
    }

    private void setupClickListenersCount() {
        View.OnClickListener listenerFollowers = v -> abrirLista("followers");
        txtFollowers.setOnClickListener(listenerFollowers);
        ((View) txtFollowers.getParent()).setOnClickListener(listenerFollowers);

        View.OnClickListener listenerFollowing = v -> abrirLista("following");
        txtFollowing.setOnClickListener(listenerFollowing);
        ((View) txtFollowing.getParent()).setOnClickListener(listenerFollowing);
    }

    private void abrirLista(String type) {
        Intent intent = new Intent(this, UserListActivity.class);
        intent.putExtra("userId", targetUserId);
        intent.putExtra("type", type);
        startActivity(intent);
    }

    // Metodo de Notificação
    private void enviarNotificacao(String receiverId, String type, String msg, String postId) {
        if (receiverId.equals(currentUserId)) return;

        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            User eu = doc.toObject(User.class);
            if (eu != null) {
                // Certifica-te que tens a classe Notification criada com estes campos
                Map<String, Object> notif = new HashMap<>();
                notif.put("targetUserId", receiverId);
                notif.put("fromUserId", currentUserId);
                notif.put("fromUserName", eu.getNome());
                notif.put("fromUserPhoto", eu.getPhotoUrl() != null ? eu.getPhotoUrl() : "");
                notif.put("type", type);
                notif.put("message", msg);
                notif.put("postId", postId);
                notif.put("timestamp", System.currentTimeMillis());
                notif.put("read", false);

                db.collection("notifications").add(notif);
            }
        });
    }
}