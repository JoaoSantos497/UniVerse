package com.universe;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FeedPagerAdapter extends FragmentStateAdapter {

    public FeedPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return FeedTabFragment.newInstance("global");
        } else {
            return FeedTabFragment.newInstance("uni");
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Temos 2 abas
    }
}