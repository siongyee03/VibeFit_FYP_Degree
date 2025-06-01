package com.example.vibefitapp;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

public class HeaderFragment extends Fragment {

    private AutoCompleteTextView searchEditText;
    private TabLayout tabLayout;
    private ImageView notificationIcon;

    // search suggestions
    private final String[] searchSuggestions = {
            "Summer Dress", "Crop Top", "Pattern", "Denim Jacket", "Midi Skirt", "Street Style", "Knitting", "DIY", "Boho Look"
    };

    public HeaderFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_header, container, false);

        searchEditText = view.findViewById(R.id.search_edit_text);
        tabLayout = view.findViewById(R.id.tab_bar);
        notificationIcon = view.findViewById(R.id.notification_icon);

        setupSearchSuggestions();
        setupSearchAction();
        setupTabs();

        return view;
    }

    private void setupSearchSuggestions() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                searchSuggestions
        );
        searchEditText.setAdapter(adapter);
        searchEditText.setThreshold(0); // show search suggestion
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                searchEditText.showDropDown();
            }
        });

        // click on a suggestion item
        searchEditText.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            Toast.makeText(requireContext(), "You selected: " + selected, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSearchAction() {
        searchEditText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                String query = searchEditText.getText().toString();
                if (!query.isEmpty()) {
                    Toast.makeText(requireContext(), "Searching for: " + query, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Showing suggestions...", Toast.LENGTH_SHORT).show();
                    searchEditText.showDropDown();
                }

                // Auto close keyboard
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                }

                return true;
            }
            return false;
        });
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
                        selectedFragment = new ExploreFragment();
                        selectedCategory = "tutorial/pattern";
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
