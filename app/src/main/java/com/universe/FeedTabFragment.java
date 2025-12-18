package com.universe;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration postListener;
    private ListenerRegistration blockListener; // Listener para bloqueios

    private String tabType = "global";
    private String myDomain = "";
    private String myUid = "";
    private Set<String> blockedIds = new HashSet<>(); // Conjunto de IDs bloqueados

    public FeedTabFragment() { }

    public static FeedTabFragment newInstance(String type) {
        FeedTabFragment fragment = new FeedTabFragment();
        Bundle args = new Bundle();
        args.putString("TYPE", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tabType = getArguments().getString("TYPE");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_feed_tab_fragment, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            myUid = mAuth.getCurrentUser().getUid();
            String email = mAuth.getCurrentUser().getEmail();
            if (email != null && email.contains("@")) {
                myDomain = email.substring(email.indexOf("@") + 1);
            }
        }

        recyclerView = view.findViewById(R.id.recyclerViewPosts);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        swipeRefreshLayout.setOnRefreshListener(this::carregarPosts);

        // 1. Primeiro escutamos os bloqueios. Quando eles mudarem, os posts recarregam sozinhos.
        ouvirBloqueios();

        return view;
    }

    private void ouvirBloqueios() {
        if (myUid.isEmpty()) return;

        // Escuta a sub-coleção de bloqueados em tempo real
        blockListener = db.collection("users").document(myUid)
                .collection("blocked")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    blockedIds.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            blockedIds.add(doc.getId());
                        }
                    }
                    // Sempre que a lista de bloqueados mudar, recarregamos o feed
                    carregarPosts();
                });
    }

    private void carregarPosts() {
        if (postListener != null) postListener.remove();
        swipeRefreshLayout.setRefreshing(true);

        Query query;
        if (tabType.equals("global")) {
            query = db.collection("posts")
                    .whereEqualTo("postType", "global")
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            // Filtra pelo domínio da universidade para a aba "Minha Uni"
            query = db.collection("posts")
                    .whereEqualTo("universityDomain", myDomain)
                    .whereEqualTo("postType", "uni")
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        postListener = query.addSnapshotListener((value, error) -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

            if (error != null) return;

            if (value != null) {
                postList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Post post = doc.toObject(Post.class);
                    if (post != null) {
                        // FILTRO DE SEGURANÇA: Bloqueados nunca aparecem em nenhuma aba
                        if (!blockedIds.contains(post.getUserId())) {
                            post.setPostId(doc.getId());
                            postList.add(post);
                        }
                    }
                }
                postAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postListener != null) postListener.remove();
        if (blockListener != null) blockListener.remove();
    }
}