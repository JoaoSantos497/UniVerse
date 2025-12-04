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
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publicarPost();
            }
        });
    }

    private void publicarPost() {
        String content = inputPostContent.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            inputPostContent.setError("Escreve qualquer coisa!");
            return;
        }

        // Desativar botão para evitar cliques duplos
        btnPublish.setEnabled(false);
        btnPublish.setText("A publicar...");

        String uid = mAuth.getCurrentUser().getUid();

        // 1. Buscar o nome do utilizador à base de dados
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);

                            // 2. Preparar os dados do Post
                            // Data para mostrar (Texto)
                            String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                            // Timestamp para ordenar (Número)
                            long timestamp = System.currentTimeMillis();
                            // 3. Criar o objeto Post (com os 4 argumentos)
                            Post novoPost = new Post(uid, user.getNome(), content, dataHora, timestamp);
                            // 4. Guardar
                            guardarNoFirestore(novoPost);
                        }
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CreatePostActivity.this, "Erro ao obter utilizador", Toast.LENGTH_SHORT).show();
                        btnPublish.setEnabled(true);
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
                        finish(); // Fecha a janela e volta ao Feed
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