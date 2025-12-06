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

    // Dados atuais (para comparação)
    private String usernameAtual;
    private long ultimaTrocaTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editName = findViewById(R.id.editName);
        editUsername = findViewById(R.id.editUsername);
        editCourse = findViewById(R.id.editCourse);
        editUni = findViewById(R.id.editUni);
        btnSave = findViewById(R.id.btnSaveProfile);

        // Carregar dados frescos da BD (melhor que vir do Intent para termos o timestamp certo)
        carregarDadosAtuais();

        btnSave.setOnClickListener(v -> guardarAlteracoes());
    }

    private void carregarDadosAtuais() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            editName.setText(user.getNome());
                            editCourse.setText(user.getCurso());
                            editUni.setText(user.getUniversidade());

                            // Guardamos o username atual e o tempo para comparar depois
                            editUsername.setText(user.getUsername());
                            usernameAtual = user.getUsername();
                            ultimaTrocaTimestamp = user.getUltimaTrocaUsername();
                        }
                    }
                });
    }

    private void guardarAlteracoes() {
        String novoNome = editName.getText().toString().trim();
        String novoUsername = editUsername.getText().toString().trim().replace(" ", "").toLowerCase();
        String novoCurso = editCourse.getText().toString().trim();
        String novaUni = editUni.getText().toString().trim();

        if (TextUtils.isEmpty(novoNome) || TextUtils.isEmpty(novoUsername) || TextUtils.isEmpty(novoCurso)) {
            Toast.makeText(this, "Preenche todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("A verificar...");

        // Se o username NÃO mudou, gravamos direto
        if (novoUsername.equals(usernameAtual)) {
            atualizarDadosNaBase(novoNome, novoUsername, novoCurso, novaUni, 0); // 0 significa que não atualizamos o tempo
        }
        else {
            // Se mudou, temos de verificar DUAS coisas:
            // 1. A regra dos 7 dias
            long agora = System.currentTimeMillis();
            long diferenca = agora - ultimaTrocaTimestamp;
            long seteDiasEmMs = TimeUnit.DAYS.toMillis(7);

            if (ultimaTrocaTimestamp != 0 && diferenca < seteDiasEmMs) {
                long diasRestantes = TimeUnit.MILLISECONDS.toDays(seteDiasEmMs - diferenca);
                editUsername.setError("Espera " + (diasRestantes + 1) + " dias para mudar de novo.");
                btnSave.setEnabled(true);
                btnSave.setText("Guardar Alterações");
                return;
            }

            // 2. Se o username já existe (Verificação de Unicidade)
            db.collection("users")
                    .whereEqualTo("username", novoUsername)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            // Já existe!
                            editUsername.setError("Este username já existe.");
                            btnSave.setEnabled(true);
                            btnSave.setText("Guardar Alterações");
                        } else {
                            // Está livre! Gravar e atualizar timestamp
                            atualizarDadosNaBase(novoNome, novoUsername, novoCurso, novaUni, agora);
                        }
                    });
        }
    }

    // Função auxiliar para não repetir código
    private void atualizarDadosNaBase(String nome, String username, String curso, String uni, long timestampTroca) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("nome", nome);
        updates.put("username", username);
        updates.put("curso", curso);
        updates.put("universidade", uni);

        // Só atualiza a data se tiver havido troca (timestamp > 0)
        if (timestampTroca > 0) {
            updates.put("ultimaTrocaUsername", timestampTroca);
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