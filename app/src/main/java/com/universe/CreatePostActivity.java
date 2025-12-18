package com.universe;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private EditText inputPostContent;
    private Button btnPublish;
    private ImageButton btnSelectPhoto;
    private ImageView imagePreview;
    private ImageButton btnRemoveImage;
    private TextView currentUserName;
    private ImageView currentUserImage;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> mGetContent;

    // VARIÁVEIS PARA EDIÇÃO
    private String editPostId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ligarComponentes();

        // --- VERIFICAÇÃO DE MODO ---
        editPostId = getIntent().getStringExtra("editPostId");
        String contentParaEditar = getIntent().getStringExtra("currentContent");

        if (editPostId != null) {
            inputPostContent.setText(contentParaEditar);
            btnPublish.setText("Atualizar");
            // Na edição, escondemos o botão de foto para não complicar a lógica de substituição no Storage
            btnSelectPhoto.setVisibility(View.GONE);
        }

        configurarGestoesDeImagem();
        carregarDadosUtilizador();

        btnPublish.setOnClickListener(v -> prepararPublicacao());
        findViewById(R.id.btnCloseCreatePost).setOnClickListener(v -> finish());
    }

    private void ligarComponentes() {
        inputPostContent = findViewById(R.id.inputPostContent);
        btnPublish = findViewById(R.id.btnPublish);
        btnSelectPhoto = findViewById(R.id.btnAddImage);
        currentUserName = findViewById(R.id.currentUserName);
        currentUserImage = findViewById(R.id.currentUserImage);
        imagePreview = findViewById(R.id.imagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);
    }

    private void configurarGestoesDeImagem() {
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                imagePreview.setImageURI(uri);
                imagePreview.setVisibility(View.VISIBLE);
                btnRemoveImage.setVisibility(View.VISIBLE);
            }
        });

        btnSelectPhoto.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            imagePreview.setVisibility(View.GONE);
            btnRemoveImage.setVisibility(View.GONE);
        });
    }

    private void carregarDadosUtilizador() {
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currentUserName.setText(doc.getString("nome"));
                            String photo = doc.getString("photoUrl");
                            if (photo != null && !photo.isEmpty()) {
                                Glide.with(this).load(photo).circleCrop().into(currentUserImage);
                            }
                        }
                    });
        }
    }

    private void prepararPublicacao() {
        String content = inputPostContent.getText().toString().trim();
        if (TextUtils.isEmpty(content) && selectedImageUri == null) return;

        btnPublish.setEnabled(false);
        btnPublish.setText("A processar...");

        if (editPostId != null) {
            // Se estamos a editar, apenas atualizamos o conteúdo de texto do documento original
            atualizarPostNoFirestore(content);
        } else {
            // Se é novo, verificamos se tem imagem
            if (selectedImageUri != null) {
                fazerUploadImagem(content);
            } else {
                criarNovoPost(content, null);
            }
        }
    }

    private void atualizarPostNoFirestore(String novoConteudo) {
        db.collection("posts").document(editPostId)
                .update("content", novoConteudo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Atualizado!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Atualizar");
                });
    }

    private void fazerUploadImagem(String content) {
        String fileName = "post_images/" + mAuth.getCurrentUser().getUid() + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> criarNovoPost(content, uri.toString())))
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publicar");
                    Toast.makeText(this, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show();
                });
    }

    private void criarNovoPost(String content, String imageUrl) {
        String uid = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();
        String domain = (email != null && email.contains("@")) ?email.substring(email.indexOf("@") + 1).toLowerCase() : "geral";
        int selectedTab = getIntent().getIntExtra("selectedTab", 0);
        String postType = (selectedTab == 1) ? "uni" : "global";

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String nome = doc.getString("nome");
            String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

            // 1. Geramos um ID manualmente ANTES de enviar.
            // Isso garante que o postId já vai preenchido e o SnapshotListener o deteta logo.
            String newPostId = db.collection("posts").document().getId();

            Post post = new Post(uid, nome, content, dataHora, System.currentTimeMillis(), imageUrl, domain, postType);
            post.setPostId(newPostId); // Define o ID no objeto

            // 2. Usamos .document(newPostId).set() em vez de .add()
            db.collection("posts").document(newPostId).set(post)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Publicado!", Toast.LENGTH_SHORT).show();
                        finish(); // Ao fechar, o FeedTabFragment já terá o post novo via SnapshotListener
                    })
                    .addOnFailureListener(e -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("Publicar");
                    });
        });
    }

}