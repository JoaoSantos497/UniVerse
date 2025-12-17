package com.universe;

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
            holder.imgAvatar.setImageResource(R.drawable.circle_bg);
        }

        // Clique: Se for like/comment vai para os comentários do post
        holder.itemView.setOnClickListener(v -> {
            if (n.getPostId() != null) {
                Intent intent = new Intent(context, CommentsActivity.class);
                intent.putExtra("postId", n.getPostId());
                context.startActivity(intent);
            } else if (n.getType().equals("follow")) {
                Intent intent = new Intent(context, PublicProfileActivity.class);
                intent.putExtra("targetUserId", n.getFromUserId());
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
        TextView txtName, txtMessage; // Reutilizando IDs do item_user

        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.searchAvatar);
            txtName = itemView.findViewById(R.id.searchName);
            txtMessage = itemView.findViewById(R.id.searchUsername); // Usamos o campo username para a mensagem
        }
    }
}