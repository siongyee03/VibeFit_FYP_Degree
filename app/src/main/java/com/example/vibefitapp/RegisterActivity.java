package com.example.vibefitapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final int DEFAULT_AVATAR_RESOURCE = R.drawable.ic_avatar_placeholder;
    private EditText regisEmail, regis_pass, regis_confirm_pass;
    private ImageView ivAvatar;
    private Uri selectedImageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    // ActivityResultLauncher for image picking
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mAuth.useEmulator("10.0.2.2", 9099); // Auth Emulator

        db = FirebaseFirestore.getInstance();
        db.useEmulator("10.0.2.2", 8080); // Firestore Emulator

        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();
        storage.useEmulator("10.0.2.2", 9199); // Connect to the Storage Emulator
        storageReference = storage.getReference();
        //storageReference = FirebaseStorage.getInstance().getReference();

        // Initializing views
        EditText regisUsername = findViewById(R.id.regisUsername);
        regisEmail = findViewById(R.id.regisEmail);
        regis_pass = findViewById(R.id.regis_pass);
        regis_confirm_pass = findViewById(R.id.regis_confirm_pass);
        Button btnRegister = findViewById(R.id.btn_register);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ivAvatar = findViewById(R.id.avatar);
        TextView loginNow = findViewById(R.id.login_now);

        // Back Button Click Listener
        btnBack.setOnClickListener(v -> {
            finish(); // return to previous page
        });

        Spinner genderSpinner = findViewById(R.id.spinner_gender);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        // Initialize ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        selectedImageUri = data.getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            ivAvatar.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            Log.e(TAG, "Error getting bitmap from URI", e);
                        }
                    }
                });

        // Set default avatar image
        ivAvatar.setImageResource(DEFAULT_AVATAR_RESOURCE);

        // Password visibility toggle
        setupPasswordVisibilityToggle();

        // Avatar Image Click Listener - You can use a library to open the image picker (e.g., Glide or Picasso)
        ivAvatar.setOnClickListener(v -> openImagePicker());

        //Register button listener
        btnRegister.setOnClickListener(v -> {
            String username = regisUsername.getText().toString().trim();
            String email = regisEmail.getText().toString().trim();
            String password = regis_pass.getText().toString().trim();
            String confirmPassword = regis_confirm_pass.getText().toString().trim();
            String selectedGender = genderSpinner.getSelectedItem().toString();

            if (username.isEmpty()) {
                regisUsername.setError("Username is required");
                regisUsername.requestFocus();
                return;
            }

            // Validation
            if (email.isEmpty()) {
                regisEmail.setError("Email is required");
                regisEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                regisEmail.setError("Please enter a valid email");
                regisEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                regis_pass.setError("Password is required");
                regis_pass.requestFocus();
                return;
            }

            if (password.length() < 8) {
                regis_pass.setError("Minimum password length is 8 characters");
                regis_pass.requestFocus();
                return;
            }

            if (!password.equals(confirmPassword)) {
                regis_confirm_pass.setError("Passwords do not match");
                regis_confirm_pass.requestFocus();
                return;
            }

            // If all validations pass, proceed with Firebase registration
            createAcc(username, email, password, selectedGender);
        });

        loginNow.setOnClickListener(v -> {
            // Navigate to Register Activity
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }

    // Open Image Picker
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private void setupPasswordVisibilityToggle() {
        ImageView togglePassword = findViewById(R.id.iv_toggle_password);
        ImageView toggleConfirmPassword = findViewById(R.id.iv_toggle_confirm_password);

        togglePassword.setTag("hidden");
        toggleConfirmPassword.setTag("hidden");

        togglePassword.setOnClickListener(v -> togglePasswordVisibility(regis_pass, togglePassword));

        toggleConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(regis_confirm_pass, toggleConfirmPassword));
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleIcon) {
        if ("hidden".equals(toggleIcon.getTag())) {
            // show password
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleIcon.setImageResource(R.drawable.ic_eye);
            toggleIcon.setTag("visible");
        } else {
            // hide password
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleIcon.setImageResource(R.drawable.ic_eye_off);
            toggleIcon.setTag("hidden");
        }
        editText.setSelection(editText.length()); // keep cursor at the end
    }

    private void createAcc(String username, String email, String password, String selectedGender) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {uploadImageAndStoreUser(user, username, selectedGender);

                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration successful, but could not get user.  Please try logging in.",
                                    Toast.LENGTH_LONG).show();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish(); // Close RegisterActivity
                        }
                    } else {
                        // Registration failed
                        Exception e = task.getException();
                        if (e != null) {
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Upload Image and Store User Info
    private void uploadImageAndStoreUser(FirebaseUser user, String username, String selectedGender) {
        if (selectedImageUri != null) {
            // Upload the image to Firebase Storage
            StorageReference imageRef = storageReference.child("avatars/" + UUID.randomUUID().toString());

            // Convert image to bytes
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            } catch (IOException e) {
                Log.e(TAG, "Error getting bitmap from URI", e);
            }

            if (bitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
                byte[] data = baos.toByteArray();

                UploadTask uploadTask = imageRef.putBytes(data);
                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    // Get the image URL
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        // Store user info with image URL in Firestore
                        storeUserInfo(user, username, imageUrl, selectedGender);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL", e);
                        storeUserInfo(user, username, String.valueOf(DEFAULT_AVATAR_RESOURCE),selectedGender);
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed", e);
                    Toast.makeText(RegisterActivity.this, "Image upload failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    storeUserInfo(user, username, String.valueOf(DEFAULT_AVATAR_RESOURCE),selectedGender);
                });
            } else {
                Log.w(TAG, "Bitmap is null, using default avatar");
                storeUserInfo(user, username, String.valueOf(DEFAULT_AVATAR_RESOURCE),selectedGender);
            }
        } else {
            // No image selected, store user info with a default image URL in Firestore
            storeUserInfo(user, username, String.valueOf(DEFAULT_AVATAR_RESOURCE),selectedGender);
        }
    }

    // Store User Info in Firestore
    private void storeUserInfo(FirebaseUser user, String username, String imageUrl, String selectedGender) {
        String userId = user.getUid();
        User newUser = new User(username, user.getEmail(), imageUrl, selectedGender);

        db.collection("users")
                .document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d("RegisterActivity", "Username and image URL stored in Firestore");

                    // email verification
                    user.sendEmailVerification().addOnCompleteListener(sendTask -> {
                        if (sendTask.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Verification email sent. Please check your inbox.",
                                    Toast.LENGTH_LONG).show();

                            // Optionally, sign out the user after sending the verification email
                            mAuth.signOut();

                            // Redirect to LoginActivity (or a "check your email" screen)
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish(); // Close RegisterActivity
                        } else {
                            Toast.makeText(RegisterActivity.this, "Failed to send verification email.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.w("RegisterActivity", "Error adding document", e);
                    Toast.makeText(RegisterActivity.this, "Failed to store username.",
                            Toast.LENGTH_SHORT).show();
                });
    }
}