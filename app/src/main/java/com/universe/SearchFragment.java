package com.universe;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
    private Set<String> userIdsAdicionados; // Para evitar duplicados
    private EditText searchBar;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recycler_view_search);
        searchBar = view.findViewById(R.id.search_bar);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        userList = new ArrayList<>();
        userIdsAdicionados = new HashSet<>(); // Inicializar o conjunto de IDs
        userAdapter = new UserAdapter(userList);
        recyclerView.setAdapter(userAdapter);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                pesquisarUtilizadores(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        return view;
    }

    private void pesquisarUtilizadores(String texto) {
        // 1. Limpar resultados anteriores
        userList.clear();
        userIdsAdicionados.clear();
        userAdapter.notifyDataSetChanged();

        if (texto.isEmpty()) {
            return;
        }

        // CASO 1: Pesquisa por EMAIL (se tiver @ no meio)
        if (texto.contains("@")) {
            db.collection("users")
                    .orderBy("email")
                    .startAt(texto)
                    .endAt(texto + "\uf8ff")
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                adicionarSemDuplicados(doc.toObject(User.class));
                            }
                            userAdapter.notifyDataSetChanged();
                        }
                    });
        }
        // CASO 2: Pesquisa HÍBRIDA (Nome OU Username)
        else {
            // A. Pesquisar por NOME (tal como escreveste, ex: "Joao")
            db.collection("users")
                    .orderBy("nome")
                    .startAt(texto)
                    .endAt(texto + "\uf8ff")
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                adicionarSemDuplicados(doc.toObject(User.class));
                            }
                            // Atualizamos a lista já com os nomes encontrados
                            userAdapter.notifyDataSetChanged();

                            // B. E agora pesquisamos também por USERNAME (em minúsculas)
                            // Fazemos isto DENTRO do sucesso do primeiro para não atropelar
                            String usernameBusca = texto.toLowerCase(); // Usernames são sempre minúsculos

                            db.collection("users")
                                    .orderBy("username")
                                    .startAt(usernameBusca)
                                    .endAt(usernameBusca + "\uf8ff")
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot querySnapshotUsername) {
                                            for (DocumentSnapshot doc : querySnapshotUsername) {
                                                adicionarSemDuplicados(doc.toObject(User.class));
                                            }
                                            // Atualizamos a lista de novo com os usernames extra
                                            userAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    });
        }
    }

    // Função auxiliar para garantir que a mesma pessoa não aparece 2 vezes
    private void adicionarSemDuplicados(User user) {
        if (user != null && user.getUid() != null) {
            // Se o ID ainda não estiver na lista de "Já mostrados"
            if (!userIdsAdicionados.contains(user.getUid())) {
                userList.add(user);
                userIdsAdicionados.add(user.getUid()); // Marca como mostrado
            }
        }
    }
}