package com.universe;

import static com.universe.NotificationType.FOLLOW;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private String myUid;
    private FirebaseFirestore db;
    private boolean mostrarBotaoSeguir;
    private Set<String> bloqueadosIds = new HashSet<>();

    private UserService userService;

    private NotificationService notificationService;



    public UserAdapter(List<User> userList, boolean mostrarBotaoSeguir) {
        this.userList = userList;
        this.mostrarBotaoSeguir = mostrarBotaoSeguir;
        this.userService = new UserService();
        this.notificationService = new NotificationService();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            this.db = FirebaseFirestore.getInstance();
            // Inicia a escuta ativa da lista de bloqueados
            carregarBloqueados();
        }
    }



    private void carregarBloqueados() {
        if (myUid == null) return;

        db.collection("users").document(myUid).collection("blocked")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        bloqueadosIds.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            bloqueadosIds.add(doc.getId());
                        }
                        // Notifica o adapter para esconder utilizadores recém-bloqueados
                        notifyDataSetChanged();
                    }
                });
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        String targetUid = user.getUid();

        if (targetUid == null) return;
        if (bloqueadosIds.contains(targetUid)) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            holder.itemView.setLayoutParams(params);
        }

        holder.txtName.setText(user.getNome() != null ? user.getNome() : "Utilizador");
        holder.txtUsername.setText(user.getUsername() != null ? "@" + user.getUsername() : "");

        Glide.with(context)
                .load(user.getPhotoUrl())
                .circleCrop()
                .placeholder(R.drawable.circle_bg)
                .into(holder.imgAvatar);

        // --- LÓGICA DO BOTÃO SEGUIR ATUALIZADA ---
        if (!mostrarBotaoSeguir || targetUid.equals(myUid)) {
            // Esconde o botão se a flag for falsa OU se o perfil na lista for o meu
            holder.btnSeguir.setVisibility(View.GONE);
        } else {
            holder.btnSeguir.setVisibility(View.VISIBLE);
            holder.btnSeguir.setEnabled(false);
            verificarSeSigo(targetUid, (MaterialButton) holder.btnSeguir);
        }

        holder.btnSeguir.setOnClickListener(v -> alternarFollow(user, (MaterialButton) holder.btnSeguir));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PublicProfileActivity.class);
            intent.putExtra("targetUserId", targetUid);
            context.startActivity(intent);
        });
    }

    private void verificarSeSigo(String targetUid, MaterialButton btn) {
        db.collection("users").document(myUid)
                .collection("following").document(targetUid)
                .get().addOnCompleteListener(task -> {
                    btn.setEnabled(true);
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        configurarBotaoSeguindo(btn);
                    } else {
                        configurarBotaoSeguir(btn);
                    }
                });
    }

    private void configurarBotaoSeguindo(MaterialButton btn) {
        btn.setText("Seguindo");
        btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.darker_gray)));
        btn.setTextColor(ContextCompat.getColor(context, android.R.color.white));
    }

    private void configurarBotaoSeguir(MaterialButton btn) {
        btn.setText("Seguir");
        btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.purple_500)));
        btn.setTextColor(ContextCompat.getColor(context, android.R.color.white));
    }

    private void alternarFollow(User targetUser, MaterialButton btn) {
        btn.setEnabled(false);
        String targetUid = targetUser.getUid();

        db.collection("users").document(myUid).collection("following").document(targetUid)
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        executarUnfollow(targetUid, btn);
                    } else {
                        executarFollow(targetUser, btn);
                    }
                }).addOnFailureListener(e -> btn.setEnabled(true));
    }

    private void executarFollow(User targetUser, MaterialButton btn) {
        WriteBatch batch = db.batch();
        String tUid = targetUser.getUid();
        batch = userService.followUser(batch, tUid);
        batch = notificationService.sendNotification(batch, tUid, FOLLOW);

        batch.commit().addOnSuccessListener(aVoid -> {
            configurarBotaoSeguindo(btn);
            btn.setEnabled(true);
        }).addOnFailureListener(e -> btn.setEnabled(true));
    }

    private void executarUnfollow(String tUid, MaterialButton btn) {
        WriteBatch batch = db.batch();

        batch = userService.unfollowUser(batch, tUid);

        batch.commit().addOnSuccessListener(aVoid -> {
            configurarBotaoSeguir(btn);
            btn.setEnabled(true);
        }).addOnFailureListener(e -> btn.setEnabled(true));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtUsername;
        ImageView imgAvatar;
        Button btnSeguir;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.searchName);
            txtUsername = itemView.findViewById(R.id.searchUsername);
            imgAvatar = itemView.findViewById(R.id.searchAvatar);
            btnSeguir = itemView.findViewById(R.id.btnSeguir);
        }
    }
}