package com.example.vibefitapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class EditSizesActivity extends AppCompatActivity {

    private EditText etShoulder, etArmLength, etBust, etWaist, etHip, etFootLength;
    private Spinner spinnerMainShape, spinnerBodyShape;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    private String[] mainShapeOptions;
    private String[] bodyShapeOptions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_sizes);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = mAuth.getCurrentUser().getUid();

        // Views
        etShoulder = findViewById(R.id.et_shoulder);
        etArmLength = findViewById(R.id.et_arm_length);
        etBust = findViewById(R.id.et_bust);
        etWaist = findViewById(R.id.et_waist);
        etHip = findViewById(R.id.et_hip);
        etFootLength = findViewById(R.id.et_foot_length);
        spinnerMainShape = findViewById(R.id.spinner_main_shape);
        spinnerBodyShape = findViewById(R.id.spinner_body_shape);

        mainShapeOptions = getResources().getStringArray(R.array.main_shape_simple_options);
        bodyShapeOptions = getResources().getStringArray(R.array.body_shape_detailed_options);

        ImageButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveSizes());

        loadExistingData();
    }

    private void loadExistingData() {
        db.collection("users").document(uid).collection("size_profile").document("detailed")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        etShoulder.setText(document.getString("shoulder"));
                        etArmLength.setText(document.getString("armLength"));
                        etBust.setText(document.getString("bust"));
                        etWaist.setText(document.getString("waist"));
                        etHip.setText(document.getString("hip"));
                        etFootLength.setText(document.getString("footLength"));

                        String mainShape = document.getString("mainShape");
                        String bodyShape = document.getString("bodyShape");

                        if (mainShape != null) {
                            for (int i = 0; i < mainShapeOptions.length; i++) {
                                if (mainShape.equalsIgnoreCase(mainShapeOptions[i])) {
                                    spinnerMainShape.setSelection(i);
                                    break;
                                }
                            }
                        }

                        if (bodyShape != null) {
                            for (int i = 0; i < bodyShapeOptions.length; i++) {
                                if (bodyShape.equalsIgnoreCase(bodyShapeOptions[i])) {
                                    spinnerBodyShape.setSelection(i);
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    private void saveSizes() {
        Map<String, Object> sizeMap = new HashMap<>();

        addIfNotEmpty(sizeMap, "shoulder", etShoulder.getText().toString().trim());
        addIfNotEmpty(sizeMap, "armLength", etArmLength.getText().toString().trim());
        addIfNotEmpty(sizeMap, "bust", etBust.getText().toString().trim());
        addIfNotEmpty(sizeMap, "waist", etWaist.getText().toString().trim());
        addIfNotEmpty(sizeMap, "hip", etHip.getText().toString().trim());
        addIfNotEmpty(sizeMap, "footLength", etFootLength.getText().toString().trim());

        boolean allEmpty = sizeMap.isEmpty();

        sizeMap.put("mainShape", spinnerMainShape.getSelectedItem().toString());
        sizeMap.put("bodyShape", spinnerBodyShape.getSelectedItem().toString());

        if (allEmpty) {
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

    private void addIfNotEmpty(Map<String, Object> map, String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            map.put(key, value);
        }
    }
}