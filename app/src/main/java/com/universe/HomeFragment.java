package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Importante
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration; // Importante
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FloatingActionButton fabCreatePost;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton btnNotifications; // <--- Variável global

    private FirebaseFirestore db;
    private ListenerRegistration postListener; // <--- Para controlar a ligação ao Firebase

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();

        // 1. Ligar componentes
        recyclerView = view.findViewById(R.id.recyclerViewPosts);
        fabCreatePost = view.findViewById(R.id.fabCreatePost);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        btnNotifications = view.findViewById(R.id.btnNotifications); // <--- Ligar ID

        // 2. Configurar Lista
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // 3. Ação do Botão Criar Post
        fabCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(intent);
        });

        // 4. Ação do Botão Notificações
        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(intent);
        });

        // 5. Lógica do Swipe to Refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                carregarPosts();
            }
        });

        // 6. Carregar Posts ao iniciar
        carregarPosts();

        return view;
    }

    private void carregarPosts() {
        // Se já existir um listener ativo, removemos antes de criar um novo
        // Isto evita que a app fique lenta ou leia dados duplicados
        if (postListener != null) {
            postListener.remove();
        }

        Query query = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        postListener = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                // Parar a animação de refresh se estiver ativa
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (error != null) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Erro ao atualizar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                if (value != null) {
                    postList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(doc.getId()); // Fundamental para os likes funcionarem
                            postList.add(post);
                        }
                    }
                    postAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    // Boa prática: Parar de escutar o Firebase quando o utilizador sai deste ecrã
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postListener != null) {
            postListener.remove();
        }
    }
}