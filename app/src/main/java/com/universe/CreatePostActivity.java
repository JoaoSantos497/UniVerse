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

    // UI para a Imagem
    private ImageView imagePreview;
    private ImageButton btnRemoveImage;

    // UI do Utilizador (Topo)
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

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Ligar UI
        inputPostContent = findViewById(R.id.inputPostContent);
        btnPublish = findViewById(R.id.btnPublish);
        btnSelectPhoto = findViewById(R.id.btnAddImage);
        currentUserName = findViewById(R.id.currentUserName);
        currentUserImage = findViewById(R.id.currentUserImage);
        ImageButton btnBack = findViewById(R.id.btnCloseCreatePost);
        imagePreview = findViewById(R.id.imagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);

        // --- VERIFICAÇÃO DE MODO (EDIÇÃO OU CRIAÇÃO) ---
        editPostId = getIntent().getStringExtra("editPostId");
        String contentParaEditar = getIntent().getStringExtra("currentContent");

        if (editPostId != null) {
            inputPostContent.setText(contentParaEditar);
            btnPublish.setText("Atualizar");
            btnSelectPhoto.setVisibility(View.GONE); // Desativar troca de foto na edição (opcional)
        }

        btnBack.setOnClickListener(v -> finish());

        carregarDadosUtilizador();

        // Configurar Galeria
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
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

        btnPublish.setOnClickListener(v -> prepararPublicacao());
    }

    private void carregarDadosUtilizador() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nome = documentSnapshot.getString("nome");
                            if (nome != null) currentUserName.setText(nome);

                            String photo = documentSnapshot.getString("photoUrl");
                            if (photo != null && !photo.isEmpty()) {
                                Glide.with(this).load(photo).circleCrop().into(currentUserImage);
                            }
                        }
                    });
        }
    }

    private void prepararPublicacao() {
        String content = inputPostContent.getText().toString().trim();

        if (TextUtils.isEmpty(content) && selectedImageUri == null) {
            Toast.makeText(this, "Escreve algo!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPublish.setEnabled(false);
        btnPublish.setText(editPostId != null ? "A atualizar..." : "A publicar...");

        if (editPostId != null) {
            // MODO EDIÇÃO: Atualiza apenas o texto diretamente no post original
            atualizarPostExistente(content);
        } else {
            // MODO CRIAÇÃO: Segue o fluxo normal
            if (selectedImageUri != null) {
                fazerUploadImagem(content);
            } else {
                buscarDadosUserEGuardar(content, null);
            }
        }
    }

    private void atualizarPostExistente(String novoTexto) {
        db.collection("posts").document(editPostId)
                .update("content", novoTexto)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Publicação atualizada!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Atualizar");
                    Toast.makeText(this, "Erro ao atualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- OS MÉTODOS ABAIXO SÃO PARA CRIAÇÃO DE NOVOS POSTS ---

    private void fazerUploadImagem(String content) {
        String uid = mAuth.getCurrentUser().getUid();
        String fileName = "post_images/" + uid + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    buscarDadosUserEGuardar(content, uri.toString());
                }))
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publicar");
                });
    }

    private void buscarDadosUserEGuardar(String content, String imageUrl) {
        String uid = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();

        String universityDomain = (email != null && email.contains("@")) ?
                email.substring(email.indexOf("@") + 1) : "geral";

        int selectedTab = getIntent().getIntExtra("selectedTab", 0);
        String postTypeFinal = (selectedTab == 1) ? "uni" : "global";

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String nomeAutor = doc.getString("nome");
                String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                long timestamp = System.currentTimeMillis();

                Post novoPost = new Post(
                        uid,
                        nomeAutor != null ? nomeAutor : "Anónimo",
                        content,
                        dataHora,
                        timestamp,
                        imageUrl,
                        universityDomain,
                        postTypeFinal
                );

                guardarNoFirestore(novoPost);
            }
        });
    }

    private void guardarNoFirestore(Post post) {
        db.collection("posts").add(post)
                .addOnSuccessListener(documentReference -> {
                    documentReference.update("postId", documentReference.getId());
                    Toast.makeText(this, "Publicado!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}