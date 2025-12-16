package com.universe;

import android.content.Context;
import android.content.Intent;
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

    // 1. Interface para comunicar o clique à Activity
    public interface OnReplyListener {
        void onReplyClick(String username);
    }

    private OnReplyListener replyListener;

    // 2. Construtor ATUALIZADO (Recebe a lista E o listener)
    public CommentsAdapter(List<Comment> commentList, OnReplyListener listener) {
        this.commentList = commentList;
        this.replyListener = listener; // Guardamos o listener
        this.db = FirebaseFirestore.getInstance();
    }

    // Construtor antigo (caso precises para compatibilidade, mas o erro vai desaparecer com o de cima)
    public CommentsAdapter(List<Comment> commentList) {
        this.commentList = commentList;
        this.replyListener = null;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        // Certifica-te que o layout se chama item_comment
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.userName.setText(comment.getUserName());
        holder.content.setText(comment.getContent());

        long now = System.currentTimeMillis();
        CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                comment.getTimestamp(),
                now,
                android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.date.setText(relativeTime);

        // Imagem Anexada
        if (comment.getCommentImageUrl() != null && !comment.getCommentImageUrl().isEmpty()) {
            holder.attachedImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(comment.getCommentImageUrl()).into(holder.attachedImage);

            holder.attachedImage.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                intent.putExtra("imageUrl", comment.getCommentImageUrl());
                context.startActivity(intent);
            });
        } else {
            holder.attachedImage.setVisibility(View.GONE);
        }

        // Foto de Perfil
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

        // 3. CLIQUE NO BOTÃO RESPONDER
        // Se definimos um listener, ativamos o clique
        if (holder.btnReply != null) {
            holder.btnReply.setOnClickListener(v -> {
                if (replyListener != null) {
                    replyListener.onReplyClick(comment.getUserName());
                }
            });
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
        TextView btnReply; // <--- O botão de responder no XML

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.commentUser);
            content = itemView.findViewById(R.id.commentContent);
            date = itemView.findViewById(R.id.commentDate);
            imgProfile = itemView.findViewById(R.id.commentProfileImage);
            attachedImage = itemView.findViewById(R.id.commentAttachedImage);

            // Certifica-te que adicionaste este ID ao teu item_comment.xml
            btnReply = itemView.findViewById(R.id.btnReplyItem);
        }
    }
}