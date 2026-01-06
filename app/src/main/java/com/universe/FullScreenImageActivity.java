package com.universe;

import android.net.Uri;
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

        ImageView fullScreenImage = findViewById(R.id.fullScreenImage);
        ImageButton btnClose = findViewById(R.id.btnCloseFullScreen);

        // Verifica se recebemos "imageUrl" (String do Firebase) ou "imageUri" (Uri local da Galeria)
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String imageUriString = getIntent().getStringExtra("imageUri");

        if (imageUrl != null) {
            // Carregar da Internet (Feed)
            Glide.with(this).load(imageUrl).into(fullScreenImage);
        } else if (imageUriString != null) {
            // Carregar Localmente (CreatePost)
            Uri uri = Uri.parse(imageUriString);
            Glide.with(this).load(uri).into(fullScreenImage);
        }

        btnClose.setOnClickListener(v -> finish());
    }
}