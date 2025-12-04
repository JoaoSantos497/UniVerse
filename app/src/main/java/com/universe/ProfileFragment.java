package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    // Variáveis da UI
    private TextView profileEmail, profileName, profileCurso, profileUni;
    private Button btnLogout;

    // Variáveis do Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Insuflar o layout
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 2. Inicializar Firebase (Auth e Firestore)
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();

        // 3. Ligar componentes da UI aos IDs do XML
        // NOTA: Certifica-te que tens estes IDs no teu XML (vê a nota em baixo)
        profileEmail = view.findViewById(R.id.profileEmail);
        profileName = view.findViewById(R.id.profileName);   // Novo ID
        profileCurso = view.findViewById(R.id.profileCurso); // Novo ID
        profileUni = view.findViewById(R.id.profileUni);     // Novo ID
        btnLogout = view.findViewById(R.id.btnLogout);

        // 4. Se o utilizador estiver logado, carregar dados
        if (user != null) {
            // Mostrar Email (vem do Auth)
            profileEmail.setText(user.getEmail());

            // Carregar dados extra (vem do Firestore)
            carregarDadosDoFirestore(user.getUid());
        }

        // 5. Lógica do Botão Logout
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        return view;
    }

    // Função auxiliar para ler da Base de Dados
    private void carregarDadosDoFirestore(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Converte o documento JSON para a nossa classe Java 'User'
                            User aluno = documentSnapshot.toObject(User.class);

                            if (aluno != null) {
                                // Define os textos na Ecrã
                                profileName.setText(aluno.getNome());
                                profileCurso.setText(aluno.getCurso());
                                profileUni.setText(aluno.getUniversidade());
                            }
                        } else {
                            Toast.makeText(getContext(), "Perfil não encontrado na base de dados.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Erro ao carregar perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}