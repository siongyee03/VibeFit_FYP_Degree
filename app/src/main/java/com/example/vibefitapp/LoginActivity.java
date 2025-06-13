package com.example.vibefitapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

//login authentication
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;

import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText loginEmail, loginPass;
    private ImageView passwordToggle;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        ImageButton backButton = findViewById(R.id.backButton);
        loginEmail = findViewById(R.id.loginEmail);
        loginPass = findViewById(R.id.loginPass);
        passwordToggle = findViewById(R.id.passwordToggle);
        Button loginButton = findViewById(R.id.loginButton);
        TextView forgotPassword = findViewById(R.id.forgotPassword);
        TextView registerNow = findViewById(R.id.registerNow);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //FireStore
        db = FirebaseFirestore.getInstance();

        // Back Button Click Listener
        backButton.setOnClickListener(v -> {
            finish(); // return to previous page
        });

        // Password Toggle (Show/Hide Password)
        passwordToggle.setOnClickListener(v -> {
            if (loginPass.getTransformationMethod() instanceof PasswordTransformationMethod) {
                // Show password
                loginPass.setTransformationMethod(null);
                passwordToggle.setImageResource(R.drawable.ic_eye);
            } else {
                // Hide password
                loginPass.setTransformationMethod(new PasswordTransformationMethod());
                passwordToggle.setImageResource(R.drawable.ic_eye_off);
            }
        });

        // Login Button Click Listener
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString();
            String password = loginPass.getText().toString();

            if (email.isEmpty()) {
                loginEmail.setError("Email is required");
                loginEmail.requestFocus(); // Request focus to the email field
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                loginEmail.setError("Please enter a valid email");
                loginEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                loginPass.setError("Password is required");
                loginPass.requestFocus();
                return;
            }

            if (password.length() < 8) { // Minimum password length (adjust as needed)
                loginPass.setError("Minimum password length is 8 characters");
                loginPass.requestFocus();
                return;
            }

            signInUser(email, password);

        });

        forgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        registerNow.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            // User is already signed in and email is verified
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish(); // Close LoginActivity
        } else {

            mAuth.signOut(); //Sign out user, prompt to verify again
        }
    }

    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null && user.isEmailVerified()) {
                            db.collection("users").document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String role = documentSnapshot.getString("role");
                                            if (role != null) {
                                                Intent intent;
                                                if (role.equals("admin") || role.equals("superadmin")) {
                                                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                                } else {
                                                    intent = new Intent(LoginActivity.this, HomeActivity.class);
                                                }
                                                Toast.makeText(LoginActivity.this, "Logged in successfully.", Toast.LENGTH_SHORT).show();
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(this, "Account setup incomplete. Please reach out to support.", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(this, "Account not found. Please check your login info.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch user role: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            loginEmail.setError("Please verify your email address");
                            loginEmail.requestFocus();

                            if (user != null) {
                                user.sendEmailVerification().addOnCompleteListener(sendTask -> {
                                    if (sendTask.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this, "Verification email sent again. Please check your inbox.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Failed to send verification email. Please try again.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(LoginActivity.this, "Unable to resend verification email at this time. Please contact support if the issue continues.", Toast.LENGTH_SHORT).show();
                            }
                        }

                    } else {
                        Exception e = task.getException();
                        if (e != null) {
                            Log.w("LoginActivity", "signInWithEmail:failure", e);
                            loginEmail.setError("Authentication failed: " + e.getMessage());
                            loginEmail.requestFocus();
                        }
                    }
                });
    }
}

