package com.universe;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private String myUid;
    private FirebaseFirestore db;
    private boolean mostrarBotaoSeguir;

    public UserAdapter(List<User> userList, boolean mostrarBotaoSeguir) {
        this.userList = userList;
        this.mostrarBotaoSeguir = mostrarBotaoSeguir;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        this.db = FirebaseFirestore.getInstance();
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

        holder.txtName.setText(user.getNome() != null ? user.getNome() : "Utilizador");
        holder.txtUsername.setText(user.getUsername() != null ? "@" + user.getUsername() : "");

        Glide.with(context)
                .load(user.getPhotoUrl())
                .circleCrop()
                .placeholder(R.drawable.circle_bg)
                .into(holder.imgAvatar);

        // Lógica de Visibilidade: Esconde o botão se for o meu próprio perfil ou se desativado
        if (!mostrarBotaoSeguir || targetUid.equals(myUid)) {
            holder.btnSeguir.setVisibility(View.GONE);
        } else {
            holder.btnSeguir.setVisibility(View.VISIBLE);
            holder.btnSeguir.setEnabled(false); // Bloqueia até verificar estado
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

        DocumentReference refFollowing = db.collection("users").document(myUid).collection("following").document(tUid);
        DocumentReference refFollower = db.collection("users").document(tUid).collection("followers").document(myUid);
        DocumentReference refMe = db.collection("users").document(myUid);
        DocumentReference refTarget = db.collection("users").document(tUid);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", tUid);
        data.put("timestamp", FieldValue.serverTimestamp());

        Map<String, Object> dataFollower = new HashMap<>();
        dataFollower.put("uid", myUid);
        dataFollower.put("timestamp", FieldValue.serverTimestamp());

        batch.set(refFollowing, data);
        batch.set(refFollower, dataFollower);

        batch.update(refMe, "followingCount", FieldValue.increment(1));
        batch.update(refTarget, "followersCount", FieldValue.increment(1));

        // Notificação
        DocumentReference notifRef = db.collection("notifications").document();
        Map<String, Object> notif = new HashMap<>();
        notif.put("targetUserId", tUid);
        notif.put("fromUserId", myUid);
        notif.put("message", "começou a seguir-te!");
        notif.put("type", "follow");
        notif.put("timestamp", FieldValue.serverTimestamp());
        batch.set(notifRef, notif);

        batch.commit().addOnSuccessListener(aVoid -> {
            configurarBotaoSeguindo(btn);
            btn.setEnabled(true);
        }).addOnFailureListener(e -> btn.setEnabled(true));
    }

    private void executarUnfollow(String tUid, MaterialButton btn) {
        WriteBatch batch = db.batch();

        DocumentReference refFollowing = db.collection("users").document(myUid).collection("following").document(tUid);
        DocumentReference refFollower = db.collection("users").document(tUid).collection("followers").document(myUid);
        DocumentReference refMe = db.collection("users").document(myUid);
        DocumentReference refTarget = db.collection("users").document(tUid);

        batch.delete(refFollowing);
        batch.delete(refFollower);

        batch.update(refMe, "followingCount", FieldValue.increment(-1));
        batch.update(refTarget, "followersCount", FieldValue.increment(-1));

        batch.commit().addOnSuccessListener(aVoid -> {
            configurarBotaoSeguir(btn);
            btn.setEnabled(true);
        }).addOnFailureListener(e -> btn.setEnabled(true));
    }

    @Override
    public int getItemCount() { return userList.size(); }

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