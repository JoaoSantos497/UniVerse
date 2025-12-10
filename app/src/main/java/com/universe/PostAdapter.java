package com.universe;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView; // Importante para o Glide

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Importante para carregar imagens
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private String currentUserId;
    private FirebaseFirestore db;
    private Context context; // Necessário para Intents e Glide

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
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.txtUserName.setText(post.getUserName());
        holder.txtContent.setText(post.getContent());

        // Formatar data relativa
        long now = System.currentTimeMillis();
        CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                post.getTimestamp(),
                now,
                android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.txtDate.setText(relativeTime);

        // --- 0. CARREGAR IMAGEM DE PERFIL ---
        // Vamos buscar a foto atualizada do utilizador à base de dados
        db.collection("users").document(post.getUserId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        // Se tiver foto, usa o Glide. Se não, usa uma imagem padrão.
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            // Carrega a imagem no ImageView (que antes era TextView)
                            // Nota: Precisas de mudar o tipo no XML item_post.xml se ainda for TextView
                            // Mas como no teu XML já tens um TextView 'postProfileImage' com background circle,
                            // o ideal é trocar esse TextView por um ImageView no XML (ver passo 3 abaixo).
                            // AQUI ASSUMO QUE JÁ TROCASTE PARA IMAGEVIEW NO XML:
                            /* Se ainda for TextView no XML, este código vai dar erro de tipo.
                               Tens de ir ao item_post.xml e mudar <TextView id="@+id/postProfileImage"...
                               para <ImageView id="@+id/postProfileImage"... />
                            */

                            // Se for ImageView:
                            Glide.with(context).load(photoUrl).circleCrop().into((ImageView) holder.imgProfile);
                        } else {
                            // Se não tiver foto, podes manter o texto ou por uma imagem default
                            // Aqui simplificamos assumindo que vais mudar para ImageView
                            ((ImageView)holder.imgProfile).setImageResource(R.drawable.circle_bg);
                        }
                    }
                });

        // --- 1. CLIQUE NA FOTO -> IR PARA PERFIL PÚBLICO ---
        holder.imgProfile.setOnClickListener(v -> {
            if (!post.getUserId().equals(currentUserId)) {
                Intent intent = new Intent(context, PublicProfileActivity.class);
                intent.putExtra("targetUserId", post.getUserId()); // Usar "targetUserId" para combinar com a tua Activity
                context.startActivity(intent);
            }
        });

        // --- 2. LÓGICA DO LIKE ---
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

        // --- 3. LÓGICA DO COMENTÁRIO (CONTAGEM) ---
        // Listener em tempo real para contar comentários
        db.collection("posts").document(post.getPostId()).collection("comments")
                .addSnapshotListener((value, error) -> {
                    if (error == null && value != null) {
                        int count = value.size();
                        holder.txtComment.setText("💬 " + count); // Atualiza o texto com o número
                    } else {
                        holder.txtComment.setText("💬 0");
                    }
                });

        holder.txtComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", post.getPostId());
            context.startActivity(intent);
        });

        // --- 4. LÓGICA DE APAGAR ---
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
        View imgProfile; // Mudei para View para ser genérico, mas deves fazer Cast para ImageView
        ImageButton btnDelete;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.postUserName);
            txtContent = itemView.findViewById(R.id.postContent);
            txtDate = itemView.findViewById(R.id.postDate);
            txtLike = itemView.findViewById(R.id.postLikeBtn);
            txtComment = itemView.findViewById(R.id.postCommentBtn);
            imgProfile = itemView.findViewById(R.id.postProfileImage); // O ID da imagem
            btnDelete = itemView.findViewById(R.id.btnDeletePost);
        }
    }
}