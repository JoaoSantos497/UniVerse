package com.universe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Limpeza de cache (opcional)
        FirebaseFirestore.getInstance().clearPersistence().addOnSuccessListener(unused -> {});

        verificarEstadoConta();
        verificarPermissaoNotificacao();
        atualizarTokenFCM();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (itemId == R.id.nav_search) selectedFragment = new SearchFragment();
            else if (itemId == R.id.nav_profile) selectedFragment = new ProfileFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment).commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment()).commit();
        }

        // --- LÓGICA DE NOTIFICAÇÃO ---
        checkNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkNotificationIntent(intent);
    }

    private void checkNotificationIntent(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            String type = intent.getStringExtra("type");
            // ATENÇÃO: Tem de ser "fromUserId" para bater certo com o index.js
            String userId = intent.getStringExtra("fromUserId");
            String postId = intent.getStringExtra("postId");

            if (type != null) {
                Log.d("NOTIF", "Tipo: " + type + ", User: " + userId + ", Post: " + postId);

                switch (type) {
                    case "follow":
                        if (userId != null) {
                            // Abre perfil de outra pessoa
                            Intent profileIntent = new Intent(this, PublicProfileActivity.class);
                            profileIntent.putExtra("uid", userId);
                            startActivity(profileIntent);
                        }
                        break;

                    case "like":
                    case "comment":
                    case "new_post":
                        if (postId != null && !postId.isEmpty()) {
                            // Abre detalhes do post
                            Intent postIntent = new Intent(this, PostDetailsActivity.class);
                            postIntent.putExtra("postId", postId);
                            startActivity(postIntent);
                        }
                        break;
                }
            }
        }
    }

    // --- MÉTODOS AUXILIARES ---
    private void verificarEstadoConta() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.reload().addOnFailureListener(e -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void verificarPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notificações ativadas!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void atualizarTokenFCM() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid).update("fcmToken", token);
        });
    }
}