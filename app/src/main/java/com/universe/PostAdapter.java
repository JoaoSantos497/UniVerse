package com.universe;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private String currentUserId;
    private FirebaseFirestore db;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.txtUserName.setText(post.getUserName());
        holder.txtContent.setText(post.getContent());
        holder.txtDate.setText(post.getDate());

        // --- 1. LÓGICA DO LIKE ---
        List<String> likes = post.getLikes();
        boolean isLiked = likes.contains(currentUserId);

        if (isLiked) {
            holder.txtLike.setText("❤️ " + likes.size());
            holder.txtLike.setTextColor(Color.RED);
        } else {
            holder.txtLike.setText("🤍 " + likes.size());
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

        // --- 2. LÓGICA DO COMENTÁRIO (NOVO) ---
        holder.txtComment.setOnClickListener(v -> {
            // Criar um Intent para abrir a Activity de Comentários
            Intent intent = new Intent(v.getContext(), CommentsActivity.class);

            // Passar o ID do post para sabermos qual carregar
            intent.putExtra("postId", post.getPostId());

            // Iniciar a nova janela
            v.getContext().startActivity(intent);
        });

        // --- 3. LÓGICA DE APAGAR ---
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
        TextView txtUserName, txtContent, txtDate, txtLike, txtComment; // Adicionado txtComment
        ImageButton btnDelete;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.postUserName);
            txtContent = itemView.findViewById(R.id.postContent);
            txtDate = itemView.findViewById(R.id.postDate);
            txtLike = itemView.findViewById(R.id.postLikeBtn);
            txtComment = itemView.findViewById(R.id.postCommentBtn); // Ligar ao XML
            btnDelete = itemView.findViewById(R.id.btnDeletePost);
        }
    }
}