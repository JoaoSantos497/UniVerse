package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Inicializar o Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ligar componentes
        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        LinearLayout btnSecurity = findViewById(R.id.btnOpenSecurity);
        LinearLayout btnBlockedUsers = findViewById(R.id.btnBlockedUsers); // <--- NOVO: Botão Bloqueados
        Button btnLogout = findViewById(R.id.btnLogout);

        // 1. Botão Voltar
        btnBack.setOnClickListener(v -> finish());

        // 2. Botão Segurança (ChangePasswordActivity)
        btnSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // 3. Botão Utilizadores Bloqueados (BlockedUsersActivity)
        // (Certifica-te que criaste a BlockedUsersActivity como falámos antes)
        btnBlockedUsers.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, BlockedUsersActivity.class);
            startActivity(intent);
        });

        // 4. Botão Logout
        btnLogout.setOnClickListener(v -> {
            // Sair do Firebase
            mAuth.signOut();

            // Voltar ao Login e limpar o histórico
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}