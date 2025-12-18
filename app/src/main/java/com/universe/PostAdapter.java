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
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        String id = postList.get(position).getPostId();
        return id != null ? id.hashCode() : position;
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

        long now = System.currentTimeMillis();
        holder.txtDate.setText(android.text.format.DateUtils.getRelativeTimeSpanString(
                post.getTimestamp(), now, android.text.format.DateUtils.MINUTE_IN_MILLIS));

        // Imagem do Post
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(post.getImageUrl()).centerCrop().into(holder.postImage);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        // Foto de Perfil (Busca rápida)
        db.collection("users").document(post.getUserId()).get().addOnSuccessListener(doc -> {
            if (doc.exists() && context != null) {
                String url = doc.getString("photoUrl");
                Glide.with(context).load(url).circleCrop().placeholder(R.drawable.circle_bg).into(holder.imgProfile);
            }
        });

        // Likes em Tempo Real
        configurarLikesTempoReal(holder, post);

        // Comentários (Contagem e Clique)
        configurarComentarios(holder, post);

        // Menu Opções
        holder.btnMoreOptions.setOnClickListener(v -> {
            int actualPos = holder.getBindingAdapterPosition();
            if (actualPos == RecyclerView.NO_POSITION) return;

            PopupMenu popup = new PopupMenu(context, v);
            if (post.getUserId().equals(currentUserId)) {
                popup.getMenu().add(0, 1, 0, "Editar");
                popup.getMenu().add(0, 2, 1, "Apagar");
            } else {
                popup.getMenu().add(0, 3, 0, "Denunciar");
            }

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) abrirEditor(post);
                else if (item.getItemId() == 2) confirmarExclusao(post.getPostId(), actualPos);
                return true;
            });
            popup.show();
        });
    }

    private void configurarLikesTempoReal(PostViewHolder holder, Post post) {
        db.collection("posts").document(post.getPostId())
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        List<String> likes = (List<String>) snapshot.get("likes");
                        post.setLikes(likes); // Atualiza o objeto local

                        boolean isLiked = likes != null && likes.contains(currentUserId);
                        int count = (likes != null) ? likes.size() : 0;

                        holder.txtLike.setText((isLiked ? "❤️ " : "🤍 ") + count);
                        holder.txtLike.setTextColor(isLiked ? Color.RED : Color.GRAY);
                    }
                });

        holder.txtLike.setOnClickListener(v -> {
            boolean liked = post.getLikes() != null && post.getLikes().contains(currentUserId);
            if (liked) {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayRemove(currentUserId));
            } else {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayUnion(currentUserId));
                enviarNotificacaoLike(post.getUserId(), post.getPostId());
            }
        });
    }

    private void confirmarExclusao(String postId, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Apagar Post")
                .setMessage("Tens a certeza?")
                .setPositiveButton("Sim", (d, w) -> {
                    db.collection("posts").document(postId).delete()
                            .addOnSuccessListener(aVoid -> {
                                if (position < postList.size()) {
                                    postList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, postList.size());
                                }
                            });
                })
                .setNegativeButton("Não", null).show();
    }

    private void configurarComentarios(PostViewHolder holder, Post post) {
        db.collection("posts").document(post.getPostId()).collection("comments")
                .addSnapshotListener((value, error) -> {
                    int count = (value != null) ? value.size() : 0;
                    holder.txtComment.setText("💬 " + count);
                });

        holder.txtComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", post.getPostId());
            context.startActivity(intent);
        });
    }

    private void enviarNotificacaoLike(String targetId, String postId) {
        if (targetId.equals(currentUserId)) return;
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Map<String, Object> n = new HashMap<>();
                n.put("targetUserId", targetId);
                n.put("fromUserId", currentUserId);
                n.put("fromUserName", doc.getString("nome"));
                n.put("type", "like");
                n.put("timestamp", System.currentTimeMillis());
                db.collection("notifications").add(n);
            }
        });
    }

    private void abrirEditor(Post post) {
        Intent i = new Intent(context, CreatePostActivity.class);
        // 1. Enviamos o ID (essencial para não duplicar)
        i.putExtra("editPostId", post.getPostId());
        // 2. Enviamos o conteúdo atual para preencher o EditText
        i.putExtra("currentContent", post.getContent());
        context.startActivity(i);
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