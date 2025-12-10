package com.universe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    // Variáveis da UI
    private ImageView profileAvatar;
    private TextView profileName, profileCurso, profileUni, profileEmail, profileUsername;
    private TextView txtMyFollowers, txtMyFollowing;
    private LinearLayout emptyView;

    // REMOVI O btnLogout DAQUI
    private Button btnEditProfile;
    private ImageButton btnSettings;

    // Variáveis para a Lista de Posts
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;

    // Variáveis do Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ligar componentes
        profileAvatar = view.findViewById(R.id.profileAvatar);
        profileName = view.findViewById(R.id.profileName);
        profileUsername = view.findViewById(R.id.profileUsername);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileCurso = view.findViewById(R.id.profileCurso);
        profileUni = view.findViewById(R.id.profileUni);

        txtMyFollowers = view.findViewById(R.id.txtMyFollowers);
        txtMyFollowing = view.findViewById(R.id.txtMyFollowing);
        emptyView = view.findViewById(R.id.emptyView);

        // REMOVI A LIGAÇÃO DO ID btnLogout DAQUI
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSettings = view.findViewById(R.id.btnSettings);

        // Configurar a Lista de Posts
        recyclerView = view.findViewById(R.id.recyclerProfilePosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // Ações dos Botões
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        // CARREGAR DADOS
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            carregarDadosEmTempoReal(user.getUid());
            carregarMeusContadores(user.getUid());
            carregarMeusPosts(user.getUid());
        }

        return view;
    }

    private void carregarDadosEmTempoReal(String uid) {
        db.collection("users").document(uid)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) return;

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            // Converter documento para Objeto User
                            User aluno = documentSnapshot.toObject(User.class);

                            if (aluno != null) {
                                // Textos
                                profileName.setText(aluno.getNome());
                                profileCurso.setText(aluno.getCurso());
                                profileUni.setText(aluno.getUniversidade());
                                profileEmail.setText(aluno.getEmail());

                                if (aluno.getUsername() != null && !aluno.getUsername().isEmpty()) {
                                    profileUsername.setText("@" + aluno.getUsername());
                                } else {
                                    profileUsername.setText("");
                                }

                                // Carregar FOTO com Glide
                                if (aluno.getPhotoUrl() != null && !aluno.getPhotoUrl().isEmpty()) {
                                    Glide.with(getContext())
                                            .load(aluno.getPhotoUrl())
                                            .circleCrop()
                                            .into(profileAvatar);
                                } else {
                                    profileAvatar.setImageResource(R.drawable.circle_bg);
                                }
                            }
                        }
                    }
                });
    }

    private void carregarMeusContadores(String uid) {
        db.collection("users").document(uid).collection("followers")
                .addSnapshotListener((value, error) -> {
                    if (value != null) txtMyFollowers.setText(String.valueOf(value.size()));
                });

        db.collection("users").document(uid).collection("following")
                .addSnapshotListener((value, error) -> {
                    if (value != null) txtMyFollowing.setText(String.valueOf(value.size()));
                });
    }

    private void carregarMeusPosts(String uid) {
        db.collection("posts")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        postList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            post.setPostId(doc.getId());
                            postList.add(post);
                        }
                        postAdapter.notifyDataSetChanged();

                        if (postList.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                        }
                    }
                });
    }
}