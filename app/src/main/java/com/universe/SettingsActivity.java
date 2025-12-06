package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        LinearLayout btnSecurity = findViewById(R.id.btnOpenSecurity);

        btnBack.setOnClickListener(v -> finish());

        // Clicar em "Segurança" -> Abre o menu de Segurança
        btnSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, SecurityActivity.class);
            startActivity(intent);
        });
    }
}