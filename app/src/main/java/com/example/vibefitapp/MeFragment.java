package com.example.vibefitapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MeFragment extends Fragment {

    private ImageView imgUserAvatar;
    private TextView tvUsername;
    private ConstraintLayout cardUserInfo, cardFavourite, cardSettings, cardPosts;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseAuth firebaseAuth;
    private ListenerRegistration userListener;

    public MeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_me, container, false);

        firebaseAuth = FirebaseAuth.getInstance();

        imgUserAvatar = view.findViewById(R.id.imgUserAvatar);
        tvUsername = view.findViewById(R.id.tvUsername);

        cardUserInfo = view.findViewById(R.id.cardUserInfo);
        cardFavourite = view.findViewById(R.id.cardFavourite);
        cardSettings = view.findViewById(R.id.cardSettings);
        cardPosts = view.findViewById(R.id.cardPosts);

        setupUserInfoListener();
        setupCardsClick();

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshMe);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            setupUserInfoListener();
            swipeRefreshLayout.setRefreshing(false);
        });

        return view;
    }

    private void setupUserInfoListener() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String imageUrl = documentSnapshot.getString("profileImageUrl");
                            String username = documentSnapshot.getString("username");

                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                if (isAdded()) {
                                    Glide.with(requireContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.ic_avatar_placeholder)
                                            .circleCrop()
                                            .into(imgUserAvatar);
                                }
                            } else {
                                imgUserAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                            }

                            if (username != null && !username.isEmpty()) {
                                tvUsername.setText(username);
                            } else {
                                tvUsername.setText(R.string.anonymous);
                            }
                        } else {
                            showDefaultUserInfo();
                        }
                    })
                    .addOnFailureListener(e -> showDefaultUserInfo());
        } else {
            showDefaultUserInfo();
        }
    }

    private void showDefaultUserInfo() {
        imgUserAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        tvUsername.setText(R.string.anonymous);
    }

    private void setupCardsClick() {
        cardUserInfo.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() != null) {
                Intent intent = new Intent(requireContext(), UserProfileActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), R.string.please_log_in_first, Toast.LENGTH_SHORT).show();
            }
        });

        cardFavourite.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() != null) {
                Intent intent = new Intent(requireContext(), FavouritePostsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), R.string.please_log_in_first, Toast.LENGTH_SHORT).show();
            }
        });

        cardPosts.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() != null) {
                Intent intent = new Intent(requireContext(), UserPostsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), R.string.please_log_in_first, Toast.LENGTH_SHORT).show();
            }
        });

        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupUserInfoListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
}
