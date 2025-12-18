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
import android.widget.PopupMenu;
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
        // Melhora a performance e estabilidade das animações
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        // Retorna um ID único baseado no ID do documento para evitar bugs de reciclagem
        return postList.get(position).getPostId().hashCode();
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

        // Foto de Perfil
        if (post.getUserId() != null) {
            db.collection("users").document(post.getUserId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && context != null) {
                            String url = doc.getString("photoUrl");
                            if (url == null) url = doc.getString("fotoUrl");
                            Glide.with(context).load(url).circleCrop().placeholder(R.drawable.circle_bg).into(holder.imgProfile);
                        }
                    });
        }

        // --- MENU DE OPÇÕES ---
        holder.btnMoreOptions.setOnClickListener(v -> {
            // Obtemos a posição atualizada no momento do clique
            int actualPos = holder.getBindingAdapterPosition();
            if (actualPos == RecyclerView.NO_POSITION) return;

            PopupMenu popup = new PopupMenu(context, v);
            if (post.getUserId() != null && post.getUserId().equals(currentUserId)) {
                popup.getMenu().add(0, 1, 0, "Editar Publicação");
                popup.getMenu().add(0, 2, 1, "Apagar Publicação");
            } else {
                popup.getMenu().add(0, 3, 0, "Denunciar Conteúdo");
            }

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1: abrirEditorPost(post); return true;
                    case 2: confirmarExclusao(post.getPostId(), actualPos); return true;
                    case 3: denunciarPost(post); return true;
                }
                return false;
            });
            popup.show();
        });

        // Likes e Comentários
        configurarLikes(holder, post);
        configurarComentarios(holder, post);

        holder.imgProfile.setOnClickListener(v -> {
            if (currentUserId != null && post.getUserId() != null && !post.getUserId().equals(currentUserId)) {
                Intent intent = new Intent(context, PublicProfileActivity.class);
                intent.putExtra("targetUserId", post.getUserId());
                context.startActivity(intent);
            }
        });
    }

    private void confirmarExclusao(String postId, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Eliminar Publicação")
                .setMessage("Tens a certeza que queres apagar permanentemente este post?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Apagar", (dialog, which) -> apagarPostDoFirebase(postId, position))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void apagarPostDoFirebase(String postId, int position) {
        db.collection("posts").document(postId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Verificação extra para evitar IndexOutOfBoundsException
                    if (position >= 0 && position < postList.size()) {
                        postList.remove(position);
                        notifyItemRemoved(position);
                        // Atualiza as posições de todos os itens abaixo para evitar posts fantasmas
                        notifyItemRangeChanged(position, postList.size() - position);
                        Toast.makeText(context, "Post eliminado.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ... (configurarLikes, configurarComentarios, enviarNotificacaoLike e denunciarPost mantêm-se iguais)

    private void configurarLikes(PostViewHolder holder, Post post) {
        List<String> likes = post.getLikes();
        boolean isLiked = likes != null && likes.contains(currentUserId);
        int likeCount = (likes != null) ? likes.size() : 0;
        holder.txtLike.setText((isLiked ? "❤️ " : "🤍 ") + likeCount);
        holder.txtLike.setTextColor(isLiked ? Color.RED : Color.GRAY);
        holder.txtLike.setOnClickListener(v -> {
            if (post.getPostId() == null || currentUserId == null) return;
            if (isLiked) {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayRemove(currentUserId));
            } else {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayUnion(currentUserId));
                if (post.getUserId() != null) enviarNotificacaoLike(post.getUserId(), post.getPostId());
            }
        });
    }

    private void configurarComentarios(PostViewHolder holder, Post post) {
        if (post.getPostId() != null) {
            db.collection("posts").document(post.getPostId()).collection("comments")
                    .addSnapshotListener((value, error) -> {
                        holder.txtComment.setText("💬 " + (value != null ? value.size() : 0));
                    });
        }
        holder.txtComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", post.getPostId());
            context.startActivity(intent);
        });
    }

    private void abrirEditorPost(Post post) {
        Intent intent = new Intent(context, CreatePostActivity.class);
        intent.putExtra("editPostId", post.getPostId());
        intent.putExtra("currentContent", post.getContent());
        context.startActivity(intent);
    }

    private void denunciarPost(Post post) {
        Map<String, Object> report = new HashMap<>();
        report.put("postId", post.getPostId());
        report.put("reportedBy", currentUserId);
        report.put("timestamp", System.currentTimeMillis());
        db.collection("reports").add(report).addOnSuccessListener(doc ->
                Toast.makeText(context, "Denúncia enviada.", Toast.LENGTH_SHORT).show());
    }

    private void enviarNotificacaoLike(String targetId, String postId) {
        if (targetId.equals(currentUserId)) return;
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
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
        });
    }

    @Override
    public int getItemCount() { return postList.size(); }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserName, txtContent, txtDate, txtLike, txtComment;
        ImageView imgProfile, postImage;
        ImageButton btnMoreOptions;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.postUserName);
            txtContent = itemView.findViewById(R.id.postContent);
            txtDate = itemView.findViewById(R.id.postDate);
            txtLike = itemView.findViewById(R.id.postLikeBtn);
            txtComment = itemView.findViewById(R.id.postCommentBtn);
            imgProfile = itemView.findViewById(R.id.postProfileImage);
            postImage = itemView.findViewById(R.id.postImage);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}