package com.universe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
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
    private TextView profileAvatar, profileName, profileCurso, profileUni, profileEmail;
    private Button btnLogout;

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
        FirebaseUser user = mAuth.getCurrentUser();

        // Ligar componentes
        profileAvatar = view.findViewById(R.id.profileAvatar); // O novo TextView
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileCurso = view.findViewById(R.id.profileCurso);
        profileUni = view.findViewById(R.id.profileUni);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Configurar a Lista de Posts
        recyclerView = view.findViewById(R.id.recyclerProfilePosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        if (user != null) {
            profileEmail.setText(user.getEmail());
            carregarDadosDoUtilizador(user.getUid());
            carregarMeusPosts(user.getUid()); // <--- Carrega os posts
        }

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        return view;
    }

    private void carregarDadosDoUtilizador(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User aluno = documentSnapshot.toObject(User.class);
                            if (aluno != null) {
                                profileName.setText(aluno.getNome());
                                profileCurso.setText(aluno.getCurso());
                                profileUni.setText(aluno.getUniversidade());

                                // --- Lógica da Cor do Avatar ---
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
                    }
                });
    }

    // --- NOVA FUNÇÃO: Carrega os TEUS posts ---
    private void carregarMeusPosts(String uid) {
        db.collection("posts")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        postList.clear();
                        if (queryDocumentSnapshots.isEmpty()) {
                            // Se a lista vier vazia, avisa no Log ou num Toast (opcional)
                            // Toast.makeText(getContext(), "Nenhum post encontrado.", Toast.LENGTH_SHORT).show();
                        }
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Post post = doc.toObject(Post.class);
                            post.setPostId(doc.getId());
                            postList.add(post);
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // AQUI É QUE VAMOS VER O ERRO
                        Toast.makeText(getContext(), "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}