package com.example.vibefitapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class EditSizeProfile extends AppCompatActivity {

    private EditText etHeight, etWeight, etShoeSize;
    private Spinner spinnerBraBandSize, spinnerBraCup;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;
    private String userGender = "";

    // Bra band sizes underbust options
    private final String[] braBandOptions = {"70", "75", "80", "85", "90", "95", "100"};
    // Bra cup options A-G uppercase
    private final String[] braCupOptions = {"A", "B", "C", "D", "E", "F", "G"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_size_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = mAuth.getCurrentUser().getUid();

        // Views
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etShoeSize = findViewById(R.id.et_shoe_size);
        spinnerBraBandSize = findViewById(R.id.spinner_bra_band_size);
        spinnerBraCup = findViewById(R.id.spinner_bra_cup);

        ImageButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        btnBack.setOnClickListener(v -> finish());

        ArrayAdapter<String> bandAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, braBandOptions);
        bandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBraBandSize.setAdapter(bandAdapter);

        ArrayAdapter<String> cupAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, braCupOptions);
        cupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBraCup.setAdapter(cupAdapter);

        btnSave.setOnClickListener(v -> saveSizeProfile());

        loadUserGenderAndControlBraFields();
    }

    private void loadUserGenderAndControlBraFields() {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String gender = document.getString("gender");
                        if (!"female".equalsIgnoreCase(gender)) {
                            spinnerBraBandSize.setVisibility(View.GONE);
                            spinnerBraCup.setVisibility(View.GONE);
                            findViewById(R.id.braCupSize).setVisibility(View.GONE);
                        }
                        userGender = gender;
                    }
                    loadExistingData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load gender info", Toast.LENGTH_SHORT).show();
                    loadExistingData();
                });
    }

    private void loadExistingData() {
        db.collection("users").document(uid).collection("size_profile").document("detailed")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String height = document.getString("height");
                        String weight = document.getString("weight");
                        String shoeSize = document.getString("shoeSize");
                        String braBandSize = document.getString("braBandSize");
                        String braCup = document.getString("braCup");

                        if (!TextUtils.isEmpty(height)) etHeight.setText(height);
                        if (!TextUtils.isEmpty(weight)) etWeight.setText(weight);
                        if (!TextUtils.isEmpty(shoeSize)) etShoeSize.setText(shoeSize);

                        if (!TextUtils.isEmpty(braBandSize)) {
                            int pos = Arrays.asList(braBandOptions).indexOf(braBandSize);
                            if (pos >= 0) spinnerBraBandSize.setSelection(pos);
                        }

                        if (!TextUtils.isEmpty(braCup)) {
                            int pos = Arrays.asList(braCupOptions).indexOf(braCup.toUpperCase());
                            if (pos >= 0) spinnerBraCup.setSelection(pos);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load size profile", Toast.LENGTH_SHORT).show());
    }

    private void saveSizeProfile() {
        Map<String, Object> sizeMap = new HashMap<>();

        String height = etHeight.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();
        String shoeSize = etShoeSize.getText().toString().trim();

        if (!TextUtils.isEmpty(height)) sizeMap.put("height", height);
        if (!TextUtils.isEmpty(weight)) sizeMap.put("weight", weight);
        if (!TextUtils.isEmpty(shoeSize)) sizeMap.put("shoeSize", shoeSize);

        if ("female".equalsIgnoreCase(userGender)) {
            sizeMap.put("braBandSize", spinnerBraBandSize.getSelectedItem().toString());
            sizeMap.put("braCup", spinnerBraCup.getSelectedItem().toString());
        }

        if (TextUtils.isEmpty(height) && TextUtils.isEmpty(weight) && TextUtils.isEmpty(shoeSize)) {
            Toast.makeText(this, "No size data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(uid)
                .collection("size_profile").document("detailed")
                .set(sizeMap, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Size profile saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
