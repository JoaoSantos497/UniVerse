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
import androidx.appcompat.app.AppCompatDelegate;
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

        // --- 1. VERIFICAÇÕES DE SEGURANÇA E NOTIFICAÇÕES ---
        verificarEstadoConta(); // Verifica se a conta ainda existe
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

    /**
     * Tenta recarregar o perfil do utilizador. Se falhar (user-not-found),
     * significa que a conta foi apagada no console e força o logout.
     */
    private void verificarEstadoConta() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.reload().addOnFailureListener(e -> {
                Log.d("AUTH", "Sessão inválida ou conta eliminada. Redirecionando...");
                FirebaseAuth.getInstance().signOut();

                // Abre o Login e limpa o histórico de ecrãs
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