package com.universe;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout inputEmail, inputPassword;
    private Button btnLogin;
    private TextView textGoToRegister, textForgotPass;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // --- VERIFICAÇÃO AUTOMÁTICA DE SESSÃO ---
        // Se o utilizador já estiver logado, não precisa de fazer login outra vez.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            goToMain();
        }

        // Ligar UI
        inputEmail = findViewById(R.id.loginInputEmail);
        inputPassword = findViewById(R.id.loginInputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        textGoToRegister = findViewById(R.id.textGoToRegister);
        textForgotPass = findViewById(R.id.textForgotPass);
        progressBar = findViewById(R.id.loginProgressBar);

        // Texto sublinhado
        TextView textGoToRegister = findViewById(R.id.textGoToRegister);
        TextView textForgotPass = findViewById(R.id.textForgotPass);

        textGoToRegister.setPaintFlags(textGoToRegister.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        textForgotPass.setPaintFlags(textForgotPass.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Clique para ir para o Registo
        textGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        // Clique para fazer Login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fazerLogin();
            }
        });

        // (Forgot Password
        textForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, "Implementar lógica de recuperar password", Toast.LENGTH_SHORT).show();
                // Dica: mAuth.sendPasswordResetEmail(email)...
            }
        });
    }

    private void fazerLogin() {
        String email = inputEmail.getEditText().getText().toString().trim();
        String password = inputPassword.getEditText().getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("Introduz o teu email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Introduz a password");
            return;
        }

        // Limpar erros e mostrar loading
        inputEmail.setError(null);
        inputPassword.setError(null);
        progressBar.setVisibility(View.VISIBLE);

        // Autenticar no Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            // Login com sucesso
                            goToMain();
                        } else {
                            // Falha no login
                            Toast.makeText(LoginActivity.this, "Falha ao entrar. Verifica o email e password.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Estas flags limpam o histórico para que, se o utilizador clicar "Voltar" na Home, não volte ao Login
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}