package com.universe;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
    private RecyclerView recyclerView;

    private CommentsAdapter adapter;
    private List<Comment> commentList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String postId; // O ID do post que estamos a comentar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // 1. Receber o ID do Post (Vem do clique no Feed)
        postId = getIntent().getStringExtra("postId");

        if (postId == null) {
            Toast.makeText(this, "Erro ao carregar post", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Configurar Firebase e UI
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        inputComment = findViewById(R.id.inputComment);
        btnSend = findViewById(R.id.btnSendComment);
        recyclerView = findViewById(R.id.recyclerComments);

        // 3. Configurar Lista
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        adapter = new CommentsAdapter(commentList);
        recyclerView.setAdapter(adapter);

        // 4. Carregar comentários existentes
        carregarComentarios();

        // 5. Botão de Enviar
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarComentario();
            }
        });
    }

    private void carregarComentarios() {
        // Entramos na pasta do Post -> Sub-pasta 'comments'
        db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp") // Mais antigos em cima (tipo conversa)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) return;

                        if (value != null) {
                            commentList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                commentList.add(doc.toObject(Comment.class));
                            }
                            adapter.notifyDataSetChanged();
                            // Rolar para o fundo quando chega um novo
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

        inputComment.setText(""); // Limpa a caixa logo para ser rápido

        String uid = mAuth.getCurrentUser().getUid();

        // Precisamos do nome do utilizador antes de gravar
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);

                            // Criar objeto Comentário
                            long timestamp = System.currentTimeMillis();
                            Comment comment = new Comment(user.getNome(), texto, timestamp);

                            // Guardar na sub-coleção "comments" dentro do Post
                            db.collection("posts").document(postId).collection("comments")
                                    .add(comment);
                        }
                    }
                });
    }
}