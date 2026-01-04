package com.universe;

import static com.universe.NotificationType.FOLLOW;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    private List<Notification> list;
    private Context context;

    public NotificationAdapter(List<Notification> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        // Podes reutilizar o item_user.xml ou criar um item_notification.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new NotifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        Notification n = list.get(position);

        holder.txtMessage.setText(n.getMessage());
        holder.txtName.setText(n.getFromUserName()); // Quem enviou

        // Se não foi lida, muda a cor de fundo (exemplo)
        if (!n.isRead()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E8EAF6")); // Azul muito claro
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        // Foto de quem enviou
        if (n.getFromUserPhoto() != null && !n.getFromUserPhoto().isEmpty()) {
            Glide.with(context).load(n.getFromUserPhoto()).circleCrop().into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_person_filled);
        }

        // Clique: Se for like/comment vai para os comentários do post
        holder.itemView.setOnClickListener(v -> {
            if (n.getPostIdOptional().isPresent()) { // Adicionei verificação de isEmpty
                Intent intent = new Intent(context, CommentsActivity.class); // Ou PostDetailsActivity, tu decides
                intent.putExtra("postId", n.getPostId());
                context.startActivity(intent);
            }
            // Lógica para Seguir
            else if (FOLLOW.equals(n.getType())) { // Inverti para evitar NullPointerException se getType for null
                Intent intent = new Intent(context, PublicProfileActivity.class);

                // --- CORREÇÃO AQUI ---
                // Se a PublicProfileActivity espera "uid", usa "uid".
                // Se espera "targetUserId", mantém como estava.
                // Por coerência com a MainActivity, sugiro "uid":
                intent.putExtra("uid", n.getFromUserId());

                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class NotifViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtMessage;
        // Adiciona a referência ao botão (pode ser Button ou MaterialButton)
        View btnFollow;

        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.searchAvatar);
            txtName = itemView.findViewById(R.id.searchName);
            txtMessage = itemView.findViewById(R.id.searchUsername);

            // 1. Encontra o botão pelo ID (CONFIRMA O ID NO TEU XML item_user.xml)
            btnFollow = itemView.findViewById(R.id.btnSeguir);

            // 2. Esconde o botão imediatamente
            if (btnFollow != null) {
                btnFollow.setVisibility(View.GONE);
            }
        }
    }
}