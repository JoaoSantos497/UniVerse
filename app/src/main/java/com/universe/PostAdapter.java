package com.universe;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        // Precisamos do ID do utilizador atual para saber se ele já deu like
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

        // --- LÓGICA DO LIKE (Mantém igual) ---
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
            // ... (código do like que já tinhas) ...
            if (post.getPostId() == null) return;
            if (isLiked) {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayRemove(currentUserId));
            } else {
                db.collection("posts").document(post.getPostId()).update("likes", FieldValue.arrayUnion(currentUserId));
            }
        });

        // --- LÓGICA DE APAGAR (NOVO) ---
        // Verifica se o dono do post (post.getUserId()) é igual a quem está logado (currentUserId)
        if (post.getUserId() != null && post.getUserId().equals(currentUserId)) {
            // É meu post -> Mostra o lixo
            holder.btnDelete.setVisibility(View.VISIBLE);

            holder.btnDelete.setOnClickListener(v -> {
                // Apagar do Firebase
                if (post.getPostId() != null) {
                    db.collection("posts").document(post.getPostId()).delete();
                    // O Listener no HomeFragment vai atualizar a lista automaticamente!
                }
            });
        } else {
            // Não é meu post -> Esconde o lixo
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserName, txtContent, txtDate, txtLike;
        android.widget.ImageButton btnDelete; // <--- NOVO

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.postUserName);
            txtContent = itemView.findViewById(R.id.postContent);
            txtDate = itemView.findViewById(R.id.postDate);
            txtLike = itemView.findViewById(R.id.postLikeBtn);
            btnDelete = itemView.findViewById(R.id.btnDeletePost); // <--- Ligar ID
        }
    }
}