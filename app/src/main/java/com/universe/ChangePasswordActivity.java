package com.universe;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText inputCurrent, inputNew, inputConfirm;
    private Button btnSave;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        inputCurrent = findViewById(R.id.inputCurrentPass);
        inputNew = findViewById(R.id.inputNewPass);
        inputConfirm = findViewById(R.id.inputConfirmNewPass);
        btnSave = findViewById(R.id.btnSavePassword);

        user = FirebaseAuth.getInstance().getCurrentUser();

        btnSave.setOnClickListener(v -> tentarAlterarPassword());
    }

    private void tentarAlterarPassword() {
        String currentPass = inputCurrent.getText().toString();
        String newPass = inputNew.getText().toString();
        String confirmPass = inputConfirm.getText().toString();

        // 1. Validações Básicas
        if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "Preenche todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPass.length() < 6) {
            inputNew.setError("Mínimo 6 caracteres");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            inputConfirm.setError("As passwords não coincidem");
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("A verificar...");

        // 2. REAUTENTICAÇÃO (O Passo de Segurança)
        // Criamos uma credencial com o email atual e a password ANTIGA que o user escreveu
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

        user.reauthenticate(credential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // Se chegou aqui, a password antiga está CERTA!
                        // Agora podemos mudar para a nova.
                        atualizarParaNova(newPass);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // A password antiga está ERRADA
                        btnSave.setEnabled(true);
                        btnSave.setText("Atualizar Password");
                        inputCurrent.setError("Password incorreta");
                        Toast.makeText(ChangePasswordActivity.this, "Erro de autenticação", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void atualizarParaNova(String newPass) {
        btnSave.setText("A atualizar...");

        user.updatePassword(newPass)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ChangePasswordActivity.this, "Password alterada com sucesso!", Toast.LENGTH_LONG).show();
                        finish(); // Fecha a janela
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Atualizar Password");
                        Toast.makeText(ChangePasswordActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}