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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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

        // Novos componentes do XML
        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
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
        String uid = mAuth.getCurrentUser().getUid();
        // Nome único para a imagem: posts/ID_USER/UUID.jpg
        String fileName = "post_images/" + uid + "/" + UUID.randomUUID().toString() + ".jpg";

        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Upload sucesso, agora pegar o link
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
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                        long timestamp = System.currentTimeMillis();
                        String nomeAutor = (user != null && user.getNome() != null) ? user.getNome() : "Anónimo";

                        // --- ATENÇÃO: PRECISAS DE ATUALIZAR O POST.JAVA PARA ESTE CONSTRUTOR ---
                        Post novoPost = new Post(
                                uid,
                                nomeAutor,
                                content,
                                dataHora,
                                timestamp,
                                imageUrl
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
                    // ATUALIZAR O ID DO POST NO DOCUMENTO
                    // Isto ajuda depois para apagar/editar o post mais facilmente
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