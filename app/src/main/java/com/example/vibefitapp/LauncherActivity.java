package com.example.vibefitapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LauncherActivity extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null) {
                            if (role.equals("admin") || role.equals("superadmin")) {
                                startActivity(new Intent(this, AdminDashboardActivity.class));
                            } else {
                                startActivity(new Intent(this, HomeActivity.class));
                            }
                        } else {
                            startActivity(new Intent(this, HomeActivity.class));
                        }
                    } else {startActivity(new Intent(this, HomeActivity.class));}
                    finish();
                })
                .addOnFailureListener(e -> {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                });
    }
}

