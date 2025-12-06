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
    private TextView textGoToRegister, tvForgot; // Usei tvForgot para ser consistente
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // --- VERIFICAÇÃO AUTOMÁTICA DE SESSÃO ---
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            goToMain();
        }

        // Ligar UI aos IDs do XML
        inputEmail = findViewById(R.id.loginInputEmail);
        inputPassword = findViewById(R.id.loginInputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.loginProgressBar);

        textGoToRegister = findViewById(R.id.textGoToRegister);
        tvForgot = findViewById(R.id.tvForgotPassword);

        // Sublinhar os textos (Efeito visual de link)
        textGoToRegister.setPaintFlags(textGoToRegister.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        // Verificação de segurança: só sublinha se o botão existir
        if (tvForgot != null) {
            tvForgot.setPaintFlags(tvForgot.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

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

        // Clique para recuperar password (O código novo!)
        if (tvForgot != null) {
            tvForgot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    private void fazerLogin() {
        // (Este método mantém-se igual ao que tinhas, está correto)
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

        inputEmail.setError(null);
        inputPassword.setError(null);
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            goToMain();
                        } else {
                            Toast.makeText(LoginActivity.this, "Falha ao entrar. Verifica dados.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}