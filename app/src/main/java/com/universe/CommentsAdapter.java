package com.universe;

import android.app.AlertDialog;
import android.content.Context;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private Context context;
    private OnReplyListener replyListener;
    private FirebaseFirestore db;
    private String currentUserId;

    public interface OnReplyListener {
        void onReplyClick(String username);
    }

    public CommentsAdapter(List<Comment> commentList, OnReplyListener listener) {
        this.commentList = commentList;
        this.replyListener = listener;
        this.db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        // Ativa IDs estáveis para evitar bugs visuais durante a reciclagem
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        // Usa o hash do ID do comentário como identificador único estável
        return commentList.get(position).getCommentId().hashCode();
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

        holder.userName.setText(comment.getUserName());
        holder.content.setText(comment.getContent());

        long now = System.currentTimeMillis();
        CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                comment.getTimestamp(),
                now,
                android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.date.setText(relativeTime);

        // Foto de Perfil
        if (comment.getUserPhotoUrl() != null && !comment.getUserPhotoUrl().isEmpty()) {
            Glide.with(context).load(comment.getUserPhotoUrl()).circleCrop()
                    .placeholder(R.drawable.circle_bg).into(holder.imgProfile);
        } else if (comment.getUserId() != null) {
            holder.imgProfile.setImageResource(R.drawable.circle_bg);
            db.collection("users").document(comment.getUserId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && context != null) {
                            String url = doc.getString("photoUrl");
                            if (url != null) Glide.with(context).load(url).circleCrop().into(holder.imgProfile);
                        }
                    });
        }

        // Imagem Anexada
        if (comment.getCommentImageUrl() != null && !comment.getCommentImageUrl().isEmpty()) {
            holder.attachedImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(comment.getCommentImageUrl()).into(holder.attachedImage);
        } else {
            holder.attachedImage.setVisibility(View.GONE);
        }

        holder.btnReply.setOnClickListener(v -> {
            if (replyListener != null) replyListener.onReplyClick(comment.getUserName());
        });

        // --- MENU DE OPÇÕES (3 PONTOS) ---
        holder.btnMore.setOnClickListener(v -> {
            // Obtém a posição REAL no momento do clique
            int actualPos = holder.getBindingAdapterPosition();
            if (actualPos == RecyclerView.NO_POSITION) return;

            PopupMenu popup = new PopupMenu(context, v);
            if (comment.getUserId() != null && comment.getUserId().equals(currentUserId)) {
                popup.getMenu().add(0, 1, 0, "Editar Comentário");
                popup.getMenu().add(0, 2, 1, "Apagar Comentário");
            } else {
                popup.getMenu().add(0, 3, 0, "Denunciar");
            }

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        if (context instanceof CommentsActivity) {
                            ((CommentsActivity) context).prepararEdicaoComentario(comment);
                        }
                        return true;
                    case 2:
                        confirmarEliminacao(comment, actualPos); // Passa a posição real
                        return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void confirmarEliminacao(Comment comment, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Apagar Comentário")
                .setMessage("Tens a certeza que queres eliminar este comentário?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    if (context instanceof CommentsActivity) {
                        // Chama o método da Activity
                        ((CommentsActivity) context).apagarComentario(comment.getCommentId(), position);
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    // Metodo para ser usado pela Activity para remover da lista local de forma segura
    public void removerItemDaLista(int position) {
        if (position >= 0 && position < commentList.size()) {
            commentList.remove(position);
            notifyItemRemoved(position);
            // Corrige o bug visual de itens desaparecendo abaixo
            notifyItemRangeChanged(position, commentList.size() - position);
        }
    }

    @Override
    public int getItemCount() { return commentList.size(); }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView userName, content, date, btnReply;
        ImageView imgProfile, attachedImage;
        ImageButton btnMore;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.commentUser);
            content = itemView.findViewById(R.id.commentContent);
            date = itemView.findViewById(R.id.commentDate);
            imgProfile = itemView.findViewById(R.id.commentProfileImage);
            attachedImage = itemView.findViewById(R.id.commentAttachedImage);
            btnReply = itemView.findViewById(R.id.btnReplyItem);
            btnMore = itemView.findViewById(R.id.btnMoreOptionsComment);
        }
    }
}