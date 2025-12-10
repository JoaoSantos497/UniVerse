package com.universe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // <--- Importante
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // <--- Importante
import com.google.firebase.firestore.FirebaseFirestore; // <--- Importante

import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private FirebaseFirestore db;
    private Context context; // Necessário para o Glide

    public CommentsAdapter(List<Comment> commentList) {
        this.commentList = commentList;
        this.db = FirebaseFirestore.getInstance(); // Inicializar Firestore
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext(); // Guardar o contexto
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.userName.setText(comment.getUserName());
        holder.content.setText(comment.getContent());

        // Calcular o tempo
        long now = System.currentTimeMillis();
        CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                comment.getTimestamp(),
                now,
                android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.date.setText(relativeTime);

        // --- LÓGICA DA FOTO DE PERFIL ---
        // Se o comentário tiver um ID de utilizador, vamos buscar a foto
        if (comment.getUserId() != null) {
            db.collection("users").document(comment.getUserId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String photoUrl = documentSnapshot.getString("photoUrl");

                            // Se tiver link da foto, carrega com Glide
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(context)
                                        .load(photoUrl)
                                        .circleCrop()
                                        .into(holder.imgProfile);
                            } else {
                                // Se não tiver, põe a imagem padrão
                                holder.imgProfile.setImageResource(R.drawable.circle_bg);
                            }
                        }
                    });
        } else {
            // Comentários antigos sem ID ficam com a imagem padrão
            holder.imgProfile.setImageResource(R.drawable.circle_bg);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView userName, content, date;
        ImageView imgProfile; // <--- NOVA VARIÁVEL PARA A IMAGEM

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.commentUser);
            content = itemView.findViewById(R.id.commentContent);
            date = itemView.findViewById(R.id.commentDate);

            // Ligar ao ID que criámos no XML (item_comment.xml)
            imgProfile = itemView.findViewById(R.id.commentProfileImage);
        }
    }
}