package com.universe;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText editName, editUsername, editCourse, editUni;
    private Button btnSave;
    private ImageView imgAvatar;
    private TextView btnChangePhoto;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private String usernameAtual;
    private long ultimaTrocaTimestamp = 0;
    private Uri imageUri; // Guarda a foto FINAL (já recortada)

    // 1. LANÇADOR DA GALERIA (Antigo, mas ligeiramente modificado)
    // Quando escolhe uma imagem, em vez de mostrar logo, lança o Cropper.
    private final ActivityResultLauncher<String> selectImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Em vez de mostrar logo, iniciamos o recorte
                    iniciarRecorte(uri);
                }
            }
    );

    // 2. Resize da imagem deperfil
        private final ActivityResultLauncher<CropImageContractOptions> cropImage = registerForActivityResult(
            new CropImageContract(),
            result -> {
                if (result.isSuccessful()) {
                    // SUCESSO: Temos a imagem cortada!
                    imageUri = result.getUriContent();
                    // Mostramos a imagem cortada no ecrã com Glide
                    Glide.with(this).load(imageUri).circleCrop().into(imgAvatar);
                } else {
                    // ERRO ou CANCELADO
                    Exception error = result.getError();
                    if (error != null) {
                        Toast.makeText(this, "Erro ao recortar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Ligar componentes
        editName = findViewById(R.id.editName);
        editUsername = findViewById(R.id.editUsername);
        editCourse = findViewById(R.id.editCourse);
        editUni = findViewById(R.id.editUni);
        btnSave = findViewById(R.id.btnSaveProfile);
        imgAvatar = findViewById(R.id.imgEditAvatar);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);

        carregarDadosAtuais();

        // Configurar clique para mudar foto (abre a galeria normal)
        View.OnClickListener openGallery = v -> selectImage.launch("image/*");
        imgAvatar.setOnClickListener(openGallery);
        btnChangePhoto.setOnClickListener(openGallery);

        btnSave.setOnClickListener(v -> prepararParaGuardar());
    }


    private void iniciarRecorte(Uri sourceUri) {
        CropImageOptions options = new CropImageOptions();

        options.activityTitle = "Ajustar Foto";
        options.fixAspectRatio = true;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.cropShape = CropImageView.CropShape.OVAL;
        options.guidelines = CropImageView.Guidelines.ON;

        // --- CORES DE ALTO CONTRASTE ---
        // Fundo da barra: Preto (ou Roxo da tua app)
        options.toolbarColor = Color.parseColor("#6200EE");

        // Cor do Botão "✔" e Título: BRANCO
        options.activityMenuIconColor = Color.WHITE;
        options.toolbarTitleColor = Color.WHITE;

        // Fundo geral
        options.backgroundColor = Color.parseColor("#90000000");

        cropImage.launch(new CropImageContractOptions(sourceUri, options));
    }


    // --- O RESTO DO CÓDIGO MANTÉM-SE IGUAL ---
    // (carregarDadosAtuais, prepararParaGuardar, validarUsernameEGravar, atualizarBaseDeDados)

    private void carregarDadosAtuais() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        btnSave.setEnabled(false);
        btnSave.setText("A carregar...");

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            editName.setText(user.getNome());
                            editUsername.setText(user.getUsername());
                            editCourse.setText(user.getCurso());
                            editUni.setText(user.getUniversidade());

                            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(imgAvatar);
                            }

                            usernameAtual = user.getUsername();
                            ultimaTrocaTimestamp = user.getUltimaTrocaUsername();

                            btnSave.setEnabled(true);
                            btnSave.setText("Guardar Alterações");
                        }
                    }
                });
    }

    private void prepararParaGuardar() {
        btnSave.setEnabled(false);
        btnSave.setText("A processar...");

        String uid = mAuth.getCurrentUser().getUid();

        if (imageUri != null) {
            // Se tem foto nova (já recortada), fazemos upload
            btnSave.setText("A enviar foto...");
            // Usamos uma pasta 'profile_images' e o nome do ficheiro é o UID do user
            StorageReference fileRef = storageRef.child("profile_images/" + uid + ".jpg");

            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String photoUrl = uri.toString();
                        validarUsernameEGravar(photoUrl);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erro ao enviar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Guardar Alterações");
                    });
        } else {
            validarUsernameEGravar(null);
        }
    }

    private void validarUsernameEGravar(String photoUrl) {
        btnSave.setText("A verificar dados...");

        final String novoNome = editName.getText().toString().trim();
        final String novoUsername = editUsername.getText().toString().trim().replace(" ", "").toLowerCase();
        final String novoCurso = editCourse.getText().toString().trim();
        final String novaUni = editUni.getText().toString().trim();

        if (TextUtils.isEmpty(novoNome) || TextUtils.isEmpty(novoUsername) || TextUtils.isEmpty(novoCurso)) {
            Toast.makeText(this, "Preenche os campos obrigatórios", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            btnSave.setText("Guardar Alterações");
            return;
        }

        if (novoUsername.equals(usernameAtual)) {
            atualizarBaseDeDados(novoNome, novoUsername, novoCurso, novaUni, 0, photoUrl);
        } else {
            long agora = System.currentTimeMillis();
            long diferenca = agora - ultimaTrocaTimestamp;
            long seteDiasEmMs = TimeUnit.DAYS.toMillis(7);

            if (ultimaTrocaTimestamp != 0 && diferenca < seteDiasEmMs) {
                long diasRestantes = TimeUnit.MILLISECONDS.toDays(seteDiasEmMs - diferenca);
                editUsername.setError("Tens de esperar " + (diasRestantes + 1) + " dias para mudar.");
                btnSave.setEnabled(true);
                btnSave.setText("Guardar Alterações");
                return;
            }

            db.collection("users")
                    .whereEqualTo("username", novoUsername)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            editUsername.setError("Este username já está ocupado.");
                            btnSave.setEnabled(true);
                            btnSave.setText("Guardar Alterações");
                        } else {
                            atualizarBaseDeDados(novoNome, novoUsername, novoCurso, novaUni, agora, photoUrl);
                        }
                    });
        }
    }

    private void atualizarBaseDeDados(String nome, String username, String curso, String uni, long timestamp, String photoUrl) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("nome", nome);
        updates.put("username", username);
        updates.put("curso", curso);
        updates.put("universidade", uni);

        if (timestamp > 0) {
            updates.put("ultimaTrocaUsername", timestamp);
        }

        if (photoUrl != null) {
            updates.put("photoUrl", photoUrl);
        }

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(EditProfileActivity.this, "Perfil atualizado!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Guardar Alterações");
                });
    }
}