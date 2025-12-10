package com.universe;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton; // Importante
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    private EditText inputComment;
    private ImageButton btnSend;
    private ImageButton btnBack; // <--- NOVA VARIÁVEL
    private RecyclerView recyclerView;

    private CommentsAdapter adapter;
    private List<Comment> commentList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // 1. Receber o ID do Post
        postId = getIntent().getStringExtra("postId");

        if (postId == null) {
            Toast.makeText(this, "Erro ao carregar post", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Configurar Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 3. Ligar componentes da UI
        inputComment = findViewById(R.id.inputComment);
        btnSend = findViewById(R.id.btnSendComment);
        recyclerView = findViewById(R.id.recyclerComments);
        btnBack = findViewById(R.id.btnBackComments); // <--- LIGAR AO NOVO ID DO XML

        // 4. Ação do Botão Voltar (NOVO)
        btnBack.setOnClickListener(v -> finish());

        // 5. Configurar Lista
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        // Nota: Certifica-te que tens a classe CommentsAdapter criada
        adapter = new CommentsAdapter(commentList);
        recyclerView.setAdapter(adapter);

        // 6. Carregar comentários
        carregarComentarios();

        // 7. Botão de Enviar
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarComentario();
            }
        });
    }

    private void carregarComentarios() {
        db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) return;

                        if (value != null) {
                            commentList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                // Converte o documento para a classe Comment
                                commentList.add(doc.toObject(Comment.class));
                            }
                            adapter.notifyDataSetChanged();

                            // Rolar para o último comentário
                            if (commentList.size() > 0) {
                                recyclerView.scrollToPosition(commentList.size() - 1);
                            }
                        }
                    }
                });
    }

    private void enviarComentario() {
        String texto = inputComment.getText().toString().trim();
        if (TextUtils.isEmpty(texto)) return;

        inputComment.setText("");

        String uid = mAuth.getCurrentUser().getUid(); // Este é o userId que faltava!

        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);

                            if (user != null) {
                                long timestamp = System.currentTimeMillis();

                                // AGORA SIM: Passamos o 'uid' (userId) como primeiro dado
                                Comment comment = new Comment(uid, user.getNome(), texto, timestamp);

                                db.collection("posts").document(postId).collection("comments")
                                        .add(comment);
                            }
                        }
                    }
                });
    }
}