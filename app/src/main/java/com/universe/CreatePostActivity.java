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

    // --- RecyclerView para múltiplas imagens (Misto: Uri e String) ---
    private RecyclerView recyclerImagesPreview;
    private ImagesPreviewAdapter imagesAdapter;
    private List<Object> mixedImagesList = new ArrayList<>(); // Guarda Uris (novas) e Strings (velhas)

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private UserService userService;
    private NotificationService notificationService;

    // --- Launchers ---
    private ActivityResultLauncher<String> mGetGalleryContent;
    private ActivityResultLauncher<Uri> mTakePicture;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private Uri cameraImageUri = null;
    private String postIdParaEditar = null;

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
        configurarRecyclerView(); // Configurar antes de carregar dados
        configurarLaunchers();

        verificarSeEEdicao(); // Verifica se há dados para editar
        carregarDadosUtilizador();

        btnPublish.setOnClickListener(v -> prepararPublicacao());

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
        recyclerImagesPreview = findViewById(R.id.recyclerImagesPreview);
    }

    private void configurarRecyclerView() {
        // Agora aceita List<Object>
        imagesAdapter = new ImagesPreviewAdapter(mixedImagesList, position -> {
            mixedImagesList.remove(position);
            imagesAdapter.notifyItemRemoved(position);
            if (mixedImagesList.isEmpty()) recyclerImagesPreview.setVisibility(View.GONE);
        });
        recyclerImagesPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerImagesPreview.setAdapter(imagesAdapter);
    }

    private void configurarLaunchers() {
        // 1. Galeria (Múltipla)
        mGetGalleryContent = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris != null && !uris.isEmpty()) {
                mixedImagesList.addAll(uris); // Adiciona à lista mista
                recyclerImagesPreview.setVisibility(View.VISIBLE);
                imagesAdapter.notifyDataSetChanged();
            }
        });

        // 2. Câmara
        mTakePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                mixedImagesList.add(cameraImageUri); // Adiciona à lista mista
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

    // --- CARREGAR DADOS DE EDIÇÃO ---
    private void verificarSeEEdicao() {
        if (getIntent().hasExtra("editPostId")) {
            postIdParaEditar = getIntent().getStringExtra("editPostId");
            String conteudoAtual = getIntent().getStringExtra("currentContent");

            // Carregar imagens antigas (Strings)
            ArrayList<String> oldImages = getIntent().getStringArrayListExtra("currentImages");
            if (oldImages != null && !oldImages.isEmpty()) {
                mixedImagesList.addAll(oldImages);
                recyclerImagesPreview.setVisibility(View.VISIBLE);
                imagesAdapter.notifyDataSetChanged();
            }

            inputPostContent.setText(conteudoAtual);
            btnPublish.setText("Atualizar");
        }
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
            if (which == 0) verificarPermissaoCamera();
            else mGetGalleryContent.launch("image/*");
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

    // --- LÓGICA DE PUBLICAÇÃO / ATUALIZAÇÃO ---
    private void prepararPublicacao() {
        String content = inputPostContent.getText().toString().trim();
        if (TextUtils.isEmpty(content) && mixedImagesList.isEmpty()) return;

        btnPublish.setEnabled(false);
        btnPublish.setText(postIdParaEditar != null ? "A atualizar..." : "A publicar...");

        separarEFazerUpload(content);
    }

    private void separarEFazerUpload(String content) {
        List<String> finalImageUrls = new ArrayList<>(); // Lista final (URLs)
        List<Uri> imagesToUpload = new ArrayList<>();    // O que precisa de upload (Uris)

        // 1. Separar: O que é String (já existe) vs Uri (novo)
        for (Object item : mixedImagesList) {
            if (item instanceof String) {
                finalImageUrls.add((String) item);
            } else if (item instanceof Uri) {
                imagesToUpload.add((Uri) item);
            }
        }

        // 2. Se não houver nada novo para subir, finaliza
        if (imagesToUpload.isEmpty()) {
            finalizarPublicacao(content, finalImageUrls);
        } else {
            // 3. Fazer Upload das novas imagens
            fazerUploadDasNovas(content, finalImageUrls, imagesToUpload);
        }
    }

    private void fazerUploadDasNovas(String content, List<String> finalImageUrls, List<Uri> imagesToUpload) {
        List<Task<Uri>> uploadTasks = new ArrayList<>();
        String uid = mAuth.getCurrentUser().getUid();

        for (Uri imageUri : imagesToUpload) {
            String fileName = "post_images/" + uid + "/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);

            Task<Uri> task = ref.putFile(imageUri).continueWithTask(taskSnapshot -> {
                if (!taskSnapshot.isSuccessful()) throw taskSnapshot.getException();
                return ref.getDownloadUrl();
            });
            uploadTasks.add(task);
        }

        Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(results -> {
            for (Object result : results) {
                finalImageUrls.add(((Uri) result).toString());
            }
            finalizarPublicacao(content, finalImageUrls);
        }).addOnFailureListener(e -> {
            btnPublish.setEnabled(true);
            btnPublish.setText("Tentar Novamente");
            Toast.makeText(this, "Erro no upload", Toast.LENGTH_SHORT).show();
        });
    }

    private void finalizarPublicacao(String content, List<String> imageUrls) {
        // --- MODO EDIÇÃO ---
        if (postIdParaEditar != null) {
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("content", content);
            updates.put("imagesUrls", imageUrls); // Atualiza a lista completa

            // Atualiza capa
            if (!imageUrls.isEmpty()) updates.put("imageUrl", imageUrls.get(0));
            else updates.put("imageUrl", null);

            // IMPORTANTE: Usar o ID do post existente
            db.collection("posts").document(postIdParaEditar)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Post atualizado!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("Atualizar");
                        Toast.makeText(this, "Erro ao atualizar.", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        // --- MODO CRIAÇÃO ---
        criarNovoPost(content, imageUrls);
    }

    private void criarNovoPost(String content, List<String> imageUrls) {
        String uid = mAuth.getCurrentUser().getUid();
        int selectedTab = getIntent().getIntExtra("selectedTab", 0);
        String postType = (selectedTab == 1) ? "uni" : "global";

        String email = mAuth.getCurrentUser().getEmail();
        String domain = "geral";
        if (email != null && email.contains("@")) {
            domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        }
        final String finalDomain = domain;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String nome = doc.getString("nome");
            String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            String newPostId = db.collection("posts").document().getId();
            String firstImage = imageUrls.isEmpty() ? null : imageUrls.get(0);

            Post post = new Post(uid, nome, content, dataHora, System.currentTimeMillis(), firstImage, finalDomain, postType);
            post.setPostId(newPostId);
            post.setImagesUrls(imageUrls);

            db.collection("posts").document(newPostId).set(post)
                    .addOnSuccessListener(aVoid -> {
                        // Notificações
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

    // --- ADAPTER QUE ACEITA LISTA MISTA (String E Uri) ---
    private static class ImagesPreviewAdapter extends RecyclerView.Adapter<ImagesPreviewAdapter.ViewHolder> {
        private List<Object> items; // Mudou de Uri para Object
        private OnImageClickListener listener;

        interface OnImageClickListener {
            void onRemoveClick(int position);
        }

        public ImagesPreviewAdapter(List<Object> items, OnImageClickListener listener) {
            this.items = items;
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
            Object item = items.get(position);

            if (item != null) {
                // Glide é inteligente: aceita String (URL) ou Uri (Local) automaticamente
                Glide.with(holder.itemView.getContext())
                        .load(item)
                        .centerCrop()
                        .into(holder.img);
            }

            holder.btnRemove.setOnClickListener(v -> {
                if (listener != null) listener.onRemoveClick(holder.getAdapterPosition());
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
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