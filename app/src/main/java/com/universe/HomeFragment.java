package com.universe;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class HomeFragment extends Fragment {

    private FloatingActionButton fabCreatePost;
    private ImageButton btnNotifications;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View notificationBadge;

    private ListenerRegistration badgeListener;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FeedPagerAdapter pagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 1. Inicializar componentes
        fabCreatePost = view.findViewById(R.id.fabCreatePost);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        tabLayout = view.findViewById(R.id.tabLayoutFeed);
        viewPager = view.findViewById(R.id.viewPagerFeed);
        notificationBadge = view.findViewById(R.id.notificationBadge);

        // 2. Configurar o ViewPager2
        // UsamosgetChildFragmentManager() para fragmentos dentro de fragmentos
        pagerAdapter = new FeedPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Evita que os fragmentos sejam recriados ao deslizar (melhora performance)
        viewPager.setOffscreenPageLimit(1);

        // 3. Configurar TabLayout com títulos
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Global");
            } else {
                tab.setText("Minha Uni");
            }
        }).attach();

        // 4. Listeners
        fabCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            int currentTab = viewPager.getCurrentItem();
            intent.putExtra("selectedTab", currentTab);
            startActivity(intent);
        });

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(intent);
        });

        // 5. Iniciar escuta de notificações
        verificarNotificacoesNaoLidas();

        return view;
    }

    private void verificarNotificacoesNaoLidas() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // Remove listener anterior se existir para evitar fugas de memória
        if (badgeListener != null) badgeListener.remove();

        badgeListener = db.collection("notifications")
                .whereEqualTo("targetUserId", myId)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || notificationBadge == null) return;

                    if (value != null && !value.isEmpty()) {
                        notificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Garante que a badge atualiza ao voltar para a home
        verificarNotificacoesNaoLidas();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpeza crucial de listeners do Firebase
        if (badgeListener != null) {
            badgeListener.remove();
            badgeListener = null;
        }
    }
}