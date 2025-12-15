package com.universe;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommentsActivity extends AppCompatActivity {

    private EditText inputComment;
    private ImageButton btnSend, btnBack, btnAttach, btnRemoveImage;
    private RecyclerView recyclerView;

    // Área de Preview da Imagem
    private RelativeLayout commentImagePreviewContainer;
    private ImageView commentImagePreview;

    private CommentsAdapter adapter;
    private List<Comment> commentList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage; // <--- NOVO (Para guardar as imagens)

    private String postId;
    private Uri selectedImageUri = null; // Guarda a imagem escolhida temporariamente
    private ActivityResultLauncher<String> mGetContent; // Para abrir a galeria

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // 1. Receber ID do Post
        postId = getIntent().getStringExtra("postId");
        if (postId == null) {
            Toast.makeText(this, "Erro ao carregar post", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Configurar Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // 3. Ligar UI (Incluindo os novos elementos do XML)
        inputComment = findViewById(R.id.inputComment);
        btnSend = findViewById(R.id.btnSendComment);
        btnBack = findViewById(R.id.btnBackComments);
        btnAttach = findViewById(R.id.btnAttach); // Botão Clipe
        recyclerView = findViewById(R.id.recyclerComments);

        // UI de Preview
        commentImagePreviewContainer = findViewById(R.id.commentImagePreviewContainer);
        commentImagePreview = findViewById(R.id.commentImagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveCommentImage);

        // 4. Configurar Lista
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        adapter = new CommentsAdapter(commentList);
        recyclerView.setAdapter(adapter);

        // 5. Configurar a Galeria (ActivityResultLauncher)
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            selectedImageUri = uri;
                            commentImagePreview.setImageURI(uri);
                            commentImagePreviewContainer.setVisibility(View.VISIBLE); // Mostra o preview
                        }
                    }
                });

        // 6. Configurar Cliques
        btnBack.setOnClickListener(v -> finish());

        // Clique no Clipe -> Abre Galeria
        btnAttach.setOnClickListener(v -> mGetContent.launch("image/*"));

        // Clique no X -> Remove a imagem selecionada
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            commentImagePreview.setImageURI(null);
            commentImagePreviewContainer.setVisibility(View.GONE);
        });

        // Clique em Enviar
        btnSend.setOnClickListener(v -> prepararEnvio());

        // 7. Carregar Comentários
        carregarComentarios();
    }

    private void carregarComentarios() {
        db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        commentList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            commentList.add(doc.toObject(Comment.class));
                        }
                        adapter.notifyDataSetChanged();
                        if (commentList.size() > 0) {
                            recyclerView.scrollToPosition(commentList.size() - 1);
                        }
                    }
                });
    }

    // --- NOVA LÓGICA DE ENVIO ---

    private void prepararEnvio() {
        String texto = inputComment.getText().toString().trim();

        // Se não houver texto E não houver imagem, não faz nada
        if (TextUtils.isEmpty(texto) && selectedImageUri == null) {
            return;
        }

        // Limpar a UI imediatamente para parecer rápido
        inputComment.setText("");
        commentImagePreviewContainer.setVisibility(View.GONE);

        Uri imageToSend = selectedImageUri; // Guarda numa variável local
        selectedImageUri = null; // Limpa a global

        if (imageToSend != null) {
            // Tem imagem? Upload primeiro.
            uploadImageAndSend(texto, imageToSend);
        } else {
            // Só texto? Envia direto.
            enviarComentarioFinal(texto, null);
        }
    }

    private void uploadImageAndSend(String texto, Uri imageUri) {
        String uid = mAuth.getCurrentUser().getUid();
        // Nome único: comment_images/USER_ID/UUID.jpg
        String fileName = "comment_images/" + uid + "/" + UUID.randomUUID().toString() + ".jpg";

        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Upload feito, obter o link
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        enviarComentarioFinal(texto, uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CommentsActivity.this, "Erro ao enviar imagem", Toast.LENGTH_SHORT).show();
                });
    }

    private void enviarComentarioFinal(String texto, String imageUrl) {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            long timestamp = System.currentTimeMillis();

                            // Cria o comentário usando o NOVO construtor (5 parâmetros)
                            Comment comment = new Comment(
                                    uid,
                                    user.getNome(),
                                    texto,
                                    timestamp,
                                    imageUrl // Passamos o URL da imagem (ou null)
                            );

                            db.collection("posts").document(postId).collection("comments")
                                    .add(comment);
                        }
                    }
                });
    }
}