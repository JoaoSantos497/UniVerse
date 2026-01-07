package com.universe;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Layout auxiliar para permitir que um ViewPager2 funcione dentro de outro ViewPager2.
 * Ele interceta o toque e decide se deve bloquear o pai ou não.
 */
public class NestedScrollableHost extends FrameLayout {
    private int touchSlop;
    private float initialX;
    private float initialY;

    public NestedScrollableHost(@NonNull Context context) {
        super(context);
        init();
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    private ViewPager2 getParentViewPager() {
        ViewParent parent = getParent();

        while (parent != null) {
            if (parent instanceof ViewPager2) {
                return (ViewPager2) parent;
            }
            parent = parent.getParent();
        }

        return null;
    }

    private View getChildViewPager() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            if (child instanceof ViewPager2) {
                return child;
            }
        }
        return null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        handleInterceptTouchEvent(e);
        return super.onInterceptTouchEvent(e);
    }

    private void handleInterceptTouchEvent(MotionEvent e) {
        ViewPager2 parentViewPager = getParentViewPager();
        if (parentViewPager == null) return;

        int orientation = parentViewPager.getOrientation();

        // Se não houver filho ou não puder scrolar, não faz nada
        if (getChildViewPager() == null) return;

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            initialX = e.getX();
            initialY = e.getY();
            // No início do toque, pedimos logo para o pai não roubar
            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = e.getX() - initialX;
            float dy = e.getY() - initialY;
            boolean isVpHorizontal = orientation == ViewPager2.ORIENTATION_HORIZONTAL;

            // Lógica:
            // Se o movimento for vertical num pager horizontal -> LIBERTA o pai (scroll da lista)
            // Se o movimento for horizontal -> BLOQUEIA o pai (slide da imagem)
            float scaledDx = Math.abs(dx) * (isVpHorizontal ? .5f : 1f);
            float scaledDy = Math.abs(dy) * (isVpHorizontal ? 1f : .5f);

            if (scaledDx > touchSlop || scaledDy > touchSlop) {
                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // É scroll vertical, deixa a lista rolar
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    // É scroll horizontal, bloqueia o pai (abas) para as imagens rodarem
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        }
    }
}