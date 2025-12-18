package com.universe;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private Set<String> userIdsAdicionados;
    private Set<String> bloqueadosIds; // Nova lista para IDs bloqueados
    private EditText searchBar;
    private FirebaseFirestore db;
    private String currentUserId;

    private ProgressBar progressBar;
    private LinearLayout emptyViewSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        recyclerView = view.findViewById(R.id.recycler_view_search);
        searchBar = view.findViewById(R.id.search_bar);
        progressBar = view.findViewById(R.id.searchProgressBar);
        emptyViewSearch = view.findViewById(R.id.emptyViewSearch);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        userList = new ArrayList<>();
        userIdsAdicionados = new HashSet<>();
        bloqueadosIds = new HashSet<>();

        userAdapter = new UserAdapter(userList, false);
        recyclerView.setAdapter(userAdapter);

        // 1. Carregar lista de bloqueados antes de permitir a pesquisa
        carregarListaBloqueados();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String texto = s.toString().trim();
                if (texto.isEmpty()) {
                    userList.clear();
                    userIdsAdicionados.clear();
                    userAdapter.notifyDataSetChanged();
                    mostrarEstado(false, false);
                } else {
                    pesquisarUtilizadores(texto);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        return view;
    }

    // Carrega os IDs dos utilizadores que eu bloqueei
    private void carregarListaBloqueados() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId)
                .collection("blocked")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        bloqueadosIds.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            bloqueadosIds.add(doc.getId());
                        }
                    }
                });
    }

    private void pesquisarUtilizadores(String texto) {
        mostrarEstado(true, false);
        userList.clear();
        userIdsAdicionados.clear();

        if (texto.contains("@")) {
            db.collection("users")
                    .orderBy("email")
                    .startAt(texto)
                    .endAt(texto + "\uf8ff")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        processarResultados(queryDocumentSnapshots);
                        verificarSeVazio();
                    });
        } else {
            db.collection("users")
                    .orderBy("nome")
                    .startAt(texto)
                    .endAt(texto + "\uf8ff")
                    .get()
                    .addOnSuccessListener(snapshotsNome -> {
                        processarResultados(snapshotsNome);

                        String usernameBusca = texto.toLowerCase();
                        db.collection("users")
                                .orderBy("username")
                                .startAt(usernameBusca)
                                .endAt(usernameBusca + "\uf8ff")
                                .get()
                                .addOnSuccessListener(snapshotsUser -> {
                                    processarResultados(snapshotsUser);
                                    verificarSeVazio();
                                });
                    });
        }
    }

    private void processarResultados(QuerySnapshot snapshots) {
        for (DocumentSnapshot doc : snapshots) {
            String uid = doc.getId();

            // FILTRO DE BLOQUEIO: Se o utilizador estiver bloqueado, ignoramos
            if (bloqueadosIds.contains(uid)) continue;

            // Não mostrar o meu próprio perfil na pesquisa
            if (uid.equals(currentUserId)) continue;

            User user = doc.toObject(User.class);
            if (user != null) {
                user.setUid(uid);
                adicionarSemDuplicados(user);
            }
        }
        userAdapter.notifyDataSetChanged();
    }

    private void adicionarSemDuplicados(User user) {
        if (user != null && user.getUid() != null) {
            if (!userIdsAdicionados.contains(user.getUid())) {
                userList.add(user);
                userIdsAdicionados.add(user.getUid());
            }
        }
    }

    private void mostrarEstado(boolean isLoading, boolean isEmpty) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            if (emptyViewSearch != null) emptyViewSearch.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        } else {
            if (isEmpty) {
                if (emptyViewSearch != null) emptyViewSearch.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                if (emptyViewSearch != null) emptyViewSearch.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void verificarSeVazio() {
        mostrarEstado(false, userList.isEmpty());
    }
}