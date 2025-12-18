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
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        String id = commentList.get(position).getCommentId();
        return id != null ? id.hashCode() : position;
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

        holder.userName.setText(comment.getUserName() != null ? comment.getUserName() : "Utilizador");
        holder.content.setText(comment.getContent());

        // Tempo relativo (ex: "há 5 min")
        long now = System.currentTimeMillis();
        CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                comment.getTimestamp(),
                now,
                android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.date.setText(relativeTime);

        // Foto de Perfil
        Glide.with(context)
                .load(comment.getUserPhotoUrl())
                .circleCrop()
                .placeholder(R.drawable.circle_bg)
                .into(holder.imgProfile);

        // Imagem Anexada ao Comentário
        if (comment.getCommentImageUrl() != null && !comment.getCommentImageUrl().isEmpty()) {
            holder.attachedImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(comment.getCommentImageUrl())
                    .centerCrop()
                    .into(holder.attachedImage);
        } else {
            holder.attachedImage.setVisibility(View.GONE);
        }

        // Responder
        holder.btnReply.setOnClickListener(v -> {
            if (replyListener != null) replyListener.onReplyClick(comment.getUserName());
        });

        // Menu de Opções (Editar/Apagar)
        holder.btnMore.setOnClickListener(v -> {
            int actualPos = holder.getBindingAdapterPosition();
            if (actualPos == RecyclerView.NO_POSITION) return;

            PopupMenu popup = new PopupMenu(context, v);

            // Só o dono do comentário pode editar ou apagar
            if (comment.getUserId() != null && comment.getUserId().equals(currentUserId)) {
                popup.getMenu().add(0, 1, 0, "Editar");
                popup.getMenu().add(0, 2, 1, "Apagar");
            } else {
                popup.getMenu().add(0, 3, 0, "Denunciar");
            }

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    if (context instanceof CommentsActivity) {
                        ((CommentsActivity) context).prepararEdicaoComentario(comment);
                    }
                } else if (item.getItemId() == 2) {
                    confirmarEliminacao(comment, actualPos);
                } else if (item.getItemId() == 3) {
                    Toast.makeText(context, "Denúncia enviada.", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            popup.show();
        });
    }

    private void confirmarEliminacao(Comment comment, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Apagar Comentário")
                .setMessage("Desejas eliminar permanentemente este comentário?")
                .setPositiveButton("Apagar", (dialog, which) -> {
                    if (context instanceof CommentsActivity) {
                        ((CommentsActivity) context).apagarComentario(comment.getCommentId(), position);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public void removerItemDaLista(int position) {
        if (position >= 0 && position < commentList.size()) {
            commentList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, commentList.size());
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