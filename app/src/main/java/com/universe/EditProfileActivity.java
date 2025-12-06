package com.universe;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText editName, editUsername, editCourse, editUni;
    private Button btnSave;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Variáveis para controlo
    private String usernameAtual;
    private long ultimaTrocaTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ligar componentes
        editName = findViewById(R.id.editName);
        editUsername = findViewById(R.id.editUsername);
        editCourse = findViewById(R.id.editCourse);
        editUni = findViewById(R.id.editUni);
        btnSave = findViewById(R.id.btnSaveProfile);

        // 1. CARREGAR DADOS (A tua funcionalidade pedida!)
        // Chamamos esta função assim que o ecrã abre
        carregarDadosAtuais();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarAlteracoes();
            }
        });
    }

    private void carregarDadosAtuais() {
        String uid = mAuth.getCurrentUser().getUid();

        // Bloqueamos o botão enquanto carrega para evitar erros
        btnSave.setEnabled(false);
        btnSave.setText("A carregar...");

        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                // --- AQUI PREENCHEMOS AS CAIXAS ---
                                editName.setText(user.getNome());
                                editUsername.setText(user.getUsername());
                                editCourse.setText(user.getCurso());
                                editUni.setText(user.getUniversidade());

                                // Guardamos o estado atual para comparar depois
                                usernameAtual = user.getUsername();
                                ultimaTrocaTimestamp = user.getUltimaTrocaUsername();

                                // Dados carregados? Botão pronto!
                                btnSave.setEnabled(true);
                                btnSave.setText("Guardar Alterações");
                            }
                        }
                    }
                });
    }

    private void guardarAlteracoes() {
        final String novoNome = editName.getText().toString().trim();
        final String novoUsername = editUsername.getText().toString().trim().replace(" ", "").toLowerCase();
        final String novoCurso = editCourse.getText().toString().trim();
        final String novaUni = editUni.getText().toString().trim();

        if (TextUtils.isEmpty(novoNome) || TextUtils.isEmpty(novoUsername) || TextUtils.isEmpty(novoCurso)) {
            Toast.makeText(this, "Preenche todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("A verificar...");

        // LÓGICA DE DECISÃO:

        // 1. Se o username é IGUAL ao antigo -> Não mudou, grava direto (ignora restrições)
        if (novoUsername.equals(usernameAtual)) {
            atualizarBaseDeDados(novoNome, novoUsername, novoCurso, novaUni, 0);
        }
        else {
            // 2. O username MUDOU -> Temos de verificar 2 coisas:

            // A. Regra dos 7 Dias
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

            // B. Unicidade (Será que já existe?)
            db.collection("users")
                    .whereEqualTo("username", novoUsername)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            // JÁ EXISTE! Erro.
                            editUsername.setError("Este username já está ocupado.");
                            btnSave.setEnabled(true);
                            btnSave.setText("Guardar Alterações");
                        } else {
                            // ESTÁ LIVRE! Gravar e atualizar data de troca.
                            atualizarBaseDeDados(novoNome, novoUsername, novoCurso, novaUni, agora);
                        }
                    });
        }
    }

    private void atualizarBaseDeDados(String nome, String username, String curso, String uni, long timestamp) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("nome", nome);
        updates.put("username", username);
        updates.put("curso", curso);
        updates.put("universidade", uni);

        if (timestamp > 0) {
            updates.put("ultimaTrocaUsername", timestamp);
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