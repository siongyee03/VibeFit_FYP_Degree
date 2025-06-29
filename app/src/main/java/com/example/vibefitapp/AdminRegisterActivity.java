package com.example.vibefitapp;

import static com.example.vibefitapp.LoginActivity.fromHtmlCompat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminRegisterActivity extends AppCompatActivity {

    private EditText regisUsername, regisEmail, regisPass, regisConfirmPass;
    private ImageView togglePass, toggleConfirmPass;
    private TextView passwordRequirements;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isPassVisible = false;
    private boolean isConfirmPassVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_register);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI init
        regisUsername = findViewById(R.id.regisUsername);
        regisEmail = findViewById(R.id.regisEmail);
        regisPass = findViewById(R.id.regis_pass);
        regisConfirmPass = findViewById(R.id.regis_confirm_pass);
        togglePass = findViewById(R.id.iv_toggle_password);
        toggleConfirmPass = findViewById(R.id.iv_toggle_confirm_password);
        passwordRequirements = findViewById(R.id.password_requirements);
        TextView loginNow = findViewById(R.id.login_now);

        loginNow.setText(fromHtmlCompat("<u>Login Now</u>"));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        loginNow.setOnClickListener(v -> startActivity(new Intent(AdminRegisterActivity.this, LoginActivity.class)));
        findViewById(R.id.btn_register).setOnClickListener(v -> registerAdmin());

        togglePass.setOnClickListener(v -> togglePasswordVisibility(regisPass, togglePass, true));
        toggleConfirmPass.setOnClickListener(v -> togglePasswordVisibility(regisConfirmPass, toggleConfirmPass, false));

        regisPass.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordRequirements.setVisibility(View.VISIBLE);
            } else {
                passwordRequirements.setVisibility(View.GONE);
            }
        });

        regisPass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                togglePass.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        regisConfirmPass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                toggleConfirmPass.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleIcon, boolean isPass) {
        boolean visible = isPass ? isPassVisible : isConfirmPassVisible;
        if (visible) {
            editText.setTransformationMethod(new PasswordTransformationMethod());
            toggleIcon.setImageResource(R.drawable.ic_eye_off);
        } else {
            editText.setTransformationMethod(null);
            toggleIcon.setImageResource(R.drawable.ic_eye);
        }
        if (isPass) {
            isPassVisible = !visible;
        } else {
            isConfirmPassVisible = !visible;
        }
        editText.setSelection(editText.getText().length());
    }

    private void registerAdmin() {
        String username = regisUsername.getText().toString().trim();
        String email = regisEmail.getText().toString().trim();
        String password = regisPass.getText().toString().trim();
        String confirmPassword = regisConfirmPass.getText().toString().trim();

        if (username.isEmpty()) {
            regisUsername.setError("Username is required");
            regisUsername.requestFocus();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            regisEmail.setError("Valid email is required");
            regisEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            regisPass.setError("Password is required");
            regisPass.requestFocus();
            togglePass.setVisibility(View.GONE);
            return;
        }

        if (!isPasswordValid(password)) {
            regisPass.setError("Password must be at least 8 characters and include uppercase, lowercase, and number");
            regisPass.requestFocus();
            togglePass.setVisibility(View.GONE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            regisConfirmPass.setError("Passwords do not match");
            regisConfirmPass.requestFocus();
            toggleConfirmPass.setVisibility(View.GONE);
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) return;

                    user.sendEmailVerification()
                            .addOnSuccessListener(aVoid -> {
                                String uid = user.getUid();
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("username", username);
                                userData.put("email", email);
                                userData.put("role", "admin");
                                userData.put("registeredAt", Timestamp.now());

                                db.collection("users").document(uid)
                                        .set(userData)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(this, "Verification email sent. Please verify before login.", Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to send verification email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean isPasswordValid(String password) {
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    }
}
