package com.universe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText editName, editUsername, editCourse, editUni;
    private Button btnSave;
    private ImageView imgAvatar;      // <--- NOVO
    private TextView btnChangePhoto;  // <--- NOVO

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef; // <--- NOVO

    // Variáveis para controlo
    private String usernameAtual;
    private long ultimaTrocaTimestamp = 0;
    private Uri imageUri; // Guarda a foto temporariamente

    // Lançador para abrir a Galeria
    private final ActivityResultLauncher<String> selectImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    imgAvatar.setImageURI(imageUri); // Mostra a preview logo no ecrã
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference(); // <--- Inicializar Storage

        // Ligar componentes
        editName = findViewById(R.id.editName);
        editUsername = findViewById(R.id.editUsername);
        editCourse = findViewById(R.id.editCourse);
        editUni = findViewById(R.id.editUni);
        btnSave = findViewById(R.id.btnSaveProfile);

        // Novos componentes de imagem
        imgAvatar = findViewById(R.id.imgEditAvatar);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);

        // Carregar dados
        carregarDadosAtuais();

        // Configurar clique para mudar foto
        View.OnClickListener openGallery = v -> selectImage.launch("image/*");
        imgAvatar.setOnClickListener(openGallery);
        btnChangePhoto.setOnClickListener(openGallery);

        btnSave.setOnClickListener(v -> prepararParaGuardar());
    }

    private void carregarDadosAtuais() {
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

                            // Carregar a FOTO atual com Glide (NOVO)
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

    // PASSO 1: Decide se faz upload da imagem primeiro ou vai direto para o texto
    private void prepararParaGuardar() {
        btnSave.setEnabled(false);
        btnSave.setText("A processar...");

        String uid = mAuth.getCurrentUser().getUid();

        if (imageUri != null) {
            // Se tem foto nova, fazemos upload primeiro
            btnSave.setText("A enviar foto...");
            StorageReference fileRef = storageRef.child("profile_images/" + uid + ".jpg");

            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String photoUrl = uri.toString();
                        // Foto enviada! Agora validamos o username e gravamos tudo
                        validarUsernameEGravar(photoUrl);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erro ao enviar imagem", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Guardar Alterações");
                    });
        } else {
            // Se não mudou a foto, avançamos só com texto (photoUrl é null)
            validarUsernameEGravar(null);
        }
    }

    // PASSO 2: A tua lógica original de validar Username (agora recebe a photoUrl)
    private void validarUsernameEGravar(String photoUrl) {
        btnSave.setText("A verificar dados...");

        final String novoNome = editName.getText().toString().trim();
        final String novoUsername = editUsername.getText().toString().trim().replace(" ", "").toLowerCase();
        final String novoCurso = editCourse.getText().toString().trim();
        final String novaUni = editUni.getText().toString().trim();

        if (TextUtils.isEmpty(novoNome) || TextUtils.isEmpty(novoUsername) || TextUtils.isEmpty(novoCurso)) {
            Toast.makeText(this, "Preenche todos os campos", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        // 1. Username IGUAL -> Grava direto
        if (novoUsername.equals(usernameAtual)) {
            atualizarBaseDeDados(novoNome, novoUsername, novoCurso, novaUni, 0, photoUrl);
        }
        else {
            // 2. Username MUDOU
            long agora = System.currentTimeMillis();
            long diferenca = agora - ultimaTrocaTimestamp;
            long seteDiasEmMs = TimeUnit.DAYS.toMillis(7);

            // A. Regra dos 7 Dias
            if (ultimaTrocaTimestamp != 0 && diferenca < seteDiasEmMs) {
                long diasRestantes = TimeUnit.MILLISECONDS.toDays(seteDiasEmMs - diferenca);
                editUsername.setError("Tens de esperar " + (diasRestantes + 1) + " dias para mudar.");
                btnSave.setEnabled(true);
                btnSave.setText("Guardar Alterações");
                return;
            }

            // B. Unicidade (Verificar no Firebase)
            db.collection("users")
                    .whereEqualTo("username", novoUsername)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            editUsername.setError("Este username já está ocupado.");
                            btnSave.setEnabled(true);
                            btnSave.setText("Guardar Alterações");
                        } else {
                            // Tudo ok! Grava e atualiza data de troca
                            atualizarBaseDeDados(novoNome, novoUsername, novoCurso, novaUni, agora, photoUrl);
                        }
                    });
        }
    }

    // PASSO 3: Gravar tudo na BD
    private void atualizarBaseDeDados(String nome, String username, String curso, String uni, long timestamp, String photoUrl) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("nome", nome);
        updates.put("username", username);
        updates.put("curso", curso);
        updates.put("universidade", uni);

        // Se houve troca de username, atualiza o timestamp
        if (timestamp > 0) {
            updates.put("ultimaTrocaUsername", timestamp);
        }

        // Se houve upload de foto, atualiza o link
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