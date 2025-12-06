package com.universe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton; // <--- NÃO ESQUECER ESTE IMPORT
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    // Variáveis da UI
    private TextView profileAvatar, profileName, profileCurso, profileUni, profileEmail, profileUsername;
    private TextView txtMyFollowers, txtMyFollowing;
    private Button btnLogout, btnEditProfile;

    // 1. AQUI ESTAVA A FALTAR A VARIÁVEL
    private ImageButton btnSettings;

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

        txtMyFollowers = view.findViewById(R.id.txtMyFollowers);
        txtMyFollowing = view.findViewById(R.id.txtMyFollowing);

        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        // 2. AQUI ESTAVA A FALTAR A LIGAÇÃO AO XML
        btnSettings = view.findViewById(R.id.btnSettings);

        recyclerView = view.findViewById(R.id.recyclerProfilePosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // 3. AQUI ESTAVA A FALTAR O CLIQUE
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

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
            // Nota: Se quiseres atualização em tempo real, usa o SnapshotListener aqui
            // como fizemos antes, em vez do .get()
            profileEmail.setText(user.getEmail());
            carregarDadosDoUtilizador(user.getUid());
            carregarMeusPosts(user.getUid());
            carregarMeusContadores(user.getUid());
        }
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