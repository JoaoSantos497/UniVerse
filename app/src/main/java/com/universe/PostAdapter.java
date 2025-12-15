package com.universe;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private String currentUserId;
    private FirebaseFirestore db;
    private Context context;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
        this.db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // 1. Texto e Nome
        holder.txtUserName.setText(post.getUserName());
        holder.txtContent.setText(post.getContent());

        // 2. Data
        long now = System.currentTimeMillis();
        CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                post.getTimestamp(),
                now,
                android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.txtDate.setText(relativeTime);

        // --- 3. IMAGEM DO POST ---
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(post.getImageUrl())
                    .centerCrop()
                    .into(holder.postImage);

            // --- NOVO: CLIQUE PARA ABRIR EM ECRÃ CHEIO ---
            holder.postImage.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                intent.putExtra("imageUrl", post.getImageUrl());
                context.startActivity(intent);
            });

        } else {
            holder.postImage.setVisibility(View.GONE);
            holder.postImage.setOnClickListener(null); // Remove o clique se não houver imagem
        }

        // --- 4. FOTO DE PERFIL DO UTILIZADOR ---
        db.collection("users").document(post.getUserId()).get()
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

        // --- 5. CLIQUE NA FOTO DE PERFIL -> IR PARA PERFIL PÚBLICO ---
        holder.imgProfile.setOnClickListener(v -> {
            if (!post.getUserId().equals(currentUserId)) {
                Intent intent = new Intent(context, PublicProfileActivity.class);
                intent.putExtra("targetUserId", post.getUserId());
                context.startActivity(intent);
            }
        });

        // --- 6. LÓGICA DO LIKE ---
        List<String> likes = post.getLikes();
        boolean isLiked = likes != null && likes.contains(currentUserId);
        int likeCount = (likes != null) ? likes.size() : 0;

        if (isLiked) {
            holder.txtLike.setText("❤️ " + likeCount);
            holder.txtLike.setTextColor(Color.RED);
        } else {
            holder.txtLike.setText("🤍 " + likeCount);
            holder.txtLike.setTextColor(Color.DKGRAY);
        }

        holder.txtLike.setOnClickListener(v -> {
            if (post.getPostId() == null) return;
            if (isLiked) {
                db.collection("posts").document(post.getPostId())
                        .update("likes", FieldValue.arrayRemove(currentUserId));
            } else {
                db.collection("posts").document(post.getPostId())
                        .update("likes", FieldValue.arrayUnion(currentUserId));
            }
        });

        // --- 7. LÓGICA DO COMENTÁRIO ---
        db.collection("posts").document(post.getPostId()).collection("comments")
                .addSnapshotListener((value, error) -> {
                    if (error == null && value != null) {
                        int count = value.size();
                        holder.txtComment.setText("💬 " + count);
                    } else {
                        holder.txtComment.setText("💬 0");
                    }
                });

        holder.txtComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", post.getPostId());
            context.startActivity(intent);
        });

        // --- 8. LÓGICA DE APAGAR ---
        if (post.getUserId() != null && post.getUserId().equals(currentUserId)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (post.getPostId() != null) {
                    db.collection("posts").document(post.getPostId()).delete();
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserName, txtContent, txtDate, txtLike, txtComment;
        ImageView imgProfile;
        ImageView postImage;
        ImageButton btnDelete;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.postUserName);
            txtContent = itemView.findViewById(R.id.postContent);
            txtDate = itemView.findViewById(R.id.postDate);
            txtLike = itemView.findViewById(R.id.postLikeBtn);
            txtComment = itemView.findViewById(R.id.postCommentBtn);
            imgProfile = itemView.findViewById(R.id.postProfileImage);
            btnDelete = itemView.findViewById(R.id.btnDeletePost);
            postImage = itemView.findViewById(R.id.postImage);
        }
    }
}