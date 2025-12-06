package com.universe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    // Variáveis da UI
    private TextView profileAvatar, profileName, profileCurso, profileUni, profileEmail, profileUsername;
    private TextView txtMyFollowers, txtMyFollowing; // <--- NOVOS
    private Button btnLogout, btnEditProfile;

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;

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

        // Ligar Contadores
        txtMyFollowers = view.findViewById(R.id.txtMyFollowers);
        txtMyFollowing = view.findViewById(R.id.txtMyFollowing);

        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        recyclerView = view.findViewById(R.id.recyclerProfilePosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            profileEmail.setText(user.getEmail());
            carregarDadosDoUtilizador(user.getUid());
            carregarMeusPosts(user.getUid());
            carregarMeusContadores(user.getUid()); // <--- Chama a função nova
        }
    }

    // --- NOVA FUNÇÃO PARA CONTAR ---
    private void carregarMeusContadores(String uid) {
        // Meus Seguidores
        db.collection("users").document(uid).collection("followers")
                .addSnapshotListener((value, error) -> {
                    if (value != null) txtMyFollowers.setText(String.valueOf(value.size()));
                });

        // Quem eu Sigo
        db.collection("users").document(uid).collection("following")
                .addSnapshotListener((value, error) -> {
                    if (value != null) txtMyFollowing.setText(String.valueOf(value.size()));
                });
    }

    private void carregarDadosDoUtilizador(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User aluno = documentSnapshot.toObject(User.class);
                        if (aluno != null) {
                            profileName.setText(aluno.getNome());
                            profileCurso.setText(aluno.getCurso());
                            profileUni.setText(aluno.getUniversidade());

                            if (aluno.getUsername() != null && !aluno.getUsername().isEmpty()) {
                                profileUsername.setText("@" + aluno.getUsername());
                            } else {
                                profileUsername.setText("");
                            }

                            String nome = aluno.getNome();
                            if(nome != null && !nome.isEmpty()) {
                                String inicial = nome.substring(0, 1).toUpperCase();
                                profileAvatar.setText(inicial);
                                int hash = nome.hashCode();
                                profileAvatar.getBackground().setTint(Color.rgb(
                                        Math.abs(hash * 25) % 255,
                                        Math.abs(hash * 80) % 255,
                                        Math.abs(hash * 13) % 255
                                ));
                            }
                        }
                    }
                });
    }

    private void carregarMeusPosts(String uid) {
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