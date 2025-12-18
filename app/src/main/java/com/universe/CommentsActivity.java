package com.universe;

import android.content.Context;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView headerName, headerCourse;
    private Button btnHeaderFollow;

    // UI - Preview de Imagem
    private RelativeLayout commentImagePreviewContainer;
    private ImageView commentImagePreview;

    // UI - Responder / Editar (Overlay)
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

    // Estado de Edição
    private String idComentarioEdicao = null;

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

        // 1. Inicializar UI
        ligarComponentes();

        // 2. Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        adapter = new CommentsAdapter(commentList, username -> ativarModoResposta(username));
        recyclerView.setAdapter(adapter);

        // 3. Configurar Seletor de Imagem
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                commentImagePreview.setImageURI(uri);
                commentImagePreviewContainer.setVisibility(View.VISIBLE);
            }
        });

        // 4. Listeners de Cliques
        btnBack.setOnClickListener(v -> finish());
        btnAttach.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnRemoveImage.setOnClickListener(v -> limparImagemSelecionada());
        btnCloseReply.setOnClickListener(v -> cancelarEdicaoOuResposta());
        btnSend.setOnClickListener(v -> prepararEnvio());

        inputComment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                prepararEnvio();
                return true;
            }
            return false;
        });

        // 5. Carregar Dados Iniciais
        carregarDadosDoPost();
        carregarComentarios();
    }

    private void ligarComponentes() {
        inputComment = findViewById(R.id.inputComment);
        btnSend = findViewById(R.id.btnSendComment);
        btnBack = findViewById(R.id.btnBackComments);
        btnAttach = findViewById(R.id.btnAttach);
        recyclerView = findViewById(R.id.recyclerComments);

        headerProfileImage = findViewById(R.id.publicProfileImage);
        headerName = findViewById(R.id.publicProfileName);
        headerCourse = findViewById(R.id.publicProfileCourse);
        btnHeaderFollow = findViewById(R.id.btnFollowProfile);

        commentImagePreviewContainer = findViewById(R.id.commentImagePreviewContainer);
        commentImagePreview = findViewById(R.id.commentImagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveCommentImage);

        replyContainer = findViewById(R.id.replyContainer);
        txtReplyingTo = findViewById(R.id.txtReplyingTo);
        btnCloseReply = findViewById(R.id.btnCloseReply);
    }

    // --- LÓGICA DE DADOS ---

    private void carregarDadosDoPost() {
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
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null) {
                                comment.setCommentId(doc.getId()); // Essencial para editar/apagar
                                commentList.add(comment);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (commentList.size() > 0) {
                            recyclerView.scrollToPosition(commentList.size() - 1);
                        }
                    }
                });
    }

    // --- LÓGICA DE INTERAÇÃO ---

    public void prepararEdicaoComentario(Comment comment) {
        idComentarioEdicao = comment.getCommentId();
        replyContainer.setVisibility(View.VISIBLE);
        txtReplyingTo.setText("A editar o teu comentário...");
        inputComment.setText(comment.getContent());
        inputComment.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(inputComment, InputMethodManager.SHOW_IMPLICIT);

        btnSend.setImageResource(android.R.drawable.ic_menu_save);
        btnAttach.setVisibility(View.GONE);
    }

    private void ativarModoResposta(String username) {
        replyContainer.setVisibility(View.VISIBLE);
        txtReplyingTo.setText("A responder a @" + username);
        inputComment.setText("@" + username + " ");
        inputComment.setSelection(inputComment.getText().length());
        inputComment.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(inputComment, InputMethodManager.SHOW_IMPLICIT);
    }

    private void cancelarEdicaoOuResposta() {
        idComentarioEdicao = null;
        replyContainer.setVisibility(View.GONE);
        inputComment.setText("");
        btnSend.setImageResource(R.drawable.ic_send);
        btnAttach.setVisibility(View.VISIBLE);
        limparImagemSelecionada();
    }

    public void apagarComentario(String commentId, int position) {
        db.collection("posts").document(postId)
                .collection("comments").document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Notifica o adapter para remover o item localmente com animação correta
                    if (adapter != null) {
                        adapter.removerItemDaLista(position);
                    }
                });
    }

    private void limparImagemSelecionada() {
        selectedImageUri = null;
        commentImagePreview.setImageURI(null);
        commentImagePreviewContainer.setVisibility(View.GONE);
    }

    private void prepararEnvio() {
        String texto = inputComment.getText().toString().trim();
        if (TextUtils.isEmpty(texto) && selectedImageUri == null) return;

        if (idComentarioEdicao != null) {
            enviarComentarioFinal(texto, null);
        } else {
            Uri imageToUpload = selectedImageUri;
            if (imageToUpload != null) {
                uploadImageAndSend(texto, imageToUpload);
            } else {
                enviarComentarioFinal(texto, null);
            }
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

    private void enviarComentarioFinal(String texto, String commentImageUrl) {
        String uid = mAuth.getCurrentUser().getUid();

        if (idComentarioEdicao != null) {
            db.collection("posts").document(postId)
                    .collection("comments").document(idComentarioEdicao)
                    .update("content", texto)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Atualizado!", Toast.LENGTH_SHORT).show();
                        cancelarEdicaoOuResposta();
                    });
        } else {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                User user = doc.toObject(User.class);
                if (user != null) {
                    String userPhoto = user.getPhotoUrl() != null ? user.getPhotoUrl() : "";
                    Comment comment = new Comment(uid, user.getNome(), userPhoto, texto, System.currentTimeMillis(), commentImageUrl);
                    db.collection("posts").document(postId).collection("comments").add(comment);
                    enviarNotificacaoComment(postAuthorId);
                    cancelarEdicaoOuResposta();
                }
            });
        }
    }

    private void enviarNotificacaoComment(String donoDoPostId) {
        if (donoDoPostId == null || donoDoPostId.equals(mAuth.getCurrentUser().getUid())) return;

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(doc -> {
            User eu = doc.toObject(User.class);
            if (eu != null) {
                Map<String, Object> notifMap = new HashMap<>();
                notifMap.put("targetUserId", donoDoPostId);
                notifMap.put("fromUserId", mAuth.getCurrentUser().getUid());
                notifMap.put("fromUserName", eu.getNome());
                notifMap.put("fromUserPhoto", eu.getPhotoUrl());
                notifMap.put("type", "comment");
                notifMap.put("message", "comentou a tua publicação");
                notifMap.put("postId", postId);
                notifMap.put("timestamp", System.currentTimeMillis());
                notifMap.put("read", false);

                db.collection("notifications").add(notifMap);
            }
        });
    }
}