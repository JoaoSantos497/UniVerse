package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FloatingActionButton fabCreatePost;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Configurar RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewPosts);
        recyclerView.setHasFixedSize(true);
        // O setItemAnimator(null) ajuda a evitar "piscar" quando dás like
        recyclerView.setItemAnimator(null);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicializar Lista Vazia
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // Configurar Botão Flutuante (+)
        fabCreatePost = view.findViewById(R.id.fabCreatePost);
        fabCreatePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CreatePostActivity.class);
                startActivity(intent);
            }
        });

        // --- MUDANÇA: Chamamos a função aqui em vez de no onResume ---
        carregarPostsEmTempoReal();

        return view;
    }

    // Já não precisamos do onResume, o Listener faz o trabalho sozinho!

    private void carregarPostsEmTempoReal() {
        db.collection("posts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                // --- AQUI ESTÁ A MAGIA: addSnapshotListener ---
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        // 1. Verificar erros
                        if (error != null) {
                            Toast.makeText(getContext(), "Erro de sincronização: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 2. Se houver dados, atualizamos a lista
                        if (value != null) {
                            postList.clear(); // Limpa para não duplicar
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                Post post = doc.toObject(Post.class);

                                // Não esquecer de definir o ID para o Like funcionar
                                post.setPostId(doc.getId());

                                postList.add(post);
                            }
                            // Atualiza o ecrã instantaneamente
                            postAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }
}