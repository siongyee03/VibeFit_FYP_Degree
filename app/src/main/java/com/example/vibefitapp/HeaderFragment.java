package com.example.vibefitapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

public class HeaderFragment extends Fragment {

    private TabLayout tabLayout;

    public HeaderFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_header, container, false);

        tabLayout = view.findViewById(R.id.tab_bar);

        setupTabs();

        String targetTab = getArguments() != null ? getArguments().getString("target_tab") : null;
        if (targetTab != null) {
            TabLayout.Tab tabToSelect = null;
            switch (targetTab.toLowerCase()) {
                case "forum":
                    tabToSelect = tabLayout.getTabAt(0);
                    break;
                case "explore":
                    tabToSelect = tabLayout.getTabAt(1);
                    break;
                case "trends":
                    tabToSelect = tabLayout.getTabAt(2);
                    break;
            }
            if (tabToSelect != null) {
                tabToSelect.select();
            }
        }

        return view;
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Fragment selectedFragment = null;
                String selectedCategory = "tutorial/pattern";

                switch (position) {
                    case 0:
                        selectedFragment = new ForumFragment();
                        selectedCategory = "forum";
                        break;
                    case 1:
                        selectedCategory = "tutorial/pattern";
                        selectedFragment = new ExploreFragment();
                        break;
                    case 2:
                        selectedFragment = new TrendsFragment();
                        selectedCategory = "trends";
                        break;
                }

                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).setCurrentHeaderTab(selectedCategory);
                }

                if (selectedFragment != null && getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, selectedFragment)
                            .commit();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // The first tab is selected by default.
        TabLayout.Tab defaultTab = tabLayout.getTabAt(1);
        if (defaultTab != null) defaultTab.select();
    }
}
