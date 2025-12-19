package com.universe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PublicProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private TextView txtName, txtCourse, txtUni, txtFollowers, txtFollowing;
    private Button btnFollow, btnMessage;
    private ImageButton btnBack, btnOptions;

    private LinearLayout layoutBlocked;
    private Button btnUnblock;
    private View profileContent;

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private PostAdapter postAdapter;
    private List<Post> postList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Gestão de Listeners para evitar crashes e gastos de dados
    private ListenerRegistration perfilListener, blockListener, followListener, postListener;

    private String targetUserId;
    private String currentUserId;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        targetUserId = getIntent().getStringExtra("targetUserId");
        if (targetUserId == null) {
            Toast.makeText(this, "Erro: Utilizador não encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        initViews();
        setupButtons();

        // Ordem de execução: 1. Bloqueio -> 2. Dados
        verificarBloqueio();
        carregarDadosPerfil();
        carregarPostsDoUtilizador();
        setupFollowLogic();
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

        layoutBlocked = findViewById(R.id.layoutBlocked);
        btnUnblock = findViewById(R.id.btnUnblock);
        profileContent = findViewById(R.id.profileContentContainer);

        recyclerView = findViewById(R.id.recyclerPublicPosts);
        emptyView = findViewById(R.id.emptyViewPublic);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());
        setupOptionsButton();
        btnMessage.setOnClickListener(v -> Toast.makeText(this, "Chat em breve!", Toast.LENGTH_SHORT).show());
        btnUnblock.setOnClickListener(v -> desbloquearUtilizador());
    }

    private void verificarBloqueio() {
        if (currentUserId == null) return;

        blockListener = db.collection("users").document(currentUserId)
                .collection("blocked").document(targetUserId)
                .addSnapshotListener((value, error) -> {
                    if (isDestroyed() || isFinishing()) return;
                    mostrarInterfaceBloqueado(value != null && value.exists());
                });
    }

    private void mostrarInterfaceBloqueado(boolean isBlocked) {
        layoutBlocked.setVisibility(isBlocked ? View.VISIBLE : View.GONE);
        profileContent.setVisibility(isBlocked ? View.GONE : View.VISIBLE);
        btnOptions.setVisibility(isBlocked ? View.GONE : View.VISIBLE);
    }

    private void bloquearUtilizador() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", FieldValue.serverTimestamp());
        data.put("uid", targetUserId);

        db.collection("users").document(currentUserId)
                .collection("blocked").document(targetUserId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Utilizador bloqueado.", Toast.LENGTH_SHORT).show();
                    if (isFollowing) deixarDeSeguir();
                });
    }

    private void desbloquearUtilizador() {
        db.collection("users").document(currentUserId)
                .collection("blocked").document(targetUserId)
                .delete();
    }

    private void carregarDadosPerfil() {
        perfilListener = db.collection("users").document(targetUserId)
                .addSnapshotListener((doc, error) -> {
                    // CORREÇÃO: Evita o crash do Glide verificando se a Activity está viva
                    if (isDestroyed() || isFinishing()) return;
                    if (error != null || doc == null || !doc.exists()) return;

                    User user = doc.toObject(User.class);
                    if (user != null) {
                        txtName.setText(user.getNome());
                        txtCourse.setText(user.getCurso());
                        txtUni.setText(user.getUniversidade());

                        long followers = doc.getLong("followersCount") != null ? doc.getLong("followersCount") : 0;
                        long following = doc.getLong("followingCount") != null ? doc.getLong("followingCount") : 0;

                        txtFollowers.setText(String.valueOf(Math.max(0, followers)));
                        txtFollowing.setText(String.valueOf(Math.max(0, following)));

                        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                            Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(imgProfile);
                        }
                    }
                });
    }

    private void setupFollowLogic() {
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            btnFollow.setVisibility(View.GONE);
            btnMessage.setVisibility(View.GONE);
        } else {
            verificarSeJaSegue();
            btnFollow.setOnClickListener(v -> {
                if (isFollowing) deixarDeSeguir();
                else seguirUtilizador();
            });
        }
    }

    private void verificarSeJaSegue() {
        followListener = db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId)
                .addSnapshotListener((value, error) -> {
                    if (isDestroyed() || isFinishing()) return;

                    isFollowing = value != null && value.exists();
                    if (isFollowing) {
                        btnFollow.setText("A Seguir");
                        btnFollow.setBackgroundColor(Color.GRAY);
                        btnFollow.setTextColor(Color.BLACK);
                    } else {
                        btnFollow.setText("Seguir");
                        btnFollow.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500));
                        btnFollow.setTextColor(Color.WHITE);
                    }
                });
    }

    private void seguirUtilizador() {
        if (isFollowing) return;
        btnFollow.setEnabled(false);

        DocumentReference refFollowing = db.collection("users").document(currentUserId).collection("following").document(targetUserId);
        refFollowing.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                WriteBatch batch = db.batch();

                Map<String, Object> data = new HashMap<>();
                data.put("uid", targetUserId);
                data.put("timestamp", FieldValue.serverTimestamp());

                Map<String, Object> dataFollower = new HashMap<>();
                dataFollower.put("uid", currentUserId);
                dataFollower.put("timestamp", FieldValue.serverTimestamp());

                batch.set(refFollowing, data);
                batch.set(refTargetFollower(), dataFollower);
                batch.update(db.collection("users").document(currentUserId), "followingCount", FieldValue.increment(1));
                batch.update(db.collection("users").document(targetUserId), "followersCount", FieldValue.increment(1));

                batch.commit().addOnSuccessListener(aVoid -> {
                    enviarNotificacao(targetUserId, "follow", "começou a seguir-te", null);
                    btnFollow.setEnabled(true);
                }).addOnFailureListener(e -> btnFollow.setEnabled(true));
            } else {
                btnFollow.setEnabled(true);
            }
        });
    }

    private void deixarDeSeguir() {
        btnFollow.setEnabled(false);
        WriteBatch batch = db.batch();

        batch.delete(db.collection("users").document(currentUserId).collection("following").document(targetUserId));
        batch.delete(db.collection("users").document(targetUserId).collection("followers").document(currentUserId));
        batch.update(db.collection("users").document(currentUserId), "followingCount", FieldValue.increment(-1));
        batch.update(db.collection("users").document(targetUserId), "followersCount", FieldValue.increment(-1));

        batch.commit().addOnSuccessListener(aVoid -> btnFollow.setEnabled(true))
                .addOnFailureListener(e -> btnFollow.setEnabled(true));
    }

    private void carregarPostsDoUtilizador() {
        Query query = db.collection("posts")
                .whereEqualTo("userId", targetUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        postListener = query.addSnapshotListener((value, error) -> {
            if (isDestroyed() || isFinishing()) return;
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

    private DocumentReference refTargetFollower() {
        return db.collection("users").document(targetUserId).collection("followers").document(currentUserId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ESSENCIAL: Remove todos os listeners para evitar crashes e gastos desnecessários
        if (perfilListener != null) perfilListener.remove();
        if (blockListener != null) blockListener.remove();
        if (followListener != null) followListener.remove();
        if (postListener != null) postListener.remove();
    }

    private void setupOptionsButton() {
        btnOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(0, 1, 0, "Denunciar");
            popup.getMenu().add(0, 2, 0, "Bloquear");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 2) { bloquearUtilizador(); return true; }
                return false;
            });
            popup.show();
        });
    }

    private void setupClickListenersCount() {
        txtFollowers.setOnClickListener(v -> abrirLista("followers"));
        txtFollowing.setOnClickListener(v -> abrirLista("following"));
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
                notif.put("timestamp", FieldValue.serverTimestamp());
                notif.put("read", false);
                db.collection("notifications").add(notif);
            }
        });
    }
}