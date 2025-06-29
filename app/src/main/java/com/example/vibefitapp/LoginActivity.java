package com.example.vibefitapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

//login authentication
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import android.widget.Toast;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private EditText loginEmail, loginPass;
    private ImageView passwordToggle;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String registeredEmail = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ImageButton backButton = findViewById(R.id.backButton);
        loginEmail = findViewById(R.id.loginEmail);
        loginPass = findViewById(R.id.loginPass);
        passwordToggle = findViewById(R.id.passwordToggle);
        Button loginButton = findViewById(R.id.loginButton);
        TextView forgotPassword = findViewById(R.id.forgotPassword);
        TextView registerNow = findViewById(R.id.registerNow);
        ImageView logoImage = findViewById(R.id.logoImage);

        forgotPassword.setText(fromHtmlCompat("<u>Forgot Password?</u>"));
        registerNow.setText(fromHtmlCompat("<u>Register Now</u>"));

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

        loginPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (loginPass.getError() != null) {
                    passwordToggle.setVisibility(View.GONE);
                } else {
                    passwordToggle.setVisibility(View.VISIBLE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString();
            String password = loginPass.getText().toString();

            if (email.isEmpty()) {
                loginEmail.setError("Email is required");
                loginEmail.requestFocus();
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
                passwordToggle.setVisibility(View.GONE);
                return;
            }

            if (password.length() < 8) {
                loginPass.setError("Minimum password length is 8 characters");
                loginPass.requestFocus();
                passwordToggle.setVisibility(View.GONE);
                return;
            }

            signInUser(email, password);

        });

        forgotPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        registerNow.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        logoImage.setOnClickListener(new View.OnClickListener() {
            int clickCount = 0;
            long lastClickTime = 0;

            @Override
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                if (now - lastClickTime < 600) {
                    clickCount++;
                    if (clickCount >= 5) {
                        clickCount = 0;
                        showAdminInviteDialog();
                    }
                } else {
                    clickCount = 1;
                }
                lastClickTime = now;
            }
        });



        Intent intent = getIntent();
        boolean fromRegistration = intent.getBooleanExtra("fromRegistration", false);
        if (fromRegistration) {
            registeredEmail = intent.getStringExtra("registeredEmail");
            if (registeredEmail != null && !registeredEmail.isEmpty()) {
                loginEmail.setText(registeredEmail);
                Toast.makeText(this, "Please verify your email to log in. We've sent a verification email to " + registeredEmail + ".", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Registration complete! Please log in and verify your email.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d("LoginActivity","fromRegistration is false");
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (currentUser.isEmailVerified()) {
                        syncEmailToFirestoreIfNeeded(currentUser);

                        db.collection("users").document(currentUser.getUid())
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        Boolean isDisabled = doc.getBoolean("disabled");
                                        if (Boolean.TRUE.equals(isDisabled)) {
                                            Toast.makeText(this, "This account has been disabled.", Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                            return;
                                        }

                                        String role = doc.getString("role");
                                        if ("admin".equals(role) || "superadmin".equals(role)) {
                                            Toast.makeText(this, "Welcome back, Admin!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                                        } else {
                                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                        }
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Account not found.", Toast.LENGTH_LONG).show();
                                        mAuth.signOut();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Something went wrong. Please try again later.", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                });
                    } else {
                        Toast.makeText(this, "Please verify your email to continue.", Toast.LENGTH_LONG).show();
                        loginEmail.setText(currentUser.getEmail());
                    }
                } else {
                    Toast.makeText(this, "Failed to check user status. Please try again.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                }
            });
        } else {
            Log.d("LoginActivity","currentUser is null");
        }
    }

    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            if (user.isEmailVerified()) {
                                db.collection("users").document(user.getUid())
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {

                                                syncEmailToFirestoreIfNeeded(user);

                                                Boolean isDisabled = documentSnapshot.getBoolean("disabled");
                                                if (Boolean.TRUE.equals(isDisabled)) {
                                                    Toast.makeText(this, "This account has been disabled.", Toast.LENGTH_LONG).show();
                                                    mAuth.signOut();
                                                    return;
                                                }

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
                                                    mAuth.signOut();
                                                }
                                            } else {
                                                Toast.makeText(this, "Account not found. Please check your login info.", Toast.LENGTH_SHORT).show();
                                                mAuth.signOut();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to fetch user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            mAuth.signOut();
                                        });
                            } else {
                                loginEmail.setError("Please verify your email address.");
                                loginEmail.requestFocus();
                                Toast.makeText(LoginActivity.this, "Please verify your email to continue. A verification email has been sent.", Toast.LENGTH_LONG).show();
                                sendVerificationEmail(user);
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication successful but user object is null.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Exception e = task.getException();
                        if (e != null) {
                            Log.w("LoginActivity", "signInWithEmail:failure", e);
                            if (e instanceof FirebaseAuthException) {
                                String errorCode = ((FirebaseAuthException) e).getErrorCode();
                                switch (errorCode) {
                                    case "ERROR_USER_NOT_FOUND":
                                    case "ERROR_WRONG_PASSWORD":
                                        loginEmail.setError("Invalid email or password.");
                                        loginEmail.requestFocus();
                                        break;
                                    case "ERROR_TOO_MANY_REQUESTS":
                                        Toast.makeText(LoginActivity.this, "Too many login attempts. Please try again later.", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        loginEmail.setError("Authentication failed: " + e.getMessage());
                                        loginEmail.requestFocus();
                                        break;
                                }
                            } else {
                                loginEmail.setError("Authentication failed: " + e.getMessage());
                                loginEmail.requestFocus();
                            }
                        }
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(sendTask -> {
            if (sendTask.isSuccessful()) {
                Toast.makeText(LoginActivity.this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(LoginActivity.this, "Failed to send verification email. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAdminInviteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Admin Invite Code");

        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setPadding(64, 16, 64, 0);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        inputLayout.addView(input);

        builder.setView(inputLayout);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (code.equals("VIBE-ADMIN-ONLY")) {
                Intent intent = new Intent(LoginActivity.this, AdminRegisterActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(LoginActivity.this, "Incorrect invite code", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    public static Spanned fromHtmlCompat(String html) {
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
    }

    private void syncEmailToFirestoreIfNeeded(FirebaseUser user) {
        String authEmail = user.getEmail();
        if (authEmail == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firestoreEmail = doc.getString("email");
                        if (firestoreEmail == null || !firestoreEmail.equals(authEmail)) {
                            doc.getReference().update("email", authEmail)
                                    .addOnSuccessListener(aVoid -> Log.d("LoginActivity", "Email synced to Firestore"))
                                    .addOnFailureListener(e -> Log.e("LoginActivity", "Failed to sync email", e));
                        }
                    }
                });
    }
}

