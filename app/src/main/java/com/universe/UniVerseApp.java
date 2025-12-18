package com.universe;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class UniVerseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Ler a preferência guardada assim que a app arranca
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // 0 = Sistema, 1 = Claro, 2 = Escuro
        int themeMode = sharedPreferences.getInt("theme_mode", 0);

        // 2. Aplicar o tema antes de abrir qualquer ecrã
        aplicarTema(themeMode);
    }

    // Metodo estático para ser chamado também nas Definições
    public static void aplicarTema(int mode) {
        switch (mode) {
            case 1:
                // Forçar Modo Claro
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                // Forçar Modo Escuro
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                // Seguir o Sistema (Padrão)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}