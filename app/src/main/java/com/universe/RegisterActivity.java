package com.universe;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    // Variáveis da UI
    private TextInputLayout inputEmail, inputPassword, inputConfirmPassword, inputNome, inputCurso, inputUsername;
    private Button btnRegistar;
    private ProgressBar progressBar;

    // Variáveis do Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Inicializar Base de Dados

        // Ligar variáveis aos IDs do XML
        inputNome = findViewById(R.id.textInputLayoutName);
        inputUsername = findViewById(R.id.inputUsername);
        inputCurso = findViewById(R.id.textInputLayoutCourse);

        inputEmail = findViewById(R.id.textInputLayoutEmail);
        inputPassword = findViewById(R.id.textInputLayoutPassword);
        inputConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword);

        btnRegistar = findViewById(R.id.buttonRegistar);
        progressBar = findViewById(R.id.progressBar);

        // Texto sublinhado
        TextView textViewLoginLink = findViewById(R.id.textViewLoginLink);
        textViewLoginLink.setPaintFlags(textViewLoginLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);


        // Ação do Botão Registar
        btnRegistar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                criarConta();
            }
        });

        // Ligar a variável do link de login
        TextView textGoToLogin = findViewById(R.id.textViewLoginLink);

        // Dar vida ao botão (Clicar e ir para o Login)
        textGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish(); // Fecha o registo para poupar memória
            }
        });
    }

    private void criarConta() {
        inputNome.setError(null); // Limpa erro do Nome
        inputUsername.setError(null);// Limpa erro do Username
        inputCurso.setError(null);   // Limpa erro do Curso
        inputEmail.setError(null);   // Limpa erro do Email
        inputPassword.setError(null); // Limpa erro da Password
        inputConfirmPassword.setError(null);
        progressBar.setVisibility(View.VISIBLE);


        // 1. Obter os textos escritos
        final String nome = inputNome.getEditText().getText().toString().trim();
        final String username = inputUsername.getEditText().getText().toString().trim().replace(" ", "").toLowerCase();
        final String curso = inputCurso.getEditText().getText().toString().trim();
        final String email = inputEmail.getEditText().getText().toString().trim();
        String password = inputPassword.getEditText().getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getEditText().getText().toString().trim();

        // 2. Validações de Segurança
        if (TextUtils.isEmpty(nome)) {
            inputNome.setError("Introduz o teu nome");
            return;
        }
        if (TextUtils.isEmpty(username)) {
            inputUsername.setError("Define um username");
            return;
        }
        if (TextUtils.isEmpty(curso)) {
            inputCurso.setError("Introduz o teu curso");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("O email é obrigatório");
            return;
        }
        // Validação do Email Institucional
        // Verificamos se termina em "ipsantarem.pt" para aceitar TODAS as escolas (esg, eses, esa, ess, esdrm)
        if (!email.endsWith("ipsantarem.pt")) {
            inputEmail.setError("Acesso restrito: Usa o teu email do IPSantarém!");
            inputEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            inputPassword.setError("A password deve ter pelo menos 6 caracteres");
            return;
        }
        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("As passwords não coincidem");
            return;
        }

        // Limpar erros e mostrar loading
        inputEmail.setError(null);
        inputPassword.setError(null);
        progressBar.setVisibility(View.VISIBLE);

        // 3. Criar utilizador no Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // A conta foi criada, AGORA guardamos os dados na BD
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            guardarDadosNaBaseDeDados(firebaseUser);

                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(RegisterActivity.this, "Erro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Metodo separado para organizar o código
    private void guardarDadosNaBaseDeDados(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        String email = inputEmail.getEditText().getText().toString().trim();
        String nome = inputNome.getEditText().getText().toString().trim();
        String curso = inputCurso.getEditText().getText().toString().trim();
        String username = inputUsername.getEditText().getText().toString().trim().replace(" ", "").toLowerCase();

        // 1. AQUI ESTÁ A MAGIA: Descobrimos a escola automaticamente
        String universidade = obterNomeEscola(email);

        // 2. Criamos o objeto User com a universidade correta (em vez de texto fixo)
        User user = new User(uid, nome, username, email, curso, universidade);

        // 3. Guardar no Firestore (o resto do teu código continua igual)
        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(RegisterActivity.this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();
                        // Redirecionar para o Login ou Main...
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RegisterActivity.this, "Erro ao guardar dados: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Função auxiliar para detetar a escola com base no domínio do email
    private String obterNomeEscola(String email) {
        if (email.contains("@esg.")) {
            return "Escola Superior de Gestão e Tecnologia";
        } else if (email.contains("@eses.")) {
            return "Escola Superior de Educação";
        } else if (email.contains("@esa.")) {
            return "Escola Superior Agrária";
        } else if (email.contains("@ess.")) {
            return "Escola Superior de Saúde";
        } else if (email.contains("@esdrm.")) {
            return "Escola Superior de Desporto";
        } else {
            // Caso seja apenas @ipsantarem.pt ou outro desconhecido
            return "Politécnico de Santarém";
        }
    }
}