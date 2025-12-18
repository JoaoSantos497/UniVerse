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
import androidx.core.content.ContextCompat;
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
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        String postId = postList.get(position).getPostId();
        return postId != null ? postId.hashCode() : position;
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

        // Tempo relativo
        long now = System.currentTimeMillis();
        CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                post.getTimestamp(), now, android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.txtDate.setText(relativeTime);

        // Imagem do Post
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(post.getImageUrl()).centerCrop()
                    .placeholder(R.drawable.bg_search_outline).into(holder.postImage);

            holder.postImage.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                intent.putExtra("imageUrl", post.getImageUrl());
                context.startActivity(intent);
            });
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        // Foto de Perfil (Carregamento eficiente)
        Glide.with(context).load(post.getUserPhotoUrl())
                .circleCrop().placeholder(R.drawable.circle_bg).into(holder.imgProfile);

        // Menu de Opções
        holder.btnMoreOptions.setOnClickListener(v -> mostrarMenuOpcoes(v, post, holder.getBindingAdapterPosition()));

        // Likes e Comentários
        configurarLikes(holder, post);
        configurarComentarios(holder, post);

        // Ir para Perfil Público
        holder.imgProfile.setOnClickListener(v -> {
            if (post.getUserId() != null) {
                Intent intent = new Intent(context, PublicProfileActivity.class);
                intent.putExtra("targetUserId", post.getUserId());
                context.startActivity(intent);
            }
        });
    }

    private void mostrarMenuOpcoes(View v, Post post, int position) {
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
                case 2: confirmarExclusao(post.getPostId(), position); return true;
                case 3: denunciarPost(post); return true;
                default: return false;
            }
        });
        popup.show();
    }

    private void confirmarExclusao(String postId, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Eliminar Post")
                .setMessage("Esta ação não pode ser desfeita.")
                .setPositiveButton("Eliminar", (dialog, which) -> apagarPostDoFirebase(postId, position))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void apagarPostDoFirebase(String postId, int position) {
        if (postId == null) return;

        db.collection("posts").document(postId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Nota: Se usas SnapshotListener no Fragment, ele removerá o item automaticamente.
                    // Se não usas, mantemos a remoção manual aqui:
                    if (position >= 0 && position < postList.size()) {
                        Toast.makeText(context, "Publicação eliminada", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Erro ao eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void configurarLikes(PostViewHolder holder, Post post) {
        List<String> likes = post.getLikes();
        boolean isLiked = likes != null && likes.contains(currentUserId);
        int likeCount = (likes != null) ? likes.size() : 0;

        holder.txtLike.setText((isLiked ? "❤️ " : "🤍 ") + likeCount);
        holder.txtLike.setTextColor(isLiked ? Color.RED : Color.GRAY);

        holder.txtLike.setOnClickListener(v -> {
            if (post.getPostId() == null) return;
            if (isLiked) {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayRemove(currentUserId));
            } else {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayUnion(currentUserId));
                enviarNotificacaoLike(post.getUserId(), post.getPostId());
            }
        });
    }

    private void configurarComentarios(PostViewHolder holder, Post post) {
        // Mostra contagem simplificada. Se quiseres contagem real em tempo real, mantém o SnapshotListener.
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
                Toast.makeText(context, "Obrigado por denunciares. Vamos analisar.", Toast.LENGTH_SHORT).show());
    }

    private void enviarNotificacaoLike(String targetId, String postId) {
        if (targetId == null || targetId.equals(currentUserId)) return;

        // Obter nome de quem deu like
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Map<String, Object> notif = new HashMap<>();
                notif.put("targetUserId", targetId);
                notif.put("fromUserId", currentUserId);
                notif.put("fromUserName", doc.getString("nome"));
                notif.put("fromUserPhoto", doc.getString("photoUrl"));
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