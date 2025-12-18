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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // 3. Inicializar Componentes da UI
        initViews();

        // 4. Configurar Botões de Navegação
        btnBack.setOnClickListener(v -> finish());
        setupOptionsButton();

        btnMessage.setOnClickListener(v ->
                Toast.makeText(this, "Chat em breve!", Toast.LENGTH_SHORT).show()
        );

        // 5. Carregar Dados do Firestore
        carregarDadosPerfil();
        carregarPostsDoUtilizador();

        // 6. Lógica do Botão Seguir
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            // Esconde botões se for o próprio perfil
            btnFollow.setVisibility(View.GONE);
            btnMessage.setVisibility(View.GONE);
        } else {
            verificarSeJaSegue();
            btnFollow.setOnClickListener(v -> {
                if (isFollowing) deixarDeSeguir();
                else seguirUtilizador();
            });
        }

        // 7. Configurar cliques nos números de seguidores/seguindo
        setupClickListenersCount();
    }

    private void initViews() {
        imgProfile = findViewById(R.id.publicProfileImage);
        txtName = findViewById(R.id.publicProfileName);
        txtCourse = findViewById(R.id.publicProfileCourse);
        txtUni = findViewById(R.id.publicProfileUni);
        txtFollowers = findViewById(R.id.publicFollowersCount);
        txtFollowing = findViewById(R.id.publicFollowingCount);

        btnFollow = findViewById(R.id.btnFollowProfile);
        btnMessage = findViewById(R.id.btnMessageProfile);
        btnBack = findViewById(R.id.btnBackPublic);
        btnOptions = findViewById(R.id.btnProfileOptions);

        recyclerView = findViewById(R.id.recyclerPublicPosts);
        emptyView = findViewById(R.id.emptyViewPublic);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void carregarDadosPerfil() {
        db.collection("users").document(targetUserId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists()) return;

                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        txtName.setText(user.getNome());
                        txtCourse.setText(user.getCurso());
                        txtUni.setText(user.getUniversidade());

                        // Proteção contra números negativos na UI
                        long followers = documentSnapshot.getLong("followersCount") != null ? documentSnapshot.getLong("followersCount") : 0;
                        long following = documentSnapshot.getLong("followingCount") != null ? documentSnapshot.getLong("followingCount") : 0;

                        txtFollowers.setText(String.valueOf(Math.max(0, followers)));
                        txtFollowing.setText(String.valueOf(Math.max(0, following)));

                        String photoUrl = user.getPhotoUrl();
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this).load(photoUrl).circleCrop().into(imgProfile);
                        }
                    }
                });
    }

    private void verificarSeJaSegue() {
        if (currentUserId == null) return;

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
                        btnFollow.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500));
                        btnFollow.setTextColor(Color.WHITE);
                    }
                });
    }

    private void seguirUtilizador() {
        if (currentUserId == null) return;

        btnFollow.setEnabled(false);
        WriteBatch batch = db.batch();

        // Referências aos documentos
        DocumentReference refFollowing = db.collection("users").document(currentUserId).collection("following").document(targetUserId);
        DocumentReference refFollower = db.collection("users").document(targetUserId).collection("followers").document(currentUserId);
        DocumentReference meuPerfil = db.collection("users").document(currentUserId);
        DocumentReference outroPerfil = db.collection("users").document(targetUserId);

        // Dados da relação
        Map<String, Object> dados = new HashMap<>();
        dados.put("uid", targetUserId);
        dados.put("timestamp", FieldValue.serverTimestamp()); // Melhor que System.currentTimeMillis() para consistência de servidor

        Map<String, Object> dados2 = new HashMap<>();
        dados2.put("uid", currentUserId);
        dados2.put("timestamp", FieldValue.serverTimestamp());

        batch.set(refFollowing, dados);
        batch.set(refFollower, dados2);

        // Incremento Atómico: Inseparável da criação dos documentos acima
        batch.update(meuPerfil, "followingCount", FieldValue.increment(1));
        batch.update(outroPerfil, "followersCount", FieldValue.increment(1));

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "A seguir!", Toast.LENGTH_SHORT).show();
            enviarNotificacao(targetUserId, "follow", "começou a seguir-te", null);
            btnFollow.setEnabled(true);
        }).addOnFailureListener(e -> {
            btnFollow.setEnabled(true);
            Toast.makeText(this, "Erro ao seguir", Toast.LENGTH_SHORT).show();
        });
    }

    private void deixarDeSeguir() {
        if (currentUserId == null) return;

        btnFollow.setEnabled(false);
        WriteBatch batch = db.batch();

        DocumentReference refFollowing = db.collection("users").document(currentUserId).collection("following").document(targetUserId);
        DocumentReference refFollower = db.collection("users").document(targetUserId).collection("followers").document(currentUserId);
        DocumentReference meuPerfil = db.collection("users").document(currentUserId);
        DocumentReference outroPerfil = db.collection("users").document(targetUserId);

        // Remove os documentos das sub-coleções
        batch.delete(refFollowing);
        batch.delete(refFollower);

        // Decrementa os contadores (O Firestore impede que fiquem abaixo de zero se as regras estiverem certas)
        batch.update(meuPerfil, "followingCount", FieldValue.increment(-1));
        batch.update(outroPerfil, "followersCount", FieldValue.increment(-1));

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Deixaste de seguir.", Toast.LENGTH_SHORT).show();
            btnFollow.setEnabled(true);
        }).addOnFailureListener(e -> {
            btnFollow.setEnabled(true);
            Toast.makeText(this, "Erro ao processar", Toast.LENGTH_SHORT).show();
        });
    }

    private void carregarPostsDoUtilizador() {
        db.collection("posts")
                .whereEqualTo("userId", targetUserId)
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
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setupOptionsButton() {
        btnOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(0, 1, 0, "Denunciar");
            popup.getMenu().add(0, 2, 0, "Bloquear");

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    Toast.makeText(this, "Denúncia enviada.", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (item.getItemId() == 2) {
                    bloquearUtilizador();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void bloquearUtilizador() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(currentUserId)
                .collection("blocked").document(targetUserId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Utilizador bloqueado.", Toast.LENGTH_SHORT).show();
                    if (isFollowing) deixarDeSeguir();
                    finish();
                });
    }

    private void setupClickListenersCount() {
        View.OnClickListener listenerFollowers = v -> abrirLista("followers");
        txtFollowers.setOnClickListener(listenerFollowers);

        View.OnClickListener listenerFollowing = v -> abrirLista("following");
        txtFollowing.setOnClickListener(listenerFollowing);
    }

    private void abrirLista(String type) {
        Intent intent = new Intent(this, UserListActivity.class);
        intent.putExtra("userId", targetUserId);
        intent.putExtra("type", type);
        startActivity(intent);
    }

    private void enviarNotificacao(String receiverId, String type, String msg, String postId) {
        if (receiverId.equals(currentUserId)) return;

        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            User eu = doc.toObject(User.class);
            if (eu != null) {
                Map<String, Object> notif = new HashMap<>();
                notif.put("targetUserId", receiverId);
                notif.put("fromUserId", currentUserId);
                notif.put("fromUserName", eu.getNome());
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