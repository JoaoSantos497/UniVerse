package com.universe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BlockedUserAdapter extends RecyclerView.Adapter<BlockedUserAdapter.ViewHolder> {

    private List<User> users;
    private Context context;
    private FirebaseFirestore db;
    private String currentUserId;

    public BlockedUserAdapter(List<User> users) {
        this.users = users;
        this.db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.name.setText(user.getNome() != null ? user.getNome() : "Utilizador");

        Glide.with(context)
                .load(user.getPhotoUrl())
                .circleCrop()
                .placeholder(R.drawable.circle_bg)
                .into(holder.image);

        holder.btnUnblock.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            db.collection("users").document(currentUserId)
                    .collection("blocked").document(user.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Desbloqueado!", Toast.LENGTH_SHORT).show();
                        users.remove(currentPos);
                        notifyItemRemoved(currentPos);
                        // Importante para atualizar as posições internas do RecyclerView
                        notifyItemRangeChanged(currentPos, users.size());
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Erro ao desbloquear", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() { return users.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        Button btnUnblock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.blockedUserImage);
            name = itemView.findViewById(R.id.blockedUserName);
            btnUnblock = itemView.findViewById(R.id.btnUnblock);
        }
    }
}