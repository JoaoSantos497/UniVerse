package com.universe;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;

    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        // Certifica-te que tens o ficheiro layout/item_user.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // 1. Preencher Texto
        if (user.getNome() != null) {
            holder.txtName.setText(user.getNome());
        } else {
            holder.txtName.setText("Utilizador");
        }

        if (user.getUsername() != null) {
            holder.txtUsername.setText("@" + user.getUsername());
        } else {
            holder.txtUsername.setText("");
        }

        // 2. Carregar FOTO com Glide
        String photoUrl = user.getPhotoUrl();

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(context)
                    .load(photoUrl)
                    .circleCrop() // Deixa a imagem redonda
                    .placeholder(R.drawable.circle_bg) // Mostra o círculo roxo enquanto carrega
                    .error(R.drawable.circle_bg) // Mostra o círculo roxo se der erro
                    .into(holder.imgAvatar);
        } else {
            // Se não tiver URL, mete a imagem padrão
            holder.imgAvatar.setImageResource(R.drawable.circle_bg);
        }

        // 3. Clique -> Abrir Perfil Público
        holder.itemView.setOnClickListener(v -> {
            if (user.getUid() != null) { // <--- ESTA VERIFICAÇÃO É CRUCIAL
                Intent intent = new Intent(context, PublicProfileActivity.class);
                intent.putExtra("targetUserId", user.getUid());
                context.startActivity(intent);
            } else {
                // Se o ID for nulo, não faz nada (evita o crash)
                Toast.makeText(context, "Erro: ID inválido", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtUsername;
        ImageView imgAvatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ligar aos IDs do layout item_user.xml
            txtName = itemView.findViewById(R.id.searchName);
            txtUsername = itemView.findViewById(R.id.searchUsername);
            imgAvatar = itemView.findViewById(R.id.searchAvatar);
        }
    }
}