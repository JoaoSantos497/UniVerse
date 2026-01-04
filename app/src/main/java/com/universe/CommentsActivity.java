package com.universe;

import static com.universe.NotificationType.COMMENT;

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
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommentsActivity extends AppCompatActivity {

    private EditText inputComment;
    private ImageButton btnSend, btnBack, btnAttach, btnRemoveImage;
    private RecyclerView recyclerView;
    private ImageView headerProfileImage;
    private TextView headerName, headerCourse;
    private Button btnHeaderFollow;
    private RelativeLayout commentImagePreviewContainer;
    private ImageView commentImagePreview;
    private RelativeLayout replyContainer;
    private TextView txtReplyingTo;
    private ImageButton btnCloseReply;

    private CommentsAdapter adapter;
    private List<Comment> commentList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    private String postId;
    private String postAuthorId;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> mGetContent;
    private String idComentarioEdicao = null;

    private NotificationService notificationService;

    private UserService userService;

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

        ligarComponentes();
        userService = new UserService();
        notificationService = new NotificationService(userService);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        adapter = new CommentsAdapter(commentList, username -> ativarModoResposta(username));
        recyclerView.setAdapter(adapter);

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                commentImagePreview.setImageURI(uri);
                commentImagePreviewContainer.setVisibility(View.VISIBLE);
            }
        });

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

    // --- MÉTODOS CHAMADOS PELO ADAPTER (DEVEM SER PUBLIC) ---

    public void prepararEdicaoComentario(Comment comment) {
        idComentarioEdicao = comment.getCommentId();
        replyContainer.setVisibility(View.VISIBLE);
        txtReplyingTo.setText("A editar o teu comentário...");
        inputComment.setText(comment.getContent());
        inputComment.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(inputComment, InputMethodManager.SHOW_IMPLICIT);

        btnSend.setImageResource(android.R.drawable.ic_menu_save);
        btnAttach.setVisibility(View.GONE);
    }

    public void apagarComentario(String commentId, int position) {
        db.collection("posts").document(postId)
                .collection("comments").document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (adapter != null) {
                        adapter.removerItemDaLista(position);
                    }
                    Toast.makeText(this, "Comentário removido", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao apagar comentário", Toast.LENGTH_SHORT).show());
    }

    // --- LÓGICA INTERNA ---

    private void carregarDadosDoPost() {
        db.collection("posts").document(postId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Post post = doc.toObject(Post.class);
                if (post != null) {
                    postAuthorId = post.getUserId();
                    carregarHeaderAutor(postAuthorId);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Não tens permissão para ver este post.", Toast.LENGTH_SHORT).show();
            finish();
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
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        commentList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null) {
                                comment.setCommentId(doc.getId());
                                commentList.add(comment);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (!commentList.isEmpty()) {
                            recyclerView.scrollToPosition(commentList.size() - 1);
                        }
                    }
                });
    }

    private void ativarModoResposta(String username) {
        replyContainer.setVisibility(View.VISIBLE);
        txtReplyingTo.setText("A responder a @" + username);
        inputComment.setText("@" + username + " ");
        inputComment.setSelection(inputComment.getText().length());
        inputComment.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(inputComment, InputMethodManager.SHOW_IMPLICIT);
    }

    private void cancelarEdicaoOuResposta() {
        idComentarioEdicao = null;
        replyContainer.setVisibility(View.GONE);
        inputComment.setText("");
        btnSend.setImageResource(R.drawable.ic_send);
        btnSend.setEnabled(true);
        btnAttach.setVisibility(View.VISIBLE);
        limparImagemSelecionada();
    }

    private void limparImagemSelecionada() {
        selectedImageUri = null;
        commentImagePreview.setImageURI(null);
        commentImagePreviewContainer.setVisibility(View.GONE);
    }

    private void prepararEnvio() {
        String texto = inputComment.getText().toString().trim();
        if (TextUtils.isEmpty(texto) && selectedImageUri == null) return;

        btnSend.setEnabled(false);

        if (idComentarioEdicao != null) {
            enviarComentarioFinal(texto, null);
        } else {
            if (selectedImageUri != null) {
                uploadImageAndSend(texto, selectedImageUri);
            } else {
                enviarComentarioFinal(texto, null);
            }
        }
    }

    private void uploadImageAndSend(String texto, Uri imageUri) {
        String uid = mAuth.getCurrentUser().getUid();
        String fileName = "comment_images/" + postId + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        Toast.makeText(this, "A enviar imagem...", Toast.LENGTH_SHORT).show();

        ref.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                ref.getDownloadUrl().addOnSuccessListener(uri ->
                        enviarComentarioFinal(texto, uri.toString())
                )
        ).addOnFailureListener(e -> {
            btnSend.setEnabled(true);
            Toast.makeText(this, "Erro no upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
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
                    })
                    .addOnFailureListener(e -> btnSend.setEnabled(true));
        } else {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                User user = doc.toObject(User.class);
                if (user != null) {
                    String userPhoto = user.getPhotoUrl() != null ? user.getPhotoUrl() : "";
                    Comment comment = new Comment(uid, user.getNome(), userPhoto, texto, System.currentTimeMillis(), commentImageUrl);

                    db.collection("posts").document(postId).collection("comments").add(comment)
                            .addOnSuccessListener(documentReference -> {
                                enviarNotificacaoComment(postAuthorId);
                                cancelarEdicaoOuResposta();
                            })
                            .addOnFailureListener(e -> {
                                btnSend.setEnabled(true);
                                Toast.makeText(this, "Erro ao comentar.", Toast.LENGTH_SHORT).show();
                            });
                }
            }).addOnFailureListener(e -> btnSend.setEnabled(true));
        }
    }

    private void enviarNotificacaoComment(String donoDoPostId) {
        if (donoDoPostId == null || donoDoPostId.equals(mAuth.getCurrentUser().getUid())) return;
        notificationService.sendNotification(donoDoPostId, COMMENT, postId);
    }
}