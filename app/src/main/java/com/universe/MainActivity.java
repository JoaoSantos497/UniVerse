package com.universe;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Forçar Dark Mode a seguir o sistema
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        setContentView(R.layout.activity_main);

        // --- 1. PEDIR PERMISSÃO (Android 13+) E ATUALIZAR TOKEN ---
        verificarPermissaoNotificacao();
        atualizarTokenFCM();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private void verificarPermissaoNotificacao() {
        // No Android 13 (API 33) ou superior, o pop-up só aparece se o user permitir
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notificações ativadas!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Não receberás pop-ups de notificações.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void atualizarTokenFCM() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("fcmToken", token);
            }
        });
    }
}