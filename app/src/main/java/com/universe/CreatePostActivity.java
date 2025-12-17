package com.universe;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout; // Importante para o container
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
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

        // Dados do User (Topo)
        currentUserName = findViewById(R.id.currentUserName);
        currentUserImage = findViewById(R.id.currentUserImage); // Faltava inicializar isto!

        // Componentes da Imagem (Preview)
        ImageButton btnBack = findViewById(R.id.btnCloseCreatePost);
        imagePreview = findViewById(R.id.imagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);

        // Botão Voltar
        btnBack.setOnClickListener(v -> finish());

        // --- 1. CARREGAR DADOS DO UTILIZADOR (NOME E FOTO) ---
        carregarDadosUtilizador();

        // --- 2. CONFIGURAR A GALERIA ---
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            selectedImageUri = uri;
                            imagePreview.setImageURI(uri);

                            // Mostrar a imagem e o botão de remover
                            imagePreview.setVisibility(View.VISIBLE);
                            btnRemoveImage.setVisibility(View.VISIBLE);
                        }
                    }
                });

        // Clique para abrir galeria
        btnSelectPhoto.setOnClickListener(v -> mGetContent.launch("image/*"));

        // Clique para remover imagem
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            imagePreview.setImageURI(null);

            // Esconder a imagem e o botão de remover
            imagePreview.setVisibility(View.GONE);
            btnRemoveImage.setVisibility(View.GONE);
        });

        // Configurar Botão Publicar
        btnPublish.setOnClickListener(v -> prepararPublicacao());
    }

    // --- Metodo para carregar perfil ---
    private void carregarDadosUtilizador() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            Uri authPhotoUrl = mAuth.getCurrentUser().getPhotoUrl(); // 1. Tentar obter do Auth

            // Se existir no Auth, carrega imediatamente
            if (authPhotoUrl != null) {
                Glide.with(CreatePostActivity.this)
                        .load(authPhotoUrl)
                        .circleCrop()
                        .into(currentUserImage);
            }

            // Vai à base de dados confirmar (pode haver uma foto mais recente lá)
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Carregar Nome
                            String nome = documentSnapshot.getString("nome");
                            if (nome == null) nome = documentSnapshot.getString("username"); // Tenta username também
                            if (nome != null) {
                                currentUserName.setText(nome);
                            }

                            // 2. Tentar encontrar a foto no Firestore com VÁRIOS nomes
                            String firestorePhoto = documentSnapshot.getString("photoUrl");
                            if (firestorePhoto == null) firestorePhoto = documentSnapshot.getString("profileImage");
                            if (firestorePhoto == null) firestorePhoto = documentSnapshot.getString("imagem");
                            if (firestorePhoto == null) firestorePhoto = documentSnapshot.getString("image");
                            if (firestorePhoto == null) firestorePhoto = documentSnapshot.getString("profileUrl");

                            // Se encontrou no Firestore, usa essa (sobrescreve a do Auth se existir)
                            if (firestorePhoto != null && !firestorePhoto.isEmpty()) {
                                Glide.with(CreatePostActivity.this)
                                        .load(firestorePhoto)
                                        .circleCrop()
                                        .placeholder(R.drawable.circle_bg)
                                        .into(currentUserImage);
                            } else {
                                // DEBUG: Se não encontrou no Firestore e também não tinha no Auth
                                if (authPhotoUrl == null) {
                                    // Podes comentar esta linha depois de testar
                                    // Toast.makeText(CreatePostActivity.this, "Aviso: Nenhuma foto encontrada na BD", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CreatePostActivity.this, "Erro de ligação à BD.", Toast.LENGTH_SHORT).show();
                    });
        }
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
            fazerUploadImagem(content);
        } else {
            buscarDadosUserEGuardar(content, null);
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
                        buscarDadosUserEGuardar(content, downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreatePostActivity.this, "Erro ao enviar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publicar");
                });
    }

    private void buscarDadosUserEGuardar(String content, String imageUrl) {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();

        String dominioTemp = "geral";
        if (email != null && email.contains("@")) {
            dominioTemp = email.substring(email.indexOf("@") + 1);
        }
        final String universityDomain = dominioTemp;

        int selectedTab = getIntent().getIntExtra("selectedTab", 0);
        String type;
        if (selectedTab == 1) {
            type = "uni";
        } else {
            type = "global";
        }
        final String postTypeFinal = type;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                        long timestamp = System.currentTimeMillis();
                        String nomeAutor = (user != null && user.getNome() != null) ? user.getNome() : "Anónimo";

                        // Tentar obter a foto do objeto User também, caso exista
                        String userPhotoUrl = null;
                        //userPhotoUrl = user.getphotoUrl(); // Se tiveres este método no User.java

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