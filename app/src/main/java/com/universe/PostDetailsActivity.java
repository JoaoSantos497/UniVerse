package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PostDetailsActivity extends AppCompatActivity {

    private ImageView imgProfile, imgPost;
    private TextView txtUsername, txtDescription;
    private CardView cardPostImage;
    private FirebaseFirestore db;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        imgProfile = findViewById(R.id.imgProfile);
        imgPost = findViewById(R.id.imgPost);
        txtUsername = findViewById(R.id.txtUsername);
        txtDescription = findViewById(R.id.txtDescription);
        cardPostImage = findViewById(R.id.cardPostImage);
        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnOpenComments = findViewById(R.id.btnOpenComments);

        db = FirebaseFirestore.getInstance();
        postId = getIntent().getStringExtra("postId");

        btnBack.setOnClickListener(v -> finish());

        btnOpenComments.setOnClickListener(v -> {
            if (postId != null) {
                Intent intent = new Intent(this, CommentsActivity.class);
                intent.putExtra("postId", postId);
                startActivity(intent);
            }
        });

        if (postId != null) carregarPost(postId);
        else finish();
    }

    private void carregarPost(String id) {
        db.collection("posts").document(id).get().addOnSuccessListener(doc -> {
            if (doc.exists()) preencherInterface(doc);
            else Toast.makeText(this, "Publicação não encontrada.", Toast.LENGTH_SHORT).show();
        });
    }

    private void preencherInterface(DocumentSnapshot doc) {
        String username = doc.getString("userName");
        String userPhoto = doc.getString("userPhotoUrl");
        String description = doc.getString("description");
        String postImageUrl = doc.getString("postImageUrl");

        txtUsername.setText(username != null ? username : "Utilizador");

        if (description != null && !description.isEmpty()) {
            txtDescription.setText(description);
            txtDescription.setVisibility(View.VISIBLE);
        } else {
            txtDescription.setVisibility(View.GONE);
        }

        if (userPhoto != null) Glide.with(this).load(userPhoto).into(imgProfile);

        if (postImageUrl != null && !postImageUrl.isEmpty()) {
            cardPostImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(postImageUrl).into(imgPost);
        } else {
            cardPostImage.setVisibility(View.GONE);
        }
    }
}