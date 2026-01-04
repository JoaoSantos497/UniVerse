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

    // Componentes de UI
    private FloatingActionButton fabCreatePost;
    private ImageButton btnNotifications;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View notificationBadge; // Mapeia para o MaterialCardView no XML

    // Firebase e Serviços
    private ListenerRegistration badgeListener;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FeedPagerAdapter pagerAdapter;
    private NotificationService notificationService;
    private UserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Certifica-te que o nome do XML é 'fragment_home.xml'
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Inicializar Firebase e Serviços
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userService = new UserService();
        notificationService = new NotificationService(userService);

        // 2. Inicializar componentes (IDs correspondem ao novo XML)
        fabCreatePost = view.findViewById(R.id.fabCreatePost);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        tabLayout = view.findViewById(R.id.tabLayoutFeed);
        viewPager = view.findViewById(R.id.viewPagerFeed);

        // O badge agora é um MaterialCardView no XML, mas podemos tratá-lo como View genérica
        // pois só queremos mudar a visibilidade (VISIBLE/GONE)
        notificationBadge = view.findViewById(R.id.notificationBadge);

        // 3. Configurar o ViewPager2
        // 'this' passa o Fragment atual como gestor do ciclo de vida
        pagerAdapter = new FeedPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Mantém 1 página em memória de cada lado para melhorar a performance do scroll
        viewPager.setOffscreenPageLimit(1);

        // 4. Configurar TabLayout com o ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Global");
            } else {
                tab.setText("Minha Uni");
            }
        }).attach();

        // 5. Configurar Listeners de Clique
        fabCreatePost.setOnClickListener(v -> {
            // Verificação de segurança para evitar crash se a activity não existir
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), CreatePostActivity.class);
                // Passamos a tab atual para o post ser criado no contexto certo (Global vs Uni)
                int currentTab = viewPager.getCurrentItem();
                intent.putExtra("selectedTab", currentTab);
                startActivity(intent);
            }
        });

        btnNotifications.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), NotificationsActivity.class);
                startActivity(intent);
            }
        });

        // 6. Iniciar escuta de notificações (Badge Vermelho)
        startUnreadListener();

        return view;
    }

    private void startUnreadListener() {
        if (mAuth.getCurrentUser() == null) return;

        // Remove listener anterior para evitar duplicados
        if (badgeListener != null) {
            badgeListener.remove();
        }

        // Escuta em tempo real alterações na coleção de notificações
        badgeListener = notificationService.listenForUnreadNotifications(
                mAuth.getCurrentUser().getUid(),
                new DataListener<Boolean>() {
                    @Override
                    public void onData(Boolean hasUnread) {
                        if (notificationBadge == null) return;

                        // Mostra ou esconde o CardView vermelho
                        notificationBadge.setVisibility(
                                hasUnread ? View.VISIBLE : View.GONE
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        // Opcional: Logar erro
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        // Garante que a badge atualiza ao voltar para a home (ex: depois de ler notificações)
        startUnreadListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove o listener para poupar bateria e dados quando o fragmento é destruído
        if (badgeListener != null) {
            badgeListener.remove();
            badgeListener = null;
        }
    }
}