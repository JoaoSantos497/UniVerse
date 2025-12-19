package com.universe;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
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
    private LinearLayoutManager layoutManager;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration postListener;
    private ListenerRegistration blockListener;

    private String tabType = "global";
    private String myDomain = "";
    private String myUid = "";
    private Set<String> blockedIds = new HashSet<>();

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
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            myUid = mAuth.getCurrentUser().getUid();
            String email = mAuth.getCurrentUser().getEmail();
            if (email != null && email.contains("@")) {
                myDomain = email.substring(email.indexOf("@") + 1).toLowerCase();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_feed_tab_fragment, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewPosts);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // CORREÇÃO: O refresh agora limpa a lista para garantir uma recarga limpa
        swipeRefreshLayout.setOnRefreshListener(() -> {
            postList.clear();
            postAdapter.notifyDataSetChanged();
            iniciarEscutaRealtime();
        });

        iniciarEscutaRealtime();

        return view;
    }

    private void iniciarEscutaRealtime() {
        if (myUid.isEmpty()) return;

        if (blockListener != null) blockListener.remove();

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
                    ouvirPosts();
                });
    }

    private void ouvirPosts() {
        if (postListener != null) postListener.remove();

        Query query;
        if (tabType.equals("global")) {
            query = db.collection("posts")
                    .whereEqualTo("postType", "global")
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            query = db.collection("posts")
                    .whereEqualTo("universityDomain", myDomain)
                    .whereEqualTo("postType", "uni")
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        postListener = query.addSnapshotListener((value, error) -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (error != null) {
                Log.e("Firestore", "Erro: " + error.getMessage());
                return;
            }

            if (value != null) {
                if (postList.isEmpty()) {
                    // Carga inicial
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post != null && !blockedIds.contains(post.getUserId())) {
                            post.setPostId(doc.getId());
                            postList.add(post);
                        }
                    }
                    postAdapter.notifyDataSetChanged();
                } else {
                    // Atualizações em tempo real
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Post post = dc.getDocument().toObject(Post.class);
                        if (post == null) continue;
                        post.setPostId(dc.getDocument().getId());

                        if (blockedIds.contains(post.getUserId())) continue;

                        int oldIndex = dc.getOldIndex();
                        int newIndex = dc.getNewIndex();

                        switch (dc.getType()) {
                            case ADDED:
                                if (!contemPost(post.getPostId())) {
                                    postList.add(newIndex, post);
                                    postAdapter.notifyItemInserted(newIndex);

                                    // AUTO-SCROLL: Se o post é novo e o user está no topo, puxa para cima
                                    if (newIndex == 0 && layoutManager.findFirstVisibleItemPosition() <= 1) {
                                        recyclerView.scrollToPosition(0);
                                    }
                                }
                                break;

                            case MODIFIED:
                                if (oldIndex == newIndex && oldIndex != -1) {
                                    postList.set(newIndex, post);
                                    postAdapter.notifyItemChanged(newIndex);
                                }
                                break;

                            case REMOVED:
                                if (oldIndex != -1 && oldIndex < postList.size()) {
                                    // 1. Removemos o objeto da nossa lista local
                                    postList.remove(oldIndex);

                                    // 2. Notificamos a remoção para a animação suave
                                    postAdapter.notifyItemRemoved(oldIndex);

                                    // 3. ESSENCIAL: Notificamos que os itens abaixo mudaram de posição
                                    // Isso evita que o RecyclerView tente aceder a índices errados (o bug)
                                    postAdapter.notifyItemRangeChanged(oldIndex, postList.size());
                                }
                                break;
                        }
                    }
                }
            }
        });
    }

    private boolean contemPost(String id) {
        for (Post p : postList) {
            if (p.getPostId() != null && p.getPostId().equals(id)) return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postListener != null) postListener.remove();
        if (blockListener != null) blockListener.remove();
    }
}