package com.universe;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton; // <--- Importante
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText inputEmail;
    private Button btnSend;
    private ImageButton btnBack;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        inputEmail = findViewById(R.id.inputEmailRecovery);
        btnSend = findViewById(R.id.btnSendEmail);
        btnBack = findViewById(R.id.btnBackForgot);

        // Voltar atrás
        btnBack.setOnClickListener(v -> finish());

        // Enviar Email
        btnSend.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                inputEmail.setError("Escreve o teu email");
                return;
            }

            // Desativar botão para não clicar várias vezes
            btnSend.setEnabled(false);
            btnSend.setText("A enviar...");

            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(ForgotPasswordActivity.this, "Email enviado! Verifica a tua caixa de correio.", Toast.LENGTH_LONG).show();
                        finish(); // Volta para o Login
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ForgotPasswordActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSend.setEnabled(true);
                        btnSend.setText("Enviar Email");
                    });
        });
    }
}