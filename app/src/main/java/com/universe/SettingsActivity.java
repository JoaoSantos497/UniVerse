package com.universe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog; // Importante para o menu de escolha
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    // Variáveis para o Tema
    private TextView txtThemeStatus;
    private int selectedThemeOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Inicializar o Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ligar componentes
        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        LinearLayout btnSecurity = findViewById(R.id.btnOpenSecurity);
        LinearLayout btnBlockedUsers = findViewById(R.id.btnBlockedUsers);

        // Novos componentes do Tema
        LinearLayout btnTheme = findViewById(R.id.btnTheme);
        txtThemeStatus = findViewById(R.id.txtThemeStatus);

        Button btnLogout = findViewById(R.id.btnLogout);

        // --- CARREGAR O TEMA ATUAL (Para mostrar o texto correto) ---
        carregarEstadoTema();

        // 1. Botão Voltar
        btnBack.setOnClickListener(v -> finish());

        // 2. Botão Segurança
        btnSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // 3. Botão Bloqueados
        btnBlockedUsers.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, BlockedUsersActivity.class);
            startActivity(intent);
        });

        // 4. NOVO: Botão Tema
        btnTheme.setOnClickListener(v -> mostrarDialogoTema());

        // 5. Botão Logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // --- MÉTODOS AUXILIARES DO TEMA ---

    private void carregarEstadoTema() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        // 0 é o padrão se não houver nada gravado
        selectedThemeOption = prefs.getInt("theme_mode", 0);
        atualizarTextoTema(selectedThemeOption);
    }

    private void mostrarDialogoTema() {
        String[] themes = {"Predefinição do Sistema", "Modo Claro", "Modo Escuro"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Escolher Tema");

        builder.setSingleChoiceItems(themes, selectedThemeOption, (dialog, which) -> {
            selectedThemeOption = which;

            // 1. Guardar a preferência
            SharedPreferences.Editor editor = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit();
            editor.putInt("theme_mode", selectedThemeOption);
            editor.apply();

            // 2. Aplicar o tema na app
            UniVerseApp.aplicarTema(selectedThemeOption);

            // 3. O SEGREDO: Reiniciar a app para a Home para forçar a mudança de cor
            dialog.dismiss();

            // Pequeno delay para garantir que a preferência foi gravada antes de reiniciar
            new android.os.Handler().postDelayed(() -> {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                // Limpa o histórico de atividades
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Fecha as definições
            }, 200);
        });
        builder.show();
    }

    private void atualizarTextoTema(int mode) {
        switch (mode) {
            case 1:
                txtThemeStatus.setText("Claro");
                break;
            case 2:
                txtThemeStatus.setText("Escuro");
                break;
            default:
                txtThemeStatus.setText("Sistema");
                break;
        }
    }
}