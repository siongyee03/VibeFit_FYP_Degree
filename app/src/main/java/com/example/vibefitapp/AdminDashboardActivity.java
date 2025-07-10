package com.example.vibefitapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AdminDashboardActivity extends AppCompatActivity {

    private View lineAdmin;
    private TextView tvAdminName, tvAdminRole;
    private ConstraintLayout layoutAdminInfo,layoutManagePosts, layoutManageAdmin, layoutLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminRole = findViewById(R.id.tvAdminRole);

        layoutAdminInfo = findViewById((R.id.layoutAdminInfo));
        layoutManagePosts = findViewById(R.id.layoutManagePosts);
        layoutManageAdmin = findViewById(R.id.layoutManageAdmin);
        layoutLogout = findViewById(R.id.layoutLogout);

        lineAdmin = findViewById(R.id.lineAdmin);

        loadAdminInfo();
        setupClickListeners();
    }

    private void loadAdminInfo() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not registered. Please sign up first.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("username");
                        String role = documentSnapshot.getString("role");

                        tvAdminName.setText(name != null ? name : "Admin");
                        tvAdminRole.setText(role != null ? role : "admin");

                        if (!"superadmin".equalsIgnoreCase(role)) {
                            layoutManageAdmin.setVisibility(View.GONE);
                            lineAdmin.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to load admin information. Please try again later.", Toast.LENGTH_SHORT).show());
    }

    private void setupClickListeners() {
        layoutAdminInfo.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            showEditMyProfileDialog();
        });

        layoutManagePosts.setOnClickListener(v -> startActivity(new Intent(this, ManagePostsActivity.class)));

        layoutManageAdmin.setOnClickListener(v -> startActivity(new Intent(this, ManageAdminActivity.class)));

        layoutLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showEditMyProfileDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentUsername = documentSnapshot.getString("username");
                        String currentEmail = documentSnapshot.getString("email");
                        String currentRole = documentSnapshot.getString("role");

                        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_admin, null);
                        EditText etUsername = view.findViewById(R.id.etUsername);
                        EditText etEmail = view.findViewById(R.id.etEmail);
                        EditText etPassword = view.findViewById(R.id.etPassword);
                        TextView tvRoleLabel = view.findViewById(R.id.tvRoleLabel);
                        Spinner spinnerRole = view.findViewById(R.id.spinnerRole);
                        SwitchCompat switchEnable = view.findViewById(R.id.switchEnable);

                        etUsername.setText(currentUsername);
                        etEmail.setText(currentEmail);
                        etPassword.setHint("Blank = keep password");
                        spinnerRole.setVisibility(View.GONE);
                        tvRoleLabel.setVisibility(View.GONE);
                        switchEnable.setVisibility(View.GONE);

                        new AlertDialog.Builder(this)
                                .setTitle("Edit My Profile")
                                .setView(view)
                                .setPositiveButton("Save", (dialog, which) -> {
                                    String newUsername = etUsername.getText().toString().trim();
                                    String newEmail = etEmail.getText().toString().trim();
                                    String newPassword = etPassword.getText().toString().trim();

                                    if (newUsername.isEmpty() || newEmail.isEmpty()) {
                                        Toast.makeText(this, "Username and Email cannot be empty.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                                        Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (!newPassword.isEmpty() && !isPasswordValid(newPassword)) {
                                        Toast.makeText(this, "Password must be at least 8 characters, contain at least one digit, one lowercase, and one uppercase letter.", Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    if (!newEmail.equals(currentUser.getEmail())) {
                                        showReauthenticateDialog(currentUser, newUsername, newEmail, newPassword);
                                    } else {
                                        updateUserProfile(currentUser, newUsername, newEmail, newPassword, currentRole, false); // Role is not changed
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        Toast.makeText(this, "Could not load your profile data.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showReauthenticateDialog(FirebaseUser user, String newUsername, String newEmail, String newPassword) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Re-authenticate to change Email");
        builder.setMessage("Please enter your current password to confirm the email change.");

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Current Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(40, 0, 40, 0);
        passwordInput.setLayoutParams(params);
        container.addView(passwordInput);

        builder.setView(container);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String currentPassword = passwordInput.getText().toString();
            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Password cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), currentPassword);
            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Authentication successful. Updating profile...", Toast.LENGTH_SHORT).show();
                        updateUserProfile(user, newUsername, newEmail, newPassword, null, true); // Pass true for reauthenticated
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateUserProfile(FirebaseUser user, String newUsername, String newEmail, String newPassword, String currentRole, boolean reauthenticated) {
        Map<String, Object> updates = new HashMap<>();
        boolean firestoreUsernameUpdateNeeded = false;
        boolean authPasswordUpdateNeeded = false;

        if (!newUsername.equals(tvAdminName.getText().toString())) {
            updates.put("username", newUsername);
            firestoreUsernameUpdateNeeded = true;
        }

        if (reauthenticated && !newEmail.equals(user.getEmail())) {
            user.verifyBeforeUpdateEmail(newEmail)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Verification email sent to " + newEmail + ". Please check your inbox to confirm the email change.", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        String errorMessage = "Failed to send verification email: " + e.getMessage();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "Failed to update email: The new email is already in use by another account.";
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    });
        }

        if (!newPassword.isEmpty()) {
            user.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            authPasswordUpdateNeeded = true;
        }

        if (firestoreUsernameUpdateNeeded) {
            db.collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile data updated successfully!", Toast.LENGTH_SHORT).show();
                        loadAdminInfo();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update Firestore data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else if (!authPasswordUpdateNeeded && !firestoreUsernameUpdateNeeded && !reauthenticated) {
            Toast.makeText(this, "No changes detected.", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean isPasswordValid(String password) {
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    }
}
