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
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPass;
    private ImageView passwordToggle;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = MyApplication.getFirestore();

        // Initialize views
        ImageButton backButton = findViewById(R.id.backButton);
        loginEmail = findViewById(R.id.loginEmail);
        loginPass = findViewById(R.id.loginPass);
        passwordToggle = findViewById(R.id.passwordToggle);
        Button loginButton = findViewById(R.id.loginButton);
        TextView forgotPassword = findViewById(R.id.forgotPassword);
        TextView registerNow = findViewById(R.id.registerNow);
        ImageView avatar = findViewById(R.id.avatar);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Back Button Click Listener
        backButton.setOnClickListener(v -> {
            finish(); // return to previous page
        });

        // Password Toggle (Show/Hide Password)
        passwordToggle.setOnClickListener(v -> {
            if (loginPass.getTransformationMethod() instanceof PasswordTransformationMethod) {
                // Show password
                loginPass.setTransformationMethod(null);
                passwordToggle.setImageResource(R.drawable.ic_eye); // Change icon to open eye
            } else {
                // Hide password
                loginPass.setTransformationMethod(new PasswordTransformationMethod());
                passwordToggle.setImageResource(R.drawable.ic_eye_off); // Change icon to closed eye
            }
        });

        // Login Button Click Listener
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString();
            String password = loginPass.getText().toString();

            // Validate email and password (very important!)
            if (email.isEmpty()) {
                loginEmail.setError("Email is required");
                loginEmail.requestFocus(); // Request focus to the email field
                return; // Stop further execution
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

            if (password.length() < 6) { // Minimum password length (adjust as needed)
                loginPass.setError("Minimum password length is 6 characters");
                loginPass.requestFocus();
                return;
            }

            // Now that you've validated, call Firebase Authentication
            signInUser(email, password);

        });

        // Forgot Password Click Listener
        forgotPassword.setOnClickListener(v -> {
            // Navigate to Forgot Password Activity
            // Example: startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        // Register Now Click Listener
        registerNow.setOnClickListener(v -> {
            // Navigate to Register Activity
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
        }
        else {
            // User is signed in but not verified
            //You may want to log them out or resend verification email here
            mAuth.signOut(); //Sign out user, prompt to verify again
        }
    }

    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Check if email is verified (important!)
                        if (user != null && user.isEmailVerified()) {
                            // Proceed to the next activity (e.g., HomeActivity)
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish(); // Close LoginActivity so the user can't go back without logging out
                        } else {
                            // Email is not verified
                            // Display a message to the user and/or send a verification email again
                            loginEmail.setError("Please verify your email address");
                            loginEmail.requestFocus();

                            // Optionally, you can resend the verification email:
                            if(user!=null) { //Added Null Check
                                user.sendEmailVerification().addOnCompleteListener(sendTask -> {
                                    if (sendTask.isSuccessful()) {
                                        // Verification email resent successfully
                                        // Display a message to the user
                                        Toast.makeText(LoginActivity.this, "Verification email resent", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Handle the error
                                        Toast.makeText(LoginActivity.this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else{
                                Toast.makeText(LoginActivity.this, "Could not resend verification email. Please try again later.", Toast.LENGTH_SHORT).show();
                            }
                        }

                    } else {
                        // If sign in fails, display a message to the user.
                        Exception e = task.getException();
                        if (e != null) {
                            Log.w("LoginActivity", "signInWithEmail:failure", task.getException());
                            loginEmail.setError("Authentication failed: " + e.getMessage()); // Display Firebase's error message
                            loginEmail.requestFocus();
                        }
                    }
                });
    }

}

