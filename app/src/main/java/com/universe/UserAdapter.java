package com.universe;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // <--- Mudança 1
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // <--- Mudança 2

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
        // Certifica-te que criaste o ficheiro item_user.xml que mandei antes!
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // 1. Preencher Texto
        holder.txtName.setText(user.getNome());

        if (user.getUsername() != null) {
            holder.txtUsername.setText("@" + user.getUsername());
        } else {
            holder.txtUsername.setText("");
        }

        // 2. Carregar FOTO (Lógica Nova com Glide)
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(holder.imgAvatar);
        } else {
            // Se não tiver foto, usa a imagem padrão cinzenta
            holder.imgAvatar.setImageResource(R.drawable.circle_bg);
        }

        // 3. Clique -> Abrir Perfil
        holder.itemView.setOnClickListener(v -> {
            // Só abre se tivermos o ID válido
            if (user.getUid() != null) {
                Intent intent = new Intent(context, PublicProfileActivity.class);
                intent.putExtra("targetUserId", user.getUid());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtUsername; // Mudámos de Curso para Username
        ImageView imgAvatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ligar aos IDs do novo item_user.xml
            txtName = itemView.findViewById(R.id.searchName);
            txtUsername = itemView.findViewById(R.id.searchUsername);
            imgAvatar = itemView.findViewById(R.id.searchAvatar);
        }
    }
}