package com.example.vibefitapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ClosetFragment extends Fragment {

    private ScrollView scrollView;

    // A-card views
    private ImageView userAvatar;
    private ImageView genderIcon;
    private TextView genderText;
    private TextView heightData;
    private TextView weightData;
    private TextView shoeSizeData;
    private TextView braSizeData,braSizeText;

    // B-card views
    private TextView shoulderData;
    private TextView armLengthData;
    private TextView bustData;
    private TextView waistData;
    private TextView hipData;
    private TextView footLengthData,bodyShapeData,bodyShapeDetailedData;
    private LinearLayout editSizeButton;

    // AI Try On button
    private Button aiTryOnButton;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private boolean hasShownLoginDialog = false;
    private boolean hasShownMissingSizeToast = false;


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_closet, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Bind views
        scrollView = view.findViewById(R.id.scrollView);

        userAvatar = view.findViewById(R.id.userAvatar);
        genderIcon = view.findViewById(R.id.genderIcon);
        genderText = view.findViewById(R.id.genderText);

        heightData = view.findViewById(R.id.heightText);
        weightData = view.findViewById(R.id.weightText);
        shoeSizeData = view.findViewById(R.id.shoeSizeText);
        braSizeData = view.findViewById(R.id.braSizeText);
        braSizeText = view.findViewById(R.id.braCuptext);

        shoulderData = view.findViewById(R.id.shoulderData);
        armLengthData = view.findViewById(R.id.armLengthData);
        bustData = view.findViewById(R.id.bustData);
        waistData = view.findViewById(R.id.waistData);
        hipData = view.findViewById(R.id.hipData);
        bodyShapeData = view.findViewById(R.id.bodyShapeData);
        bodyShapeDetailedData = view.findViewById(R.id.bodyShapeDetailedData);
        footLengthData = view.findViewById(R.id.footLengthData);

        editSizeButton = view.findViewById(R.id.editSizeButton);
        aiTryOnButton = view.findViewById(R.id.aiTryOnButton);

        LinearLayout editContainer = view.findViewById(R.id.editContainer);
        LinearLayout deleteContainer = view.findViewById(R.id.deleteContainer);

        // Setup listeners
        editContainer.setOnClickListener(v -> openEditProfile());
        deleteContainer.setOnClickListener(v -> deleteProfileData());
        editSizeButton.setOnClickListener(v -> openEditSizes());
        aiTryOnButton.setOnClickListener(v -> openAiTryOn());

        loadUserData();

        return view;
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            if (!hasShownLoginDialog) {
                showLoginRequiredDialog();
                hasShownLoginDialog = true;
            }

            genderText.setText("--");
            heightData.setText("--");
            weightData.setText("--");
            shoeSizeData.setText("--");
            braSizeData.setText("--");

            shoulderData.setText("--");
            armLengthData.setText("--");
            bustData.setText("--");
            waistData.setText("--");
            hipData.setText("--");
            bodyShapeData.setText("--");
            bodyShapeDetailedData.setText("--");
            footLengthData.setText("--");

            userAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }
        hasShownLoginDialog = false;

        firestore.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!isAdded()) return;

                    if (userSnapshot.exists()) {
                        String avatarUrl = userSnapshot.getString("profileImageUrl");
                        String gender = userSnapshot.getString("gender");
                        setGenderUI(gender);

                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(avatarUrl)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(userAvatar);
                        } else {
                            Uri photoUrl = firebaseUser.getPhotoUrl();
                            if (photoUrl != null) {
                                Glide.with(requireContext())
                                        .load(photoUrl)
                                        .apply(RequestOptions.circleCropTransform())
                                        .into(userAvatar);
                            } else {
                                userAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                            }
                        }
                    } else {
                        userAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!hasShownMissingSizeToast) {
                        Toast.makeText(requireContext(), getString(R.string.add_size_profile), Toast.LENGTH_SHORT).show();
                        hasShownMissingSizeToast = true;
                    }
                    userAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                });

        firestore.collection("users")
                .document(firebaseUser.getUid())
                .collection("size_profile")
                .document("detailed")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        heightData.setText("--");
                        weightData.setText("--");
                        shoeSizeData.setText("--");
                        braSizeData.setText("--");

                        shoulderData.setText("--");
                        armLengthData.setText("--");
                        bustData.setText("--");
                        waistData.setText("--");
                        hipData.setText("--");
                        bodyShapeData.setText("--");
                        bodyShapeDetailedData.setText("--");
                        footLengthData.setText("--");
                        return;
                    }

                    String bandSize = documentSnapshot.getString("braBandSize");
                    String cupSize = documentSnapshot.getString("braCup");

                    if ((bandSize != null && !bandSize.trim().isEmpty()) || (cupSize != null && !cupSize.trim().isEmpty())) {
                        String displayBraSize = (bandSize != null ? bandSize : "") + (cupSize != null ? cupSize : "");
                        braSizeData.setText(displayBraSize);
                    } else {
                        braSizeData.setText("--");
                    }

                    heightData.setText(formatWithUnit(documentSnapshot.getString("height"), "Cm"));
                    weightData.setText(formatWithUnit(documentSnapshot.getString("weight"), "Kg"));
                    shoeSizeData.setText(formatPlain(documentSnapshot.getString("shoeSize")));

                    shoulderData.setText(formatWithUnit(documentSnapshot.getString("shoulder"), "Cm"));
                    armLengthData.setText(formatWithUnit(documentSnapshot.getString("armLength"), "Cm"));
                    bustData.setText(formatWithUnit(documentSnapshot.getString("bust"), "Cm"));
                    waistData.setText(formatWithUnit(documentSnapshot.getString("waist"), "Cm"));
                    hipData.setText(formatWithUnit(documentSnapshot.getString("hip"), "Cm"));
                    bodyShapeData.setText(formatPlain(documentSnapshot.getString("bodyShape")));
                    bodyShapeDetailedData.setText(formatPlain(documentSnapshot.getString("mainShape")));
                    footLengthData.setText(formatWithUnit(documentSnapshot.getString("footLength"), "Cm"));

                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), getString(R.string.add_size_profile), Toast.LENGTH_SHORT).show()
                );
    }

    private String formatWithUnit(String value, String unit) {
        return (value != null && !value.trim().isEmpty()) ? value + " " + unit : "--";
    }

    private String formatPlain(String value) {
        return (value != null && !value.trim().isEmpty()) ? value : "--";
    }

    private void setGenderUI(String gender) {
        if (!isAdded()) return;

        if (gender == null || gender.isEmpty()) {
            gender = "unknown";
        }

        switch (gender.toLowerCase()) {
            case "male":
                genderIcon.setImageResource(R.drawable.ic_male_symbol);
                genderText.setText(getString(R.string.male));
                braSizeData.setVisibility(View.GONE);
                braSizeText.setVisibility(View.GONE);
                break;
            case "female":
                genderIcon.setImageResource(R.drawable.ic_female_symbol);
                genderText.setText(getString(R.string.female));
                braSizeData.setVisibility(View.VISIBLE);
                braSizeText.setVisibility(View.VISIBLE);
                break;
            default:
                genderIcon.setImageResource(R.drawable.ic_unknown);
                genderText.setText(getString(R.string.unknown));
                braSizeData.setVisibility(View.GONE);
                braSizeText.setVisibility(View.GONE);
                break;
        }
    }

    private void openEditProfile() {
        Intent intent = new Intent(requireContext(), EditSizeProfile.class);
        startActivity(intent);
    }

    private void deleteProfileData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        firestore.collection("users").document(user.getUid()).collection("size_profile").document("detailed")
                .update(
                        "height", null,
                        "weight", null,
                        "shoeSize", null,
                        "braBandSize", null,
                        "braCup", null,
                        "shoulder", null,
                        "armLength", null,
                        "bust", null,
                        "waist", null,
                        "hip", null,
                        "mainShape",null,
                        "bodyShape",null,
                        "footLength", null)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), getString(R.string.size_profile_deleted), Toast.LENGTH_SHORT).show();
                    loadUserData();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), getString(R.string.size_profile_delete_failed), Toast.LENGTH_SHORT).show());
    }

    private void openEditSizes() {
        Intent intent = new Intent(requireContext(), EditSizesActivity.class);
        startActivity(intent);
    }

    private void openAiTryOn() {
        if (auth.getCurrentUser() == null) {
            showLoginRequiredDialog();
            return;
        }

        Intent intent = new Intent(requireContext(), AiTryOnActivity.class);
        startActivity(intent);
    }

    private void showLoginRequiredDialog() {
        Context context = getContext();
        if (context == null) return;

        new AlertDialog.Builder(context)
                .setTitle(R.string.login_required_title)
                .setMessage(R.string.login_required_message)
                .setCancelable(false)
                .setPositiveButton(R.string.login, (dialog, which) -> {
                    Intent intent = new Intent(context, LoginActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        hasShownMissingSizeToast = false;
        loadUserData();
    }

}