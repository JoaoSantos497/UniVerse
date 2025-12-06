package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        ImageButton btnBack = findViewById(R.id.btnBackSecurity);
        LinearLayout btnChangePass = findViewById(R.id.btnOpenChangePassword);

        btnBack.setOnClickListener(v -> finish());

        // Clicar em "Alterar Password" abre o ecrã final
        btnChangePass.setOnClickListener(v -> {
            Intent intent = new Intent(SecurityActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });
    }
}