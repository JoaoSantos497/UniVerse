package com.universe;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private Context context;
    private List<Notification> mNotifications;

    public NotificationAdapter(Context context, List<Notification> mNotifications) {
        this.context = context;
        this.mNotifications = mNotifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notif = mNotifications.get(position);

        // Formatar texto: Nome a Negrito + Mensagem
        String texto = "<b>" + notif.getFromUserName() + "</b> " + notif.getMessage();
        holder.txtText.setText(Html.fromHtml(texto));

        // Data
        long now = System.currentTimeMillis();
        CharSequence timeAgo = android.text.format.DateUtils.getRelativeTimeSpanString(
                notif.getTimestamp(), now, android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.txtDate.setText(timeAgo);

        // Foto
        if (notif.getFromUserPhoto() != null && !notif.getFromUserPhoto().isEmpty()) {
            Glide.with(context).load(notif.getFromUserPhoto()).circleCrop().into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.circle_bg);
        }

        // CLIQUE NA NOTIFICAÇÃO
        holder.itemView.setOnClickListener(v -> {
            if (notif.getType().equals("follow")) {
                // Vai para o perfil da pessoa
                Intent intent = new Intent(context, PublicProfileActivity.class);
                intent.putExtra("targetUserId", notif.getFromUserId());
                context.startActivity(intent);
            }
            else if (notif.getType().equals("like") || notif.getType().equals("comment")) {
                // Vai para o post (usamos a CommentsActivity para ver o post e os comments)
                if (notif.getPostId() != null) {
                    Intent intent = new Intent(context, CommentsActivity.class);
                    intent.putExtra("postId", notif.getPostId());
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mNotifications.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView txtText, txtDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.notifImage);
            txtText = itemView.findViewById(R.id.notifText);
            txtDate = itemView.findViewById(R.id.notifDate);
        }
    }
}