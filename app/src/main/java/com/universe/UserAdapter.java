package com.universe;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;

    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.txtName.setText(user.getNome());
        holder.txtCourse.setText(user.getCurso());

        // --- LÓGICA DO AVATAR COLORIDO ---
        String nome = user.getNome();
        if (nome != null && !nome.isEmpty()) {
            String inicial = nome.substring(0, 1).toUpperCase();
            holder.txtAvatar.setText(inicial);

            // Gerar a mesma cor baseada no nome
            int hash = nome.hashCode();
            holder.txtAvatar.getBackground().setTint(Color.rgb(
                    Math.abs(hash * 25) % 255,
                    Math.abs(hash * 80) % 255,
                    Math.abs(hash * 13) % 255
            ));
        } else {
            holder.txtAvatar.setText("?");
            holder.txtAvatar.getBackground().setTint(Color.GRAY);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Criar o Intent para abrir o Perfil Público
                android.content.Intent intent = new android.content.Intent(v.getContext(), PublicProfileActivity.class);

                // 2. Passar o ID (uid) do utilizador clicado
                intent.putExtra("targetUserId", user.getUid());

                // 3. Iniciar
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtCourse, txtAvatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.searchUserName);
            txtCourse = itemView.findViewById(R.id.searchUserCourse);
            txtAvatar = itemView.findViewById(R.id.searchProfileImage);
        }


    }
}