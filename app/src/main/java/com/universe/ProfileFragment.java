package com.universe;

import android.content.Intent;
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

        // 1. Ligar componentes
        profileAvatar = view.findViewById(R.id.profileAvatar);
        profileName = view.findViewById(R.id.profileName);
        profileUsername = view.findViewById(R.id.profileUsername);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileCurso = view.findViewById(R.id.profileCurso);
        profileUni = view.findViewById(R.id.profileUni);

        txtMyFollowers = view.findViewById(R.id.txtMyFollowers);
        txtMyFollowing = view.findViewById(R.id.txtMyFollowing);
        emptyView = view.findViewById(R.id.emptyView);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSettings = view.findViewById(R.id.btnSettings);

        // 2. Configurar a Lista de Posts
        recyclerView = view.findViewById(R.id.recyclerProfilePosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // 3. Ações dos Botões
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        // 4. CARREGAR DADOS E CONFIGURAR CLIQUES NAS LISTAS
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String myUid = user.getUid();

            carregarDadosEmTempoReal(myUid);
            carregarMeusContadores(myUid);
            carregarMeusPosts(myUid);

            // --- CORREÇÃO: Os cliques têm de estar aqui DENTRO ---

            // Clique nos Seguidores (Apanha o pai do TextView para clicar na área toda)
            if (txtMyFollowers.getParent() instanceof View) {
                ((View) txtMyFollowers.getParent()).setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), UserListActivity.class);
                    intent.putExtra("userId", myUid);
                    intent.putExtra("type", "followers");
                    startActivity(intent);
                });
            }

            // Clique no A Seguir
            if (txtMyFollowing.getParent() instanceof View) {
                ((View) txtMyFollowing.getParent()).setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), UserListActivity.class);
                    intent.putExtra("userId", myUid);
                    intent.putExtra("type", "following");
                    startActivity(intent);
                });
            }
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
                            User aluno = documentSnapshot.toObject(User.class);

                            if (aluno != null) {
                                profileName.setText(aluno.getNome());
                                profileCurso.setText(aluno.getCurso());

                                // Se tiveres os campos extra no layout:
                                // profileUni.setText(aluno.getUniversidade());
                                // profileEmail.setText(aluno.getEmail());
                                // if (profileUsername != null) profileUsername.setText("@" + aluno.getUsername());

                                if (aluno.getPhotoUrl() != null && !aluno.getPhotoUrl().isEmpty()) {
                                    if (getContext() != null) {
                                        Glide.with(getContext())
                                                .load(aluno.getPhotoUrl())
                                                .circleCrop()
                                                .into(profileAvatar);
                                    }
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
                            if (post != null) {
                                post.setPostId(doc.getId());
                                postList.add(post);
                            }
                        }
                        postAdapter.notifyDataSetChanged();

                        // Lógica para mostrar/esconder view vazia (se tiveres no XML)
                        /*
                        if (postList.isEmpty() && emptyView != null) {
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        } else if (emptyView != null) {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                        }
                        */
                    }
                });
    }
}