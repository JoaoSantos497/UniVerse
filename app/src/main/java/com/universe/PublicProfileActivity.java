package com.universe;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView; // <--- IMPORTANTE: Tens de ter isto
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // <--- IMPORTANTE: Tens de ter isto
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PublicProfileActivity extends AppCompatActivity {

    // 1. MUDANÇA: Agora é ImageView (antes era TextView txtAvatar)
    private ImageView imgAvatar;

    private TextView txtName, txtUsername, txtCourse, txtUni;
    private TextView txtFollowersCount, txtFollowingCount;
    private Button btnBack, btnFollow;

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String targetUserId;
    private String currentUserId;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        // 2. MUDANÇA: Ligar ao ID do XML
        imgAvatar = findViewById(R.id.publicProfileAvatar);

        txtName = findViewById(R.id.publicProfileName);
        txtUsername = findViewById(R.id.publicProfileUsername);
        txtCourse = findViewById(R.id.publicProfileCourse);
        txtUni = findViewById(R.id.publicProfileUni);
        txtFollowersCount = findViewById(R.id.txtFollowersCount);
        txtFollowingCount = findViewById(R.id.txtFollowingCount);
        btnBack = findViewById(R.id.btnBack);
        btnFollow = findViewById(R.id.btnFollow);

        recyclerView = findViewById(R.id.recyclerProfilePosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // Receber o ID da pessoa que queremos ver
        targetUserId = getIntent().getStringExtra("targetUserId");

        if (targetUserId != null) {
            if (targetUserId.equals(currentUserId)) {
                btnFollow.setVisibility(View.GONE);
            } else {
                verificarSeJaSegue();
            }

            carregarDadosPerfil(targetUserId);
            carregarPostsDoUtilizador(targetUserId);
            carregarContadores();
        }

        btnBack.setOnClickListener(v -> finish());

        btnFollow.setOnClickListener(v -> {
            if (isFollowing) {
                deixarDeSeguir();
            } else {
                comecarASeguir();
            }
        });
    }

    private void verificarSeJaSegue() {
        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        isFollowing = true;
                        btnFollow.setText("A Seguir");
                        btnFollow.setBackgroundColor(Color.GRAY);
                    } else {
                        isFollowing = false;
                        btnFollow.setText("Seguir");
                        btnFollow.setBackgroundColor(Color.parseColor("#6200EE"));
                    }
                });
    }

    private void comecarASeguir() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId).set(data);
        db.collection("users").document(targetUserId)
                .collection("followers").document(currentUserId).set(data);
    }

    private void deixarDeSeguir() {
        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId).delete();
        db.collection("users").document(targetUserId)
                .collection("followers").document(currentUserId).delete();
    }

    private void carregarContadores() {
        db.collection("users").document(targetUserId).collection("followers")
                .addSnapshotListener((value, error) -> {
                    if (value != null) txtFollowersCount.setText(String.valueOf(value.size()));
                });
        db.collection("users").document(targetUserId).collection("following")
                .addSnapshotListener((value, error) -> {
                    if (value != null) txtFollowingCount.setText(String.valueOf(value.size()));
                });
    }

    private void carregarDadosPerfil(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            txtName.setText(user.getNome());
                            txtCourse.setText(user.getCurso());
                            txtUni.setText(user.getUniversidade());

                            if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                                txtUsername.setText("@" + user.getUsername());
                            } else {
                                txtUsername.setText("");
                            }

                            // 3. MUDANÇA: Carregar foto com GLIDE
                            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(user.getPhotoUrl())
                                        .circleCrop()
                                        .into(imgAvatar);
                            } else {
                                imgAvatar.setImageResource(R.drawable.circle_bg);
                            }
                        }
                    }
                });
    }

    private void carregarPostsDoUtilizador(String uid) {
        db.collection("posts")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());
                        postList.add(post);
                    }
                    postAdapter.notifyDataSetChanged();
                });
    }
}