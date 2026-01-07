package com.universe;

import static com.universe.NotificationType.LIKE;

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
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private String currentUserId;
    private FirebaseFirestore db;
    private Context context;
    private UserService userService;
    private NotificationService notificationService;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
        this.db = FirebaseFirestore.getInstance();
        this.userService = new UserService();
        this.notificationService = new NotificationService(userService);
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

        // Preencher dados básicos
        holder.txtUserName.setText(post.getUserName());
        holder.txtContent.setText(post.getContent());

        long now = System.currentTimeMillis();
        holder.txtDate.setText(android.text.format.DateUtils.getRelativeTimeSpanString(
                post.getTimestamp(), now, android.text.format.DateUtils.MINUTE_IN_MILLIS));

        // --- LÓGICA DO SLIDE (CARROSSEL) ---
        List<String> imagensParaMostrar = new ArrayList<>();

        // 1. Prioridade à lista de imagens (Múltiplas)
        if (post.getImagesUrls() != null && !post.getImagesUrls().isEmpty()) {
            imagensParaMostrar.addAll(post.getImagesUrls());
        }
        // 2. Fallback para imagem única (Retrocompatibilidade)
        else if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            imagensParaMostrar.add(post.getImageUrl());
        }

        if (!imagensParaMostrar.isEmpty()) {
            holder.mediaContainer.setVisibility(View.VISIBLE);

            // Configura o Adapter do Slide
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(imagensParaMostrar);
            holder.viewPager.setAdapter(sliderAdapter);

            // LIGAÇÃO DAS BOLINHAS (TABLAYOUT)
            if (imagensParaMostrar.size() > 1) {
                holder.tabLayout.setVisibility(View.VISIBLE);
                // TabLayoutMediator liga o ViewPager às bolinhas automaticamente
                new TabLayoutMediator(holder.tabLayout, holder.viewPager, (tab, pos) -> {
                    // O design das bolinhas é tratado no XML (indicator_selector)
                }).attach();
            } else {
                holder.tabLayout.setVisibility(View.GONE);
            }
        } else {
            // Se não houver imagens, esconde a área de media
            holder.mediaContainer.setVisibility(View.GONE);
        }

        // Carregar Foto de Perfil
        db.collection("users").document(post.getUserId()).get().addOnSuccessListener(doc -> {
            if (doc.exists() && context != null) {
                String url = doc.getString("photoUrl");
                if (url != null) {
                    Glide.with(context).load(url).circleCrop().placeholder(R.drawable.ic_person_filled).into(holder.imgProfile);
                }
            }
        });

        configurarLikesTempoReal(holder, post);
        configurarComentarios(holder, post);
        configurarCliquesProfile(holder, post);

        // Menu Opções
        holder.btnMoreOptions.setOnClickListener(v -> mostrarMenuOpcoes(v, post));
    }

    // --- MÉTODOS AUXILIARES ---

    private void mostrarMenuOpcoes(View v, Post post) {
        PopupMenu popup = new PopupMenu(context, v);
        if (post.getUserId().equals(currentUserId)) {
            popup.getMenu().add(0, 1, 0, "Editar");
            popup.getMenu().add(0, 2, 1, "Apagar");
        } else {
            popup.getMenu().add(0, 3, 0, "Denunciar");
        }

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) abrirEditor(post);
            else if (item.getItemId() == 2) confirmarExclusao(post.getPostId());
            return true;
        });
        popup.show();
    }

    private void configurarCliquesProfile(PostViewHolder holder, Post post) {
        View.OnClickListener listener = v -> {
            Intent intent = new Intent(context, PublicProfileActivity.class);
            intent.putExtra("targetUserId", post.getUserId());
            context.startActivity(intent);
        };
        holder.imgProfile.setOnClickListener(listener);
        holder.txtUserName.setOnClickListener(listener);
    }

    private void configurarLikesTempoReal(PostViewHolder holder, Post post) {
        db.collection("posts").document(post.getPostId())
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        List<String> likes = (List<String>) snapshot.get("likes");
                        post.setLikes(likes);
                        boolean isLiked = likes != null && likes.contains(currentUserId);
                        int count = (likes != null) ? likes.size() : 0;

                        holder.txtLike.setText((isLiked ? "❤️ " : "🤍 ") + count);
                        holder.txtLike.setTextColor(isLiked ? Color.RED : Color.GRAY);
                    }
                });

        holder.txtLike.setOnClickListener(v -> {
            com.google.firebase.firestore.DocumentReference postRef = db.collection("posts").document(post.getPostId());
            boolean jaDeuLike = post.getLikes() != null && post.getLikes().contains(currentUserId);

            if (jaDeuLike) {
                postRef.update("likes", FieldValue.arrayRemove(currentUserId));
            } else {
                postRef.update("likes", FieldValue.arrayUnion(currentUserId));
                notificationService.sendNotification(post.getUserId(), LIKE, post.getPostId());
            }
        });
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

    private void confirmarExclusao(String postId) {
        new AlertDialog.Builder(context)
                .setTitle("Apagar Post")
                .setMessage("Tens a certeza?")
                .setPositiveButton("Sim", (d, w) -> {
                    db.collection("posts").document(postId).delete()
                            .addOnFailureListener(e -> Toast.makeText(context, "Post apagado", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Não", null).show();
    }

    private void abrirEditor(Post post) {
        Intent i = new Intent(context, CreatePostActivity.class);
        i.putExtra("editPostId", post.getPostId());
        i.putExtra("currentContent", post.getContent());

        if (post.getImagesUrls() != null && !post.getImagesUrls().isEmpty()) {
            i.putStringArrayListExtra("currentImages", new ArrayList<>(post.getImagesUrls()));
        } else if (post.getImageUrl() != null) {
            // Fallback para posts antigos com imagem única
            ArrayList<String> singleImage = new ArrayList<>();
            singleImage.add(post.getImageUrl());
            i.putStringArrayListExtra("currentImages", singleImage);
        }

        context.startActivity(i);
    }

    @Override
    public int getItemCount() { return postList.size(); }

    // --- VIEW HOLDER PRINCIPAL ---
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserName, txtContent, txtDate, txtLike, txtComment;
        ImageView imgProfile;
        ImageButton btnMoreOptions;
        View mediaContainer;
        ViewPager2 viewPager;
        TabLayout tabLayout;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.postUserName);
            txtContent = itemView.findViewById(R.id.postContent);
            txtDate = itemView.findViewById(R.id.postDate);
            txtLike = itemView.findViewById(R.id.postLikeBtn);
            txtComment = itemView.findViewById(R.id.postCommentBtn);
            imgProfile = itemView.findViewById(R.id.postProfileImage);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);

            // Bind dos novos elementos do slide
            mediaContainer = itemView.findViewById(R.id.mediaContainer);
            viewPager = itemView.findViewById(R.id.viewPagerImages);
            tabLayout = itemView.findViewById(R.id.indicator);
        }
    }

    // --- ADAPTER INTERNO PARA O SLIDER DE IMAGENS ---
    private static class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {
        private List<String> imageUrls;

        public ImageSliderAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Usa o layout item_post_image.xml
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_image, parent, false);
            return new SliderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
            String url = imageUrls.get(position);
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .centerCrop()
                    .into(holder.imageView);

            // Clique para abrir em ecrã inteiro
            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), FullScreenImageActivity.class);
                intent.putExtra("imageUrl", url);
                holder.itemView.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return imageUrls.size(); }

        static class SliderViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            public SliderViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.sliderImage);
            }
        }
    }
}