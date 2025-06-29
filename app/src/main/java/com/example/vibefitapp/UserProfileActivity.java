package com.example.vibefitapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText usernameEditText, emailEditText, contactEditText;
    private Spinner genderSpinner;
    private ImageView avatarImageView;
    private Uri imageUri;
    private String profileImageUrl = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private String uid;

    private String[] genderOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();
            loadUserProfile();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        usernameEditText = findViewById(R.id.profile_username);
        emailEditText = findViewById(R.id.profile_email);
        contactEditText = findViewById(R.id.profile_contact);
        genderSpinner = findViewById(R.id.profile_gender);
        avatarImageView = findViewById(R.id.avatar);
        ImageView ivAddPhoto = findViewById(R.id.iv_add_photo);
        MaterialButton btnUpdate = findViewById(R.id.btn_update);
        ImageButton btnBack = findViewById(R.id.btn_back);

        genderOptions = getResources().getStringArray(R.array.gender_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        loadUserProfile();

        avatarImageView.setOnClickListener(v -> openImageChooser());
        ivAddPhoto.setOnClickListener(v -> openImageChooser());

        btnUpdate.setOnClickListener(v -> uploadUserData());

        btnBack.setOnClickListener(v -> finish());
    }

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    Glide.with(this).load(imageUri).circleCrop().into(avatarImageView);
                }
            });

    private void openImageChooser() {
        imagePickerLauncher.launch("image/*");
    }

    private void loadUserProfile() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        usernameEditText.setText(document.getString("username"));
                        emailEditText.setText(document.getString("email"));
                        contactEditText.setText(document.getString("contact"));

                        String gender = document.getString("gender");
                        if (gender != null) {
                            for (int i = 0; i < genderOptions.length; i++) {
                                if (gender.equalsIgnoreCase(genderOptions[i])) {
                                    genderSpinner.setSelection(i);
                                    break;
                                }
                            }
                        }

                        profileImageUrl = document.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this).load(profileImageUrl).circleCrop().into(avatarImageView);
                        }
                    }
                });
    }

    private void uploadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        user.reload().addOnCompleteListener(reloadTask -> {
            if (!reloadTask.isSuccessful()) {
                Toast.makeText(this, "Failed to reload user info. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            String refreshedAuthEmail = user.getEmail();
            String enteredEmail = emailEditText.getText().toString().trim();

            String username = usernameEditText.getText().toString().trim();
            String contact = contactEditText.getText().toString().trim();
            String gender = genderSpinner.getSelectedItem().toString();

            // --- Input Validation ---
            if (username.isEmpty()) {
                usernameEditText.setError("Username required");
                usernameEditText.requestFocus();
                return;
            }
            if (enteredEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(enteredEmail).matches()) {
                emailEditText.setError("Valid email required");
                emailEditText.requestFocus();
                return;
            }
            if (!contact.isEmpty() && !contact.matches("\\d{6,15}")) {
                contactEditText.setError("Invalid phone number");
                contactEditText.requestFocus();
                return;
            }

            if (!enteredEmail.equals(refreshedAuthEmail)) {
                updateEmailIfChanged(enteredEmail);
                Toast.makeText(this, "Verification email sent. Email will update after verification.", Toast.LENGTH_LONG).show();
            } else {
                saveUserToFirestore(username, refreshedAuthEmail, contact, gender, profileImageUrl);
            }

            String finalEmail = enteredEmail.equals(refreshedAuthEmail) ? enteredEmail : refreshedAuthEmail;

            if (imageUri != null) {
                try {
                    Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    int targetWidth = 500;
                    int targetHeight = (int) (originalBitmap.getHeight() * (targetWidth / (float) originalBitmap.getWidth()));
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] compressedData = baos.toByteArray();

                    StorageReference fileRef = storageRef.child(uid + ".jpg");
                    UploadTask uploadTask = fileRef.putBytes(compressedData);

                    uploadTask.addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        profileImageUrl = uri.toString();
                                        saveUserToFirestore(username, finalEmail, contact, gender, profileImageUrl);
                                    }))
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show());

                } catch (IOException e) {
                    Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                saveUserToFirestore(username, finalEmail, contact, gender, profileImageUrl);
            }
        });
    }

    private void updateEmailIfChanged(String newEmail) { // Removed onSuccess, onFailure
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Oops, you need to log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentEmail = user.getEmail();
        if (newEmail.equals(currentEmail)) {
            return;
        }

        user.verifyBeforeUpdateEmail(newEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email sent. Please check your inbox and verify the new email. Profile will be fully updated after verification.", Toast.LENGTH_LONG).show();
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                            showReauthenticateDialog(newEmail); // Pass newEmail only
                        } else {
                            String errorMsg = (e != null && e.getMessage() != null) ? e.getMessage() : "Unknown error";
                            Log.e("EmailUpdate", "Failed to verifyBeforeUpdateEmail", e);
                            Toast.makeText(this, "Failed to send verification email: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showReauthenticateDialog(String newEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Re-authentication Required");
        builder.setMessage("Please enter your password to confirm your identity.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            reauthenticateAndUpdateEmail(newEmail, password); // Pass newEmail only
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }

    @SuppressWarnings("deprecation")
    private void reauthenticateAndUpdateEmail(String newEmail, String password) { // Removed onSuccess, onFailure
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentEmail = user.getEmail();
        if (currentEmail == null) {
            Toast.makeText(this, "Cannot reauthenticate: email address is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(currentEmail, password);

        user.reauthenticate(credential)
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        user.updateEmail(newEmail)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        user.sendEmailVerification(); // Send verification for the *newly updated* email
                                        Toast.makeText(this, "Email updated successfully in Auth. Please verify your new email.", Toast.LENGTH_LONG).show();
                                        // NO onSuccess.run() here. The Firestore update of email will happen
                                        // when the user loads the profile next time and the auth.email is reflected.
                                        FirebaseAuth.getInstance().signOut();
                                        Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();

                                    } else {
                                        Exception e = updateTask.getException();
                                        Toast.makeText(this, "Failed to update email in Auth: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Exception e = authTask.getException();
                        Toast.makeText(this, "Re-authentication failed: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String username, String email, String contact, String gender, String imageUrl) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email); // This 'email' parameter now comes from user.getEmail() after reload
        userMap.put("contact", contact);
        userMap.put("gender", gender);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            userMap.put("profileImageUrl", imageUrl);
            userMap.put("defaultAvatar", false);
        }

        db.collection("users").document(uid).set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Update Firebase Auth user's display name
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build();
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                        // You might want to reload the user here to reflect latest Auth data in UI
                                        // user.reload();
                                    } else {
                                        Toast.makeText(this, "Profile updated but failed to update displayName", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    hideKeyboard(v);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
