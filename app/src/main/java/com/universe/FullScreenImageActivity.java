package com.universe;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView imageView = findViewById(R.id.fullScreenImage);
        ImageButton btnClose = findViewById(R.id.btnCloseFullScreen);

        // 1. Receber o URL da imagem (passado pelo Adapter)
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // 2. Carregar a imagem
        if (imageUrl != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .fitCenter() // Ajusta para caber no ecrã sem cortar
                    .into(imageView);
        }

        // 3. Botão Fechar
        btnClose.setOnClickListener(v -> finish());
    }
}