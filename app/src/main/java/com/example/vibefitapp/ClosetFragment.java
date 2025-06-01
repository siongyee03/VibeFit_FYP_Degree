package com.example.vibefitapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Locale;

public class ClosetFragment extends Fragment {

    private ScrollView scrollView;

    // A-card views
    private ImageView userAvatar;
    private ImageView genderIcon;
    private TextView genderText;
    private TextView heightData;
    private TextView weightData;
    private TextView shoeSizeData;
    private TextView braSizeData;
    private ImageButton editButton;
    private ImageButton deleteButton;

    // B-card views
    private TextView shoulderData;
    private TextView armLengthData;
    private TextView bustData;
    private TextView waistData;
    private TextView hipData;
    private TextView footLengthData;
    private LinearLayout editSizeButton;

    // AI Try On button
    private Button aiTryOnButton;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_closet, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Bind views
        scrollView = view.findViewById(R.id.scrollView);

        userAvatar = view.findViewById(R.id.userAvatar);
        genderIcon = view.findViewById(R.id.genderIcon);
        genderText = view.findViewById(R.id.genderText);

        heightData = view.findViewById(R.id.heightText);
        weightData = view.findViewById(R.id.weightText);
        shoeSizeData = view.findViewById(R.id.shoeSizeText);
        braSizeData = view.findViewById(R.id.braSizeText);

        editButton = view.findViewById(R.id.editButton);
        deleteButton = view.findViewById(R.id.deleteButton);

        shoulderData = view.findViewById(R.id.shoulderData);
        armLengthData = view.findViewById(R.id.armLengthData);
        bustData = view.findViewById(R.id.bustData);
        waistData = view.findViewById(R.id.waistData);
        hipData = view.findViewById(R.id.hipData);
        footLengthData = view.findViewById(R.id.footLengthData);

        editSizeButton = view.findViewById(R.id.editSizeButton);
        aiTryOnButton = view.findViewById(R.id.aiTryOnButton);

        // Setup listeners
        editButton.setOnClickListener(v -> openEditProfile());
        deleteButton.setOnClickListener(v -> deleteProfileData());
        editSizeButton.setOnClickListener(v -> openEditSizes());
        aiTryOnButton.setOnClickListener(v -> openAiTryOn());

        loadUserData();

        return view;
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // Not logged in, show placeholder or prompt login
            showLoginRequiredDialog();

            genderText.setText("-");
            heightData.setText("-");
            weightData.setText("-");
            shoeSizeData.setText("-");
            braSizeData.setText("-");

            shoulderData.setText("-");
            armLengthData.setText("-");
            bustData.setText("-");
            waistData.setText("-");
            hipData.setText("-");
            footLengthData.setText("-");

            return;
        }

        // Load avatar from Firebase Storage, path e.g. "avatars/{uid}.jpg"
        StorageReference avatarRef = storage.getReference().child("avatars/" + user.getUid() + ".jpg");
        avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Use Glide or similar to load image
            Glide.with(requireContext()).load(uri).into(userAvatar);
        }).addOnFailureListener(e -> {
            // Load default avatar on failure
            userAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        });

        // Load user document from Firestore (collection "users", doc uid)
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String gender = documentSnapshot.getString("gender");
                        setGenderUI(gender);

                        // a-card: height, weight, shoeSize, braSize
                        Number height = documentSnapshot.getDouble("height");
                        Number weight = documentSnapshot.getDouble("weight");
                        Number shoeSize = documentSnapshot.getDouble("shoeSize");
                        Number braSize = documentSnapshot.getDouble("braSize");

                        heightData.setText(height != null ? String.format(Locale.US, "%.0f cm", height.doubleValue()) : "-");
                        weightData.setText(weight != null ? String.format(Locale.US, "%.0f kg", weight.doubleValue()) : "-");
                        shoeSizeData.setText(shoeSize != null ? String.format(Locale.US, "%.0f", shoeSize.doubleValue()) : "-");
                        braSizeData.setText(braSize != null ? String.format(Locale.US, "%.0f", braSize.doubleValue()) : "-");

                        // b-card: shoulderWidth, armLength, bust, waist, hip, footLength
                        Number shoulder = documentSnapshot.getDouble("shoulderWidth");
                        Number armLength = documentSnapshot.getDouble("armLength");
                        Number bust = documentSnapshot.getDouble("bust");
                        Number waist = documentSnapshot.getDouble("waist");
                        Number hip = documentSnapshot.getDouble("hip");
                        Number footLength = documentSnapshot.getDouble("footLength");

                        shoulderData.setText(shoulder != null ? String.format(Locale.US, "%.0f cm", shoulder.doubleValue()) : "-");
                        armLengthData.setText(armLength != null ? String.format(Locale.US, "%.0f cm", armLength.doubleValue()) : "-");
                        bustData.setText(bust != null ? String.format(Locale.US, "%.0f cm", bust.doubleValue()) : "-");
                        waistData.setText(waist != null ? String.format(Locale.US, "%.0f cm", waist.doubleValue()) : "-");
                        hipData.setText(hip != null ? String.format(Locale.US, "%.0f cm", hip.doubleValue()) : "-");
                        footLengthData.setText(footLength != null ? String.format(Locale.US, "%.0f cm", footLength.doubleValue()) : "-");

                    } else {
                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show());
    }

    private void setGenderUI(String gender) {
        if (gender == null || gender.isEmpty()) {
            gender = "unknown";
        }
        genderText.setText(getString(R.string.me));

        switch (gender.toLowerCase()) {
            case "male":
                genderIcon.setImageResource(R.drawable.ic_male_symbol);
                break;
            case "female":
                genderIcon.setImageResource(R.drawable.ic_female_symbol); // your female icon drawable
                break;
            default:
                genderIcon.setImageResource(R.drawable.ic_unknown); // unknown icon
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

        firestore.collection("users").document(user.getUid())
                .update(
                        "height", null,
                        "weight", null,
                        "shoeSize", null,
                        "braSize", null,
                        "shoulderWidth", null,
                        "armLength", null,
                        "bust", null,
                        "waist", null,
                        "hip", null,
                        "footLength", null)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Size Profile data deleted", Toast.LENGTH_SHORT).show();
                    loadUserData();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to delete data", Toast.LENGTH_SHORT).show());
    }

    private void openEditSizes() {
        // Open Edit Sizes Activity (for b-card)
        Intent intent = new Intent(requireContext(), EditSizesActivity.class);
        startActivity(intent);
    }

    private void openAiTryOn() {
        // Open AI Try On Activity
        Intent intent = new Intent(requireContext(), AiTryOnActivity.class);
        startActivity(intent);
    }

    private void showLoginRequiredDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.login_required_title)
                .setMessage(R.string.login_required_message)
                .setCancelable(false)
                .setPositiveButton(R.string.login, (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}