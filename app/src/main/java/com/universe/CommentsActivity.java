package com.universe;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommentsActivity extends AppCompatActivity {

    // UI - Input e Lista
    private EditText inputComment;
    private ImageButton btnSend, btnBack, btnAttach, btnRemoveImage;
    private RecyclerView recyclerView;

    // UI - Header (Dono do Post)
    private ImageView headerProfileImage;
    private TextView headerName, headerCourse, headerFollowers, headerFollowing;
    private Button btnHeaderFollow;

    // UI - Preview de Imagem
    private RelativeLayout commentImagePreviewContainer;
    private ImageView commentImagePreview;

    // UI - Responder (Reply)
    private RelativeLayout replyContainer;
    private TextView txtReplyingTo;
    private ImageButton btnCloseReply;

    // Dados e Firebase
    private CommentsAdapter adapter;
    private List<Comment> commentList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    private String postId;
    private String postAuthorId;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> mGetContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        postId = getIntent().getStringExtra("postId");
        if (postId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // 1. Ligar Componentes UI
        ligarComponentes();

        // 2. Configurar Lista com Listener de Resposta
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();

        // Passamos o listener: Quando clicam em "Responder" no adapter, executa o código aqui
        adapter = new CommentsAdapter(commentList, username -> ativarModoResposta(username));
        recyclerView.setAdapter(adapter);

        // 3. Configurar Galeria
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                commentImagePreview.setImageURI(uri);
                commentImagePreviewContainer.setVisibility(View.VISIBLE);
            }
        });

        // 4. Configurar Cliques Básicos
        btnBack.setOnClickListener(v -> finish());
        btnAttach.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnRemoveImage.setOnClickListener(v -> limparImagemSelecionada());

        // Fechar modo de resposta
        btnCloseReply.setOnClickListener(v -> {
            replyContainer.setVisibility(View.GONE);
            inputComment.setText("");
        });

        // 5. Configurar Envio (Botão + Teclado)
        btnSend.setOnClickListener(v -> prepararEnvio());

        inputComment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                prepararEnvio();
                return true;
            }
            return false;
        });

        // 6. Carregar Dados
        carregarDadosDoPost(); // Preenche o cabeçalho
        carregarComentarios(); // Preenche a lista
    }

    private void ligarComponentes() {
        // Input
        inputComment = findViewById(R.id.inputComment);
        btnSend = findViewById(R.id.btnSendComment);
        btnBack = findViewById(R.id.btnBackComments);
        btnAttach = findViewById(R.id.btnAttach);
        recyclerView = findViewById(R.id.recyclerComments);

        // Header
        headerProfileImage = findViewById(R.id.publicProfileImage);
        headerName = findViewById(R.id.publicProfileName);
        headerCourse = findViewById(R.id.publicProfileCourse);
        headerFollowers = findViewById(R.id.publicFollowersCount);
        headerFollowing = findViewById(R.id.publicFollowingCount);
        btnHeaderFollow = findViewById(R.id.btnFollowProfile);

        // Preview Imagem
        commentImagePreviewContainer = findViewById(R.id.commentImagePreviewContainer);
        commentImagePreview = findViewById(R.id.commentImagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveCommentImage);

        // Reply
        replyContainer = findViewById(R.id.replyContainer);
        txtReplyingTo = findViewById(R.id.txtReplyingTo);
        btnCloseReply = findViewById(R.id.btnCloseReply);
    }

    // --- LÓGICA DE DADOS ---

    private void carregarDadosDoPost() {
        // Primeiro buscamos o Post para saber quem é o autor
        db.collection("posts").document(postId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Post post = doc.toObject(Post.class);
                if (post != null) {
                    postAuthorId = post.getUserId();
                    carregarHeaderAutor(postAuthorId);
                }
            }
        });
    }

    private void carregarHeaderAutor(String userId) {
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    headerName.setText(user.getNome());
                    headerCourse.setText(user.getCurso());

                    if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                        Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(headerProfileImage);
                    }

                }
            }
        });

        // Esconder botão seguir se for eu mesmo
        if (userId.equals(mAuth.getCurrentUser().getUid())) {
            btnHeaderFollow.setVisibility(View.GONE);
        }
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

    // --- LÓGICA DE INTERAÇÃO ---

    private void ativarModoResposta(String username) {
        replyContainer.setVisibility(View.VISIBLE);
        txtReplyingTo.setText("A responder a @" + username);

        String mention = "@" + username + " ";
        inputComment.setText(mention);
        inputComment.setSelection(inputComment.getText().length());

        // Abrir teclado
        inputComment.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(inputComment, InputMethodManager.SHOW_IMPLICIT);
    }

    private void limparImagemSelecionada() {
        selectedImageUri = null;
        commentImagePreview.setImageURI(null);
        commentImagePreviewContainer.setVisibility(View.GONE);
    }

    private void prepararEnvio() {
        String texto = inputComment.getText().toString().trim();

        if (TextUtils.isEmpty(texto) && selectedImageUri == null) return;

        // Reset UI
        inputComment.setText("");
        limparImagemSelecionada();
        replyContainer.setVisibility(View.GONE); // Fecha o modo resposta

        // Fechar teclado
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(inputComment.getWindowToken(), 0);

        if (selectedImageUri != null) {
            uploadImageAndSend(texto, selectedImageUri); // A imagem na variavel local seria melhor, mas aqui simplifico
        } else {
            enviarComentarioFinal(texto, null);
        }
    }

    private void uploadImageAndSend(String texto, Uri imageUri) {
        String uid = mAuth.getCurrentUser().getUid();
        String fileName = "comment_images/" + uid + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                ref.getDownloadUrl().addOnSuccessListener(uri ->
                        enviarComentarioFinal(texto, uri.toString())
                )
        );
    }

    private void enviarComentarioFinal(String texto, String imageUrl) {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    Comment comment = new Comment(
                            uid,
                            user.getNome(),
                            texto,
                            System.currentTimeMillis(),
                            imageUrl
                    );

                    db.collection("posts").document(postId).collection("comments").add(comment);

                    // --- ENVIAR NOTIFICAÇÃO ---
                    enviarNotificacaoComment(postAuthorId);
                }
            }
        });
    }

    // --- NOTIFICAÇÃO ---
    private void enviarNotificacaoComment(String donoDoPostId) {
        if (donoDoPostId == null || donoDoPostId.equals(mAuth.getCurrentUser().getUid())) return;

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(doc -> {
            User eu = doc.toObject(User.class);
            if (eu != null) {
                Map<String, Object> notifMap = new HashMap<>();
                notifMap.put("targetUserId", donoDoPostId);
                notifMap.put("fromUserId", eu.getUid() != null ? eu.getUid() : mAuth.getCurrentUser().getUid());
                notifMap.put("fromUserName", eu.getNome());
                notifMap.put("fromUserPhoto", eu.getPhotoUrl());
                notifMap.put("type", "comment");
                notifMap.put("message", "comentou a tua publicação");
                notifMap.put("postId", postId);
                notifMap.put("timestamp", System.currentTimeMillis());

                db.collection("notifications").add(notifMap);
            }
        });
    }
}