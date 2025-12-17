package com.universe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        holder.txtUserName.setText(post.getUserName());
        holder.txtContent.setText(post.getContent());

        try {
            long now = System.currentTimeMillis();
            CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                    post.getTimestamp(), now, android.text.format.DateUtils.MINUTE_IN_MILLIS);
            holder.txtDate.setText(relativeTime);
        } catch (Exception e) {
            holder.txtDate.setText(post.getDate());
        }

        // Imagem do Post
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(post.getImageUrl()).centerCrop().placeholder(R.drawable.bg_search_outline).into(holder.postImage);

            holder.postImage.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                intent.putExtra("imageUrl", post.getImageUrl());
                context.startActivity(intent);
            });
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        // --- CARREGAR FOTO (USANDO getUserId) ---
        if (post.getUserId() != null) { // <--- MUDANÇA AQUI
            db.collection("users").document(post.getUserId()).get() // <--- E AQUI
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Tenta encontrar a foto com vários nomes
                            String photoUrl = documentSnapshot.getString("photoUrl");
                            if (photoUrl == null) photoUrl = documentSnapshot.getString("fotoUrl");
                            if (photoUrl == null) photoUrl = documentSnapshot.getString("profileImage");

                            if (photoUrl != null && !photoUrl.isEmpty() && context != null) {
                                Glide.with(context)
                                        .load(photoUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.circle_bg)
                                        .into(holder.imgProfile);
                            } else {
                                holder.imgProfile.setImageResource(R.drawable.circle_bg);
                            }
                        }
                    });
        }

        // Clique no Perfil
        holder.imgProfile.setOnClickListener(v -> {
            if (currentUserId != null && post.getUserId() != null && !post.getUserId().equals(currentUserId)) {
                Intent intent = new Intent(context, PublicProfileActivity.class);
                intent.putExtra("targetUserId", post.getUserId()); // <--- AQUI
                context.startActivity(intent);
            }
        });

        // Likes
        List<String> likes = post.getLikes();
        boolean isLiked = likes != null && likes.contains(currentUserId);
        int likeCount = (likes != null) ? likes.size() : 0;

        if (isLiked) {
            holder.txtLike.setText("❤️ " + likeCount);
            holder.txtLike.setTextColor(Color.RED);
        } else {
            holder.txtLike.setText("🤍 " + likeCount);
            holder.txtLike.setTextColor(Color.GRAY);
        }

        holder.txtLike.setOnClickListener(v -> {
            if (post.getPostId() == null || currentUserId == null) return;
            if (isLiked) {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayRemove(currentUserId));
            } else {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayUnion(currentUserId));
                if (post.getUserId() != null) {
                    enviarNotificacaoLike(post.getUserId(), post.getPostId()); // <--- AQUI
                }
            }
        });

        // Comentários
        if (post.getPostId() != null) {
            db.collection("posts").document(post.getPostId()).collection("comments")
                    .addSnapshotListener((value, error) -> {
                        if (error == null && value != null) {
                            holder.txtComment.setText("💬 " + value.size());
                        } else {
                            holder.txtComment.setText("💬 0");
                        }
                    });
        }

        holder.txtComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", post.getPostId());
            context.startActivity(intent);
        });

        // Botão Apagar
        if (currentUserId != null && post.getUserId() != null && post.getUserId().equals(currentUserId)) { // <--- AQUI
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Eliminar Post")
                        .setMessage("Tens a certeza?")
                        .setPositiveButton("Sim", (d, w) -> apagarPostDoFirebase(post.getPostId(), position))
                        .setNegativeButton("Não", null)
                        .show();
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return postList.size(); }

    private void apagarPostDoFirebase(String postId, int position) {
        if (postId == null) return;
        db.collection("posts").document(postId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (position >= 0 && position < postList.size()) {
                        postList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, postList.size());
                    }
                    Toast.makeText(context, "Post eliminado.", Toast.LENGTH_SHORT).show();
                });
    }

    private void enviarNotificacaoLike(String targetId, String postId) {
        if (targetId.equals(currentUserId)) return;
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User eu = doc.toObject(User.class);
                if (eu != null) {
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("targetUserId", targetId);
                    notif.put("fromUserId", currentUserId);
                    notif.put("fromUserName", eu.getNome());
                    notif.put("fromUserPhoto", eu.getPhotoUrl() != null ? eu.getPhotoUrl() : "");
                    notif.put("type", "like");
                    notif.put("message", "gostou da tua publicação");
                    notif.put("postId", postId);
                    notif.put("timestamp", System.currentTimeMillis());
                    notif.put("read", false);
                    db.collection("notifications").add(notif);
                }
            }
        });
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserName, txtContent, txtDate, txtLike, txtComment;
        ImageView imgProfile, postImage;
        ImageButton btnDelete;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.postUserName);
            txtContent = itemView.findViewById(R.id.postContent);
            txtDate = itemView.findViewById(R.id.postDate);
            txtLike = itemView.findViewById(R.id.postLikeBtn);
            txtComment = itemView.findViewById(R.id.postCommentBtn);
            imgProfile = itemView.findViewById(R.id.postProfileImage);
            postImage = itemView.findViewById(R.id.postImage);
            btnDelete = itemView.findViewById(R.id.btnDeletePost);
        }
    }
}