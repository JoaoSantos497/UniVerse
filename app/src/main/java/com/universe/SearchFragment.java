package com.universe;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout; // Importante
import android.widget.ProgressBar; // Importante

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
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
    private EditText searchBar;
    private FirebaseFirestore db;

    // --- NOVAS VARIÁVEIS DE UI ---
    private ProgressBar progressBar;
    private LinearLayout emptyViewSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Certifica-te que o layout tem a ProgressBar e o LinearLayout "emptyViewSearch"
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recycler_view_search);
        searchBar = view.findViewById(R.id.search_bar);

        // Ligar os novos componentes
        progressBar = view.findViewById(R.id.searchProgressBar);
        emptyViewSearch = view.findViewById(R.id.emptyViewSearch);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        userList = new ArrayList<>();
        userIdsAdicionados = new HashSet<>();
        userAdapter = new UserAdapter(userList, false);
        recyclerView.setAdapter(userAdapter);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String texto = s.toString().trim();
                if (texto.isEmpty()) {
                    // Se apagou tudo, limpa a lista e esconde os avisos
                    userList.clear();
                    userIdsAdicionados.clear();
                    userAdapter.notifyDataSetChanged();
                    mostrarEstado(false, false); // Esconde tudo
                } else {
                    pesquisarUtilizadores(texto);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        return view;
    }

    private void pesquisarUtilizadores(String texto) {
        // Mostra o loading e limpa a lista atual
        mostrarEstado(true, false);

        userList.clear();
        userIdsAdicionados.clear();
        userAdapter.notifyDataSetChanged();

        // Lógica de pesquisa igual à anterior, mas com gestão do loading no final

        if (texto.contains("@")) {
            db.collection("users")
                    .orderBy("email")
                    .startAt(texto)
                    .endAt(texto + "\uf8ff")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        processarResultados(queryDocumentSnapshots);
                        // Como só faz uma query, podemos parar o loading aqui
                        verificarSeVazio();
                    });
        } else {
            // Pesquisa dupla (Nome e Username)
            // Precisamos de saber quando AMBAS terminaram para parar o loading.
            // Solução simples: processamos o nome, e dentro do sucesso do nome, lançamos o username.

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

                                    // Agora que as duas acabaram, verificamos
                                    verificarSeVazio();
                                });
                    });
        }
    }

    // Método para processar os documentos e adicionar à lista
    private void processarResultados(QuerySnapshot snapshots) {
        for (DocumentSnapshot doc : snapshots) {
            User user = doc.toObject(User.class);
            if (user != null) {
                user.setUid(doc.getId());
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

    // Controla o que é visível: Loading, Lista ou Mensagem de Vazio
    private void mostrarEstado(boolean isLoading, boolean isEmpty) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        if (isLoading) {
            // Se está a carregar, esconde o resto
            if (emptyViewSearch != null) emptyViewSearch.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // Se parou de carregar...
            if (isEmpty) {
                // ... e está vazio -> Mostra aviso
                if (emptyViewSearch != null) emptyViewSearch.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                // ... e tem dados -> Mostra lista
                if (emptyViewSearch != null) emptyViewSearch.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void verificarSeVazio() {
        boolean estaVazia = userList.isEmpty();
        mostrarEstado(false, estaVazia); // Pára o loading e define se mostra lista ou aviso
    }
}