package com.universe;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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
    private Button btnPublish, btnSelectPhoto;

    // UI para a Imagem
    private RelativeLayout imagePreviewContainer;
    private ImageView imagePreview;
    private ImageButton btnRemoveImage;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> mGetContent;

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

        // Componentes do XML
        ImageButton btnBack = findViewById(R.id.btnCloseCreatePost);
        imagePreview = findViewById(R.id.imagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);

        // Botão Voltar
        btnBack.setOnClickListener(v -> finish());

        // --- 1. CONFIGURAR A GALERIA ---
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            selectedImageUri = uri;
                            imagePreview.setImageURI(uri);
                            imagePreviewContainer.setVisibility(View.VISIBLE);
                        }
                    }
                });

        // Clique para abrir galeria
        btnSelectPhoto.setOnClickListener(v -> mGetContent.launch("image/*"));

        // Clique para remover imagem
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            imagePreview.setImageURI(null);
            imagePreviewContainer.setVisibility(View.GONE);
        });

        // Configurar Botão Publicar
        btnPublish.setOnClickListener(v -> prepararPublicacao());
    }

    private void prepararPublicacao() {
        String content = inputPostContent.getText().toString().trim();

        // Só bloqueia se não houver texto E não houver imagem
        if (TextUtils.isEmpty(content) && selectedImageUri == null) {
            inputPostContent.setError("Escreve algo ou adiciona uma foto!");
            return;
        }

        btnPublish.setEnabled(false);
        btnPublish.setText("A publicar...");

        if (selectedImageUri != null) {
            // Se tem imagem, faz upload primeiro
            fazerUploadImagem(content);
        } else {
            // Se não tem imagem, segue direto
            buscarDadosUsuarioEGuardar(content, null);
        }
    }

    private void fazerUploadImagem(String content) {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        String fileName = "post_images/" + uid + "/" + UUID.randomUUID().toString() + ".jpg";

        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        buscarDadosUsuarioEGuardar(content, downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreatePostActivity.this, "Erro ao enviar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publicar");
                });
    }

    private void buscarDadosUsuarioEGuardar(String content, String imageUrl) {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();

        // --- 1. EXTRAIR DOMÍNIO DA UNIVERSIDADE ---
        String dominioTemp = "geral";
        if (email != null && email.contains("@")) {
            dominioTemp = email.substring(email.indexOf("@") + 1);
        }
        final String universityDomain = dominioTemp;

        // --- 2. RECEBER O TIPO DE POST (VEM DO HOMEFRAGMENT) ---
        // 0 = Global, 1 = Minha Uni
        int selectedTab = getIntent().getIntExtra("selectedTab", 0);
        String type;
        if (selectedTab == 1) {
            type = "uni";
        } else {
            type = "global";
        }
        final String postTypeFinal = type;
        // -------------------------------------------------------

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                        long timestamp = System.currentTimeMillis();
                        String nomeAutor = (user != null && user.getNome() != null) ? user.getNome() : "Anónimo";

                        // --- 3. CRIAR POST COM TODOS OS DADOS ---
                        Post novoPost = new Post(
                                uid,
                                nomeAutor,
                                content,
                                dataHora,
                                timestamp,
                                imageUrl,
                                universityDomain,
                                postTypeFinal
                        );

                        guardarNoFirestore(novoPost);

                    } else {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("Publicar");
                        Toast.makeText(CreatePostActivity.this, "Erro: Utilizador não encontrado.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreatePostActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publicar");
                });
    }

    private void guardarNoFirestore(Post post) {
        db.collection("posts").add(post)
                .addOnSuccessListener(documentReference -> {
                    // Atualizar o ID do post no documento
                    String postId = documentReference.getId();
                    documentReference.update("postId", postId);

                    Toast.makeText(CreatePostActivity.this, "Publicado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreatePostActivity.this, "Erro ao publicar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publicar");
                });
    }
}