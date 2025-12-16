package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
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
    private ImageButton btnNotifications;
    private TabLayout tabLayout;

    // Badge de Notificação
    private View notificationBadge;
    private ListenerRegistration badgeListener;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration postListener;

    private String myDomain = ""; // Guarda o domínio da universidade (ex: ips.pt)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 1. Ligar componentes
        recyclerView = view.findViewById(R.id.recyclerViewPosts);
        fabCreatePost = view.findViewById(R.id.fabCreatePost);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        tabLayout = view.findViewById(R.id.tabLayoutFeed);

        // Se já implementaste a bolinha vermelha:
        notificationBadge = view.findViewById(R.id.notificationBadge);

        // 2. Configurar Lista
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // 3. Descobrir o meu Domínio
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            if (email.contains("@")) {
                myDomain = email.substring(email.indexOf("@") + 1);
            }
        }

        // 4. Configurar as Abas
        tabLayout.addTab(tabLayout.newTab().setText("Global"));
        tabLayout.addTab(tabLayout.newTab().setText("Minha Uni"));

        // Listener para quando mudamos de aba
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    carregarPosts("global");
                } else {
                    carregarPosts("uni");
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                recyclerView.smoothScrollToPosition(0);
                if (tab.getPosition() == 0) carregarPosts("global");
                else carregarPosts("uni");
            }
        });

        // 5. Botões
        fabCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            // Enviar a aba selecionada para o CreatePost saber se é Global ou Uni
            int currentTab = tabLayout.getSelectedTabPosition();
            intent.putExtra("selectedTab", currentTab);
            startActivity(intent);
        });

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(intent);
        });

        // 6. Swipe to Refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (tabLayout.getSelectedTabPosition() == 0) {
                carregarPosts("global");
            } else {
                carregarPosts("uni");
            }
        });

        // 7. Badge de Notificações (Se implementaste a bolinha)
        verificarNotificacoesNaoLidas();

        // Carregar Global ao iniciar
        carregarPosts("global");

        return view;
    }

    private void carregarPosts(String tipo) {
        if (postListener != null) {
            postListener.remove();
        }

        swipeRefreshLayout.setRefreshing(true);

        Query query;

        if (tipo.equals("global")) {
            // GLOBAL: Apenas mostra posts com postType="global"
            // (Requer índice: postType + timestamp)
            query = db.collection("posts")
                    .whereEqualTo("postType", "global")
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            // MINHA UNI: Apenas mostra posts com postType="uni" E do meu domínio
            // (Requer índice: universityDomain + postType + timestamp)
            query = db.collection("posts")
                    .whereEqualTo("universityDomain", myDomain)
                    .whereEqualTo("postType", "uni") // <--- SEPARAÇÃO TOTAL AQUI
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        postListener = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (error != null) {
                    if (getContext() != null) {
                        // Se aparecer este erro, clica no link azul no Logcat!
                        Toast.makeText(getContext(), "Índice necessário: verifica o Logcat", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

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
                }
            }
        });
    }

    // Método da Bolinha Vermelha (Caso tenhas a View notificationBadge no XML)
    private void verificarNotificacoesNaoLidas() {
        if (mAuth.getCurrentUser() == null || notificationBadge == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        badgeListener = db.collection("notifications")
                .whereEqualTo("targetUserId", myId)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && !value.isEmpty()) {
                        notificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postListener != null) postListener.remove();
        if (badgeListener != null) badgeListener.remove();
    }
}