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
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FeedTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration postListener;

    private String tabType = "global"; // "global" ou "uni"
    private String myDomain = "";

    // Construtor vazio obrigatório
    public FeedTabFragment() { }

    // Metodo para criar nova instância com argumentos
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

        recyclerView = view.findViewById(R.id.recyclerViewPosts);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Configurar Lista
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // Descobrir domínio
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            if (email.contains("@")) {
                myDomain = email.substring(email.indexOf("@") + 1);
            }
        }

        // Swipe to Refresh
        swipeRefreshLayout.setOnRefreshListener(this::carregarPosts);

        carregarPosts();

        return view;
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
            query = db.collection("posts")
                    .whereEqualTo("universityDomain", myDomain)
                    .whereEqualTo("postType", "uni")
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        postListener = query.addSnapshotListener((value, error) -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

            if (error != null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Erro/Índice: verifica o Logcat", Toast.LENGTH_SHORT).show();
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
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postListener != null) postListener.remove();
    }
}