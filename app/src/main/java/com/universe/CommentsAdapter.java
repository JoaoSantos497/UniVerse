package com.universe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private FirebaseFirestore db;
    private Context context;


    public CommentsAdapter(List<Comment> commentList) {
        this.commentList = commentList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        // 1. Texto e Nome
        holder.userName.setText(comment.getUserName());
        holder.content.setText(comment.getContent());

        // 2. Data
        long now = System.currentTimeMillis();
        CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                comment.getTimestamp(),
                now,
                android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.date.setText(relativeTime);

        // --- 3. IMAGEM ANEXADA NO COMENTÁRIO (NOVO) ---
        if (comment.getCommentImageUrl() != null && !comment.getCommentImageUrl().isEmpty()) {
            holder.attachedImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(comment.getCommentImageUrl())
                    // Não usamos circleCrop aqui porque queremos ver a foto normal
                    .into(holder.attachedImage);
        } else {
            holder.attachedImage.setVisibility(View.GONE);
        }

        // --- 4. FOTO DE PERFIL ---
        if (comment.getUserId() != null) {
            db.collection("users").document(comment.getUserId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String photoUrl = documentSnapshot.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(context).load(photoUrl).circleCrop().into(holder.imgProfile);
                            } else {
                                holder.imgProfile.setImageResource(R.drawable.circle_bg);
                            }
                        }
                    });
        } else {
            holder.imgProfile.setImageResource(R.drawable.circle_bg);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView userName, content, date;
        ImageView imgProfile;
        ImageView attachedImage;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.commentUser);
            content = itemView.findViewById(R.id.commentContent);
            date = itemView.findViewById(R.id.commentDate);
            imgProfile = itemView.findViewById(R.id.commentProfileImage);

            // Ligar ao ID que criámos no item_comment.xml
            attachedImage = itemView.findViewById(R.id.commentAttachedImage);
        }
    }
}