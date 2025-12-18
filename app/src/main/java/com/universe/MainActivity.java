package com.universe;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Forçar Dark Mode a seguir o sistema
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        setContentView(R.layout.activity_main);

        // --- 1. CHAMAR A ATUALIZAÇÃO DO TOKEN ---
        atualizarTokenFCM();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Bottom Navigation Menu
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

        // Carregar a Home quando a app abre
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }


    private void atualizarTokenFCM() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (uid != null) {
                // Guardamos o token no documento do utilizador no Firestore
                FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("fcmToken", token);
            }
        });
    }
}