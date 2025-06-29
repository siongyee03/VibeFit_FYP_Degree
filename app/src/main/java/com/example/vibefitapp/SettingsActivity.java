package com.example.vibefitapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private ConstraintLayout  layoutLogout;
    private ImageView imgLogoutIcon;
    private TextView tvLogout;

    private ConstraintLayout layoutChangePassword;

    private ImageButton btnBack;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        firebaseAuth = FirebaseAuth.getInstance();

        layoutLogout = findViewById(R.id.layoutLogout);
        imgLogoutIcon = findViewById(R.id.imgLogoutIcon);
        tvLogout = findViewById(R.id.tvLogout);

        layoutChangePassword = findViewById(R.id.layoutChangePassword);

        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        updateLogoutUI();

        layoutLogout.setOnClickListener(v -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                // Logout
                firebaseAuth.signOut();
                Toast.makeText(SettingsActivity.this, R.string.logged_out, Toast.LENGTH_SHORT).show();
                updateLogoutUI();
            } else {
                // Login/Register
                Intent intent = new Intent(SettingsActivity.this, EntryActivity.class);
                startActivity(intent);
            }
        });

        layoutChangePassword.setOnClickListener(v -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(SettingsActivity.this, R.string.please_log_in_first, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLogoutUI() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            tvLogout.setText(R.string.logout);
            imgLogoutIcon.setImageResource(R.drawable.ic_logout_door);
        } else {
            tvLogout.setText(R.string.login_register);
            imgLogoutIcon.setImageResource(R.drawable.ic_login);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLogoutUI();
    }
}