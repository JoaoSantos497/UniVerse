package com.universe;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreatePostActivity extends AppCompatActivity {

    private EditText inputPostContent;
    private Button btnPublish;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ligar UI
        inputPostContent = findViewById(R.id.inputPostContent);
        btnPublish = findViewById(R.id.btnPublish);

        // Configurar Botão
        btnPublish.setOnClickListener(v -> publicarPost());
    }

    private void publicarPost() {
        String content = inputPostContent.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            inputPostContent.setError("Escreve qualquer coisa!");
            return;
        }

        btnPublish.setEnabled(false);
        btnPublish.setText("A publicar...");

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // O utilizador existe! Vamos publicar.
                            User user = documentSnapshot.toObject(User.class);

                            String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                            long timestamp = System.currentTimeMillis();

                            // Usa o nome do utilizador ou "Anónimo" se vier vazio
                            String nomeAutor = (user.getNome() != null) ? user.getNome() : "Anónimo";

                            Post novoPost = new Post(uid, nomeAutor, content, dataHora, timestamp);
                            guardarNoFirestore(novoPost);

                        } else {
                            // --- AQUI ESTÁ A CORREÇÃO ---
                            // Se o utilizador não existir na BD, desbloqueia o botão e avisa.
                            btnPublish.setEnabled(true);
                            btnPublish.setText("Publicar");
                            Toast.makeText(CreatePostActivity.this, "Erro: A tua conta está incompleta. Faz logout e cria uma nova.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CreatePostActivity.this, "Erro de ligação: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnPublish.setEnabled(true);
                        btnPublish.setText("Publicar");
                    }
                });
    }

    // Guardar na base de dados
    private void guardarNoFirestore(Post post) {
        db.collection("posts").add(post)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(CreatePostActivity.this, "Publicado com sucesso!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CreatePostActivity.this, "Erro ao publicar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnPublish.setEnabled(true);
                        btnPublish.setText("Publicar");
                    }
                });
    }
}