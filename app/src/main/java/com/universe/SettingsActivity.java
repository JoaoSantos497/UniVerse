package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // <--- Faltava este import
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth; // <--- Faltava este import

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth; // <--- Variável para controlar o Logout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Inicializar o Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ligar componentes
        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        LinearLayout btnSecurity = findViewById(R.id.btnOpenSecurity);
        Button btnLogout = findViewById(R.id.btnLogout); // <--- Faltava ligar este botão

        // Botão Voltar
        btnBack.setOnClickListener(v -> finish());

        // Botão Segurança (Abre o alterar password)
        btnSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // --- AQUI ESTÁ A PARTE QUE FALTAVA (LOGOUT) ---
        btnLogout.setOnClickListener(v -> {
            // 1. Sair do Firebase
            mAuth.signOut();

            // 2. Voltar ao Login e limpar o histórico (para não dar para voltar atrás)
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}