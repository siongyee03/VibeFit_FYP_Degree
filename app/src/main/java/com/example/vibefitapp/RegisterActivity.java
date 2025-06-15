package com.example.vibefitapp;

import static com.example.vibefitapp.LoginActivity.fromHtmlCompat;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.auth.UserProfileChangeRequest;
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
    private ImageView ivAvatar, togglePassword, toggleConfirmPassword;
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
        db = FirebaseFirestore.getInstance();
        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initializing views
        TextView passwordRequirements = findViewById(R.id.password_requirements);

        EditText regisUsername = findViewById(R.id.regisUsername);
        regisEmail = findViewById(R.id.regisEmail);
        regis_pass = findViewById(R.id.regis_pass);
        regis_confirm_pass = findViewById(R.id.regis_confirm_pass);
        togglePassword = findViewById(R.id.iv_toggle_password);
        toggleConfirmPassword = findViewById(R.id.iv_toggle_confirm_password);
        Button btnRegister = findViewById(R.id.btn_register);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ivAvatar = findViewById(R.id.avatar);
        TextView loginNow = findViewById(R.id.login_now);
        loginNow.setText(fromHtmlCompat("<u>Login Now</u>"));

        // Back Button Click Listener
        btnBack.setOnClickListener(v -> {
            finish(); // return to previous page
        });

        regis_pass.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordRequirements.setVisibility(View.VISIBLE);
            } else {
                passwordRequirements.setVisibility(View.GONE);
            }
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
                        selectedImageUri = result.getData().getData();
                        Glide.with(RegisterActivity.this)
                                .load(selectedImageUri)
                                .circleCrop()
                                .into(ivAvatar);
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

        ivAvatar.setOnClickListener(v -> openImagePicker());

        regis_pass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                togglePassword.setVisibility(View.VISIBLE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        regis_confirm_pass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                toggleConfirmPassword.setVisibility(View.VISIBLE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

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
                togglePassword.setVisibility(View.GONE);
                return;
            }

            if (!isPasswordValid(password)) {
                regis_pass.setError("Password must be at least 8 characters and include uppercase, lowercase, and number");
                regis_pass.requestFocus();
                togglePassword.setVisibility(View.GONE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                regis_confirm_pass.setError("Passwords do not match");
                regis_confirm_pass.requestFocus();
                toggleConfirmPassword.setVisibility(View.GONE);
                return;
            }

            createAcc(username, email, password, selectedGender);
        });

        loginNow.setOnClickListener(v -> {
            // Navigate to Register Activity
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }

    private boolean isPasswordValid(String password) {
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    }

    // Open Image Picker
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private void setupPasswordVisibilityToggle() {
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
        editText.setSelection(editText.length());
    }

    private void createAcc(String username, String email, String password, String selectedGender) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user == null) {
                            Toast.makeText(RegisterActivity.this, "You're registered! Please log in to continue.", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.putExtra("fromRegistration", true);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        // Set displayName
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build();

                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        Log.d(TAG, "User profile updated with displayName: " + username);
                                    }
                                    uploadImageAndStoreUser(user, username, selectedGender);
                                });

                    } else {
                        Exception e = task.getException();
                        if (e != null) {
                            Log.e("RegisterActivity", "Registration failed: " + e.getMessage(), e);
                            Toast.makeText(RegisterActivity.this, "Sorry, we couldn't complete your registration. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Upload Image and Store User Info
    private void uploadImageAndStoreUser(FirebaseUser user, String username, String selectedGender) {
        if (selectedImageUri != null) {
            StorageReference imageRef = storageReference.child("avatars/" + UUID.randomUUID().toString());

            try {
                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);

                int width = originalBitmap.getWidth();
                int height = originalBitmap.getHeight();
                int newEdge = Math.min(width, height);
                int xOffset = (width - newEdge) / 2;
                int yOffset = (height - newEdge) / 2;

                Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, xOffset, yOffset, newEdge, newEdge);

                int targetSize = 512;
                Bitmap bitmapToUpload = Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmapToUpload.compress(Bitmap.CompressFormat.JPEG, 20, baos);
                byte[] data = baos.toByteArray();

                UploadTask uploadTask = imageRef.putBytes(data);
                uploadTask.addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    storeUserInfo(user, username, imageUrl, selectedGender);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL", e);
                    storeUserInfo(user, username, null, selectedGender);
                })).addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Image upload failed: " + e.getMessage(), e);
                    Toast.makeText(RegisterActivity.this, "Sorry, we couldn't upload your image. Please try again.", Toast.LENGTH_SHORT).show();
                    storeUserInfo(user, username, null, selectedGender);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error getting bitmap from URI", e);
                storeUserInfo(user, username, null, selectedGender);
            }
        } else {
            storeUserInfo(user, username, null, selectedGender);
        }
    }


    private void storeUserInfo(FirebaseUser user, String username, String imageUrl, String selectedGender) {
        String userId = user.getUid();
        User newUser = new User(username, user.getEmail(), imageUrl, selectedGender, "user");

        db.collection("users")
                .document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d("RegisterActivity", "Username and image URL stored in Firestore");

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .setPhotoUri(imageUrl != null ? Uri.parse(imageUrl) : null)
                            .build();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful()) {
                                    Log.d(TAG, "Firebase Auth profile updated with name and avatar");
                                } else {
                                    Log.w(TAG, "Failed to update Firebase Auth profile", profileTask.getException());
                                }
                            });

                    // email verification
                    user.sendEmailVerification().addOnCompleteListener(sendTask -> {
                        if (sendTask.isSuccessful()) {
                            //Toast.makeText(RegisterActivity.this, "We've sent a verification email. Please check your inbox or spam folder to activate your account.", Toast.LENGTH_LONG).show();

                            mAuth.signOut();

                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.putExtra("fromRegistration", true);
                            intent.putExtra("registeredEmail", user.getEmail());
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Failed to send verification email.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.w("RegisterActivity", "Error adding document", e);
                    Toast.makeText(RegisterActivity.this, "Oops! Something went wrong while saving your account. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
}