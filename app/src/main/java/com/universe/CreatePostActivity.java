package com.universe;

import static com.universe.NotificationType.POST;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private EditText inputPostContent;
    private Button btnPublish;
    private ImageButton btnCloseCreatePost;
    private ImageView currentUserImage;
    private TextView currentUserName;
    private LinearLayout btnContainerAddImage;

    // --- RecyclerView para múltiplas imagens ---
    private RecyclerView recyclerImagesPreview;
    private ImagesPreviewAdapter imagesAdapter;
    private List<Uri> selectedImagesUris = new ArrayList<>(); // Lista de URIs locais

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private UserService userService;
    private NotificationService notificationService;

    // --- Launchers ---
    private ActivityResultLauncher<String> mGetGalleryContent; // Para Galeria
    private ActivityResultLauncher<Uri> mTakePicture;          // Para Câmara
    private ActivityResultLauncher<String> requestPermissionLauncher; // Permissão Câmara

    private Uri cameraImageUri = null; // URI temporário para a foto da câmara

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        userService = new UserService();
        notificationService = new NotificationService(userService);

        ligarComponentes();
        configurarRecyclerView();
        configurarLaunchers();

        carregarDadosUtilizador();

        btnPublish.setOnClickListener(v -> prepararPublicacao());

        // Ao clicar, abre diálogo de escolha
        if (btnContainerAddImage != null) {
            btnContainerAddImage.setOnClickListener(v -> mostrarDialogoEscolha());
        }
    }

    private void ligarComponentes() {
        inputPostContent = findViewById(R.id.inputPostContent);
        btnPublish = findViewById(R.id.btnPublish);
        btnCloseCreatePost = findViewById(R.id.btnCloseCreatePost);
        if (btnCloseCreatePost != null) btnCloseCreatePost.setOnClickListener(v -> finish());

        currentUserImage = findViewById(R.id.currentUserImage);
        currentUserName = findViewById(R.id.currentUserName);
        btnContainerAddImage = findViewById(R.id.btnContainerAddImage);

        // Recycler
        recyclerImagesPreview = findViewById(R.id.recyclerImagesPreview);
    }

    private void configurarRecyclerView() {
        imagesAdapter = new ImagesPreviewAdapter(selectedImagesUris, position -> {
            selectedImagesUris.remove(position);
            imagesAdapter.notifyItemRemoved(position);
            if (selectedImagesUris.isEmpty()) recyclerImagesPreview.setVisibility(View.GONE);
        });
        recyclerImagesPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerImagesPreview.setAdapter(imagesAdapter);
    }

    private void configurarLaunchers() {
        // 1. Galeria (Múltipla)
        mGetGalleryContent = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris != null && !uris.isEmpty()) {
                selectedImagesUris.addAll(uris);
                recyclerImagesPreview.setVisibility(View.VISIBLE);
                imagesAdapter.notifyDataSetChanged();
            }
        });

        // 2. Câmara
        mTakePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                selectedImagesUris.add(cameraImageUri);
                recyclerImagesPreview.setVisibility(View.VISIBLE);
                imagesAdapter.notifyDataSetChanged();
            }
        });

        // 3. Permissão
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) abrirCamera();
            else Toast.makeText(this, "Permissão de câmara necessária", Toast.LENGTH_SHORT).show();
        });
    }

    private void carregarDadosUtilizador() {
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currentUserName.setText(doc.getString("nome"));
                            String photo = doc.getString("photoUrl");

                            // CORREÇÃO: Verifica se é null antes de carregar
                            if (photo != null && !photo.isEmpty()) {
                                Glide.with(this).load(photo).circleCrop().into(currentUserImage);
                            } else {
                                currentUserImage.setImageResource(R.drawable.ic_person_filled);
                            }
                        }
                    });
        }
    }

    private void mostrarDialogoEscolha() {
        String[] options = {"Tirar Foto", "Escolher da Galeria"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adicionar Imagem");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                verificarPermissaoCamera();
            } else {
                mGetGalleryContent.launch("image/*");
            }
        });
        builder.show();
    }

    private void verificarPermissaoCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamera() {
        try {
            File photoFile = criarFicheiroTemporario();
            // Gera URI seguro usando FileProvider
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            mTakePicture.launch(cameraImageUri);
        } catch (IOException ex) {
            Toast.makeText(this, "Erro ao criar ficheiro de imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private File criarFicheiroTemporario() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void prepararPublicacao() {
        String content = inputPostContent.getText().toString().trim();
        if (TextUtils.isEmpty(content) && selectedImagesUris.isEmpty()) return;

        btnPublish.setEnabled(false);
        btnPublish.setText("A publicar...");

        if (!selectedImagesUris.isEmpty()) {
            fazerUploadMultiplasImagens(content);
        } else {
            criarNovoPost(content, new ArrayList<>()); // Lista vazia
        }
    }

    private void fazerUploadMultiplasImagens(String content) {
        List<String> uploadedUrls = new ArrayList<>();
        List<Task<Uri>> uploadTasks = new ArrayList<>();
        String uid = mAuth.getCurrentUser().getUid();

        // Cria uma tarefa de upload para CADA imagem na lista
        for (Uri imageUri : selectedImagesUris) {
            String fileName = "post_images/" + uid + "/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);

            // Faz upload e depois obtém a URL de download
            Task<Uri> task = ref.putFile(imageUri).continueWithTask(taskSnapshot -> {
                if (!taskSnapshot.isSuccessful()) throw taskSnapshot.getException();
                return ref.getDownloadUrl();
            });
            uploadTasks.add(task);
        }

        // Espera que TODAS as tarefas terminem
        Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(results -> {
            for (Object result : results) {
                uploadedUrls.add(((Uri) result).toString());
            }
            // Agora que temos todas as URLs, criamos o post
            criarNovoPost(content, uploadedUrls);
        }).addOnFailureListener(e -> {
            btnPublish.setEnabled(true);
            btnPublish.setText("Publicar");
            Toast.makeText(this, "Erro no upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void criarNovoPost(String content, List<String> imageUrls) {
        String uid = mAuth.getCurrentUser().getUid();

        // 1. Obter o tipo de post (Global vs Uni) baseado na aba de onde vieste
        int selectedTab = getIntent().getIntExtra("selectedTab", 0);
        String postType = (selectedTab == 1) ? "uni" : "global";

        // 2. Obter o domínio do email (para saber de que universidade é)
        String email = mAuth.getCurrentUser().getEmail();
        String domain = "geral";
        if (email != null && email.contains("@")) {
            domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        }

        // Variável final para usar dentro do lambda
        final String finalDomain = domain;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String nome = doc.getString("nome");
            String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            String newPostId = db.collection("posts").document().getId();

            // Define a primeira imagem como capa (para retrocompatibilidade)
            String firstImage = imageUrls.isEmpty() ? null : imageUrls.get(0);

            // CRIAR O POST COM OS DADOS DINÂMICOS
            Post post = new Post(uid, nome, content, dataHora, System.currentTimeMillis(), firstImage, finalDomain, postType);

            post.setPostId(newPostId);

            // --- AQUI ESTÁ A LISTA DE IMAGENS (O QUE FAZ O SLIDER FUNCIONAR) ---
            post.setImagesUrls(imageUrls);

            db.collection("posts").document(newPostId).set(post)
                    .addOnSuccessListener(aVoid -> {
                        // Notificar seguidores
                        db.collection("users").document(uid).collection("followers").get()
                                .addOnSuccessListener(querySnapshot -> {
                                    for (com.google.firebase.firestore.DocumentSnapshot document : querySnapshot.getDocuments()) {
                                        notificationService.sendNotification(document.getId(), POST);
                                    }
                                });
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("Publicar");
                        Toast.makeText(this, "Erro ao publicar.", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // --- ADAPTER INTERNO COM CORREÇÕES ---
    private static class ImagesPreviewAdapter extends RecyclerView.Adapter<ImagesPreviewAdapter.ViewHolder> {
        private List<Uri> uris;
        private OnImageClickListener listener;

        interface OnImageClickListener {
            void onRemoveClick(int position);
        }

        public ImagesPreviewAdapter(List<Uri> uris, OnImageClickListener listener) {
            this.uris = uris;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_upload_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Uri imageUri = uris.get(position);

            if (imageUri != null) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUri)
                        .centerCrop()
                        .into(holder.img);
            } else {
                holder.img.setImageDrawable(null);
            }

            holder.btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveClick(holder.getAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() {
            return uris.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            ImageButton btnRemove;

            ViewHolder(View v) {
                super(v);
                img = v.findViewById(R.id.itemImage);
                btnRemove = v.findViewById(R.id.itemBtnRemove);
            }
        }
    }
}