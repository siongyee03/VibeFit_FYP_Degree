package com.example.vibefitapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class AiTryOnActivity extends AppCompatActivity {

    private View loadingOverlay;
    private TextView loadingText, tvFaceGuideline;
    private ImageView imgUserModel;
    private RecyclerView recyclerOutfit;
    private LinearLayout placeholderContainer;
    private TabLayout tabLayout;
    private Uri userFaceUri;
    private OutfitAdapter outfitAdapter;
    private final List<Outfit> userUploadedOutfits = new ArrayList<>();
    private String userGender = "Prefer not to say";
    private Button btnUploadOverlay, btnChangeModel;
    private ActivityResultLauncher<Intent> facePickerLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<Intent> outfitPickerLauncher;
    private ImageButton btnDownload;
    private String tryOnEventId;
    private String userGlamMediaUrl;
    private String userGlamModelUrl = null;
    private String userFaceImageUrl;
    private boolean isGlamModelReady = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_try_on);

        imgUserModel = findViewById(R.id.img_user_model);
        recyclerOutfit = findViewById(R.id.recycler_outfit_grid);
        placeholderContainer = findViewById(R.id.placeholder_container);
        tabLayout = findViewById(R.id.tab_layout);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingText = findViewById(R.id.loadingText);

        btnUploadOverlay = findViewById(R.id.btn_upload_overlay);
        tvFaceGuideline = findViewById(R.id.tv_face_guideline);

        btnChangeModel = findViewById(R.id.btn_change_model);

        btnDownload = findViewById(R.id.btn_download);

        ImageButton btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(v -> finish());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerOutfit.setLayoutManager(layoutManager);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerOutfit);

        outfitAdapter = new OutfitAdapter(new OutfitAdapter.OnOutfitClick() {
            @Override
            public void onTryClick(Outfit outfit) {
                if (userFaceImageUrl != null && !userFaceImageUrl.isEmpty()) {
                    startTryOnProcess(outfit.getImageUrl());
                } else {
                    Toast.makeText(AiTryOnActivity.this, "Please upload or take a front-facing photo first", Toast.LENGTH_SHORT).show();
                }

                if (!isGlamModelReady || TextUtils.isEmpty(userGlamModelUrl)) {
                    Toast.makeText(AiTryOnActivity.this, "Face image is still processing. Please wait.", Toast.LENGTH_SHORT).show();
                } else {
                    startTryOnProcess(outfit.getImageUrl());
                }
            }

            @Override
            public void onUploadClick() {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                outfitPickerLauncher.launch(pickIntent);
            }

            @Override
            public void onDeleteClick(Outfit outfit) {
                new AlertDialog.Builder(AiTryOnActivity.this)
                        .setTitle("Delete Outfit")
                        .setMessage("Are you sure you want to delete this outfit?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteOutfit(outfit))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        recyclerOutfit.setAdapter(outfitAdapter);

        initActivityResultLaunchers();

        setupTabs();

        loadUserInfo();
        fetchUserUploadedOutfits();

        boolean hasNew = getSharedPreferences("ai_tryon_cache", MODE_PRIVATE)
                .getBoolean("hasNewUploads", false);
        if (hasNew) {
            getSharedPreferences("ai_tryon_cache", MODE_PRIVATE)
                    .edit().putBoolean("hasNewUploads", false).apply();
        }

        btnUploadOverlay.setOnClickListener(v -> {
            if (!"Male".equalsIgnoreCase(userGender) && !"Female".equalsIgnoreCase(userGender)) {
                showGenderSelectDialog();
            } else {
                showUploadOrCameraDialog();
            }
        });

        btnChangeModel.setOnClickListener(v -> {
            if (!"Male".equalsIgnoreCase(userGender) && !"Female".equalsIgnoreCase(userGender)) {
                showGenderSelectDialog();
            } else {
                showUploadOrCameraDialog();
            }
        });

        btnDownload.setOnClickListener(v -> {
            Bitmap bitmap = Bitmap.createBitmap(imgUserModel.getWidth(), imgUserModel.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            imgUserModel.draw(canvas);

            FileUtil.saveBitmapToGallery(AiTryOnActivity.this, bitmap, "vibe_tryon_" + System.currentTimeMillis());
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        });
    }

    private void initActivityResultLaunchers() {
        facePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        userFaceUri = result.getData().getData();
                        updateUserModelImage(userFaceUri);
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        userFaceUri = FileUtil.saveBitmapToUri(this, bitmap);
                        updateUserModelImage(userFaceUri);
                    }
                });

        outfitPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadOutfitToImgBBAndSave(selectedImageUri);
                        }
                    }
                }
        );
    }

    private void showLoading() {
        runOnUiThread(() -> {
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingText.setVisibility(View.VISIBLE);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> loadingOverlay.setVisibility(View.GONE));
    }

    private void showUploadOrCameraDialog() {
        String[] options = {"Choose from gallery", "Take photo"};
        new AlertDialog.Builder(this)
                .setTitle("Upload or take a front-facing photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageFromGallery();
                    } else if (which == 1) {
                        takePictureWithCamera();
                    }
                }).show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        facePickerLauncher.launch(intent);
    }

    private void takePictureWithCamera() {
        cameraLauncher.launch(null);
    }

    private void updateUserModelImage(Uri faceUri) {
        if (faceUri == null) return;

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        showLoading();
        loadingText.setVisibility(View.GONE);
        isGlamModelReady = false;

        Glide.with(this).load(faceUri).into(imgUserModel);
        imgUserModel.setVisibility(View.VISIBLE);
        placeholderContainer.setVisibility(View.GONE);
        btnUploadOverlay.setVisibility(View.GONE);
        btnChangeModel.setVisibility(View.VISIBLE);
        tvFaceGuideline.setVisibility(View.GONE);

        String filename = "user_faces/" + uid + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(filename);

        ref.putFile(faceUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            String firebaseStorageUrl = downloadUri.toString();
                            Log.d("UpdateModel", "Firebase Storage URL: " + firebaseStorageUrl);

                            // Update local variable immediately
                            userFaceImageUrl = firebaseStorageUrl;

                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(uid)
                                    .update("faceImageUrl", firebaseStorageUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("UpdateModel", "Firestore faceImageUrl updated.");

                                        new Thread(() -> {
                                            try {

                                                File tempFaceFile = FileUtil.downloadImageToTempFile(this, firebaseStorageUrl);
                                                runOnUiThread(() -> {
                                                    uploadFaceImageToGlamAI(tempFaceFile, uploadedGlamUrl -> {
                                                        if (uploadedGlamUrl != null) {
                                                            userGlamModelUrl = uploadedGlamUrl;
                                                            isGlamModelReady = true;

                                                            if (!"Prefer not to say".equalsIgnoreCase(userGender)) {
                                                                FirebaseFirestore.getInstance()
                                                                        .collection("users")
                                                                        .document(uid)
                                                                        .update("glamMediaUrl", uploadedGlamUrl, "gender", userGender)
                                                                        .addOnSuccessListener(v -> {
                                                                            hideLoading();
                                                                            Toast.makeText(this, "Face model updated successfully!", Toast.LENGTH_SHORT).show();
                                                                            Log.d("UpdateModel", "Firestore glamMediaUrl and gender updated.");
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            hideLoading();
                                                                            Toast.makeText(this, "Face model updated, but failed to save to Firestore.", Toast.LENGTH_SHORT).show();
                                                                            Log.e("UpdateModel", "Firestore glamMediaUrl update failed: " + e.getMessage());
                                                                        });
                                                            } else {
                                                                FirebaseFirestore.getInstance()
                                                                        .collection("users")
                                                                        .document(uid)
                                                                        .update("glamMediaUrl", uploadedGlamUrl)
                                                                        .addOnSuccessListener(v -> {
                                                                            hideLoading();
                                                                            Toast.makeText(this, "Face model updated successfully!", Toast.LENGTH_SHORT).show();
                                                                            Log.d("UpdateModel", "Firestore glamMediaUrl updated.");
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            hideLoading();
                                                                            Toast.makeText(this, "Face model updated, but failed to save to Firestore.", Toast.LENGTH_SHORT).show();
                                                                            Log.e("UpdateModel", "Firestore glamMediaUrl update failed: " + e.getMessage());
                                                                        });
                                                            }
                                                        } else {
                                                            isGlamModelReady = false;
                                                            hideLoading();
                                                            Toast.makeText(this, "Failed to upload face to Glam AI.", Toast.LENGTH_SHORT).show();
                                                            Log.e("UpdateModel", "Glam AI upload failed.");
                                                        }
                                                    });
                                                });
                                            } catch (IOException e) {
                                                runOnUiThread(() -> {
                                                    hideLoading();
                                                    Log.e("updateUserModelImage", "Error downloading image to temp file", e);
                                                    Toast.makeText(this, "Error preparing image for Glam AI.", Toast.LENGTH_SHORT).show();
                                                });
                                            }
                                        }).start();
                                    })
                                    .addOnFailureListener(e -> {
                                        hideLoading();
                                        Toast.makeText(this, "Failed to update face image URL in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e("UpdateModel", "Firestore faceImageUrl update failed: " + e.getMessage());
                                    });
                        }))
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Failed to upload face photo to Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UpdateModel", "Firebase Storage upload failed: " + e.getMessage());
                });
    }

    private void showGenderSelectDialog() {
        String[] genderOptions = {"Male", "Female"};
        new AlertDialog.Builder(this)
                .setTitle("Select a model gender")
                .setItems(genderOptions, (dialog, which) -> {
                    userGender = genderOptions[which];

                    String uid = FirebaseAuth.getInstance().getUid();
                    if (uid != null) {
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                .update("gender", userGender)
                                .addOnSuccessListener(aVoid -> Log.d("GenderUpdate", "User gender updated to " + userGender))
                                .addOnFailureListener(e -> Log.e("GenderUpdate", "Error updating user gender", e));
                    }

                    showUploadOrCameraDialog();
                })
                .setCancelable(true)
                .show();
    }

    private void loadUserInfo() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getUid();
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.contains("gender")) {
                        userGender = doc.getString("gender");
                    }

                    if (doc.contains("faceImageUrl")) {
                        userFaceImageUrl = doc.getString("faceImageUrl");
                        if (userFaceImageUrl != null && !userFaceImageUrl.isEmpty()) {
                            Glide.with(this).load(userFaceImageUrl).into(imgUserModel);
                            imgUserModel.setVisibility(View.VISIBLE);
                            placeholderContainer.setVisibility(View.GONE);
                            btnUploadOverlay.setVisibility(View.GONE);
                            btnChangeModel.setVisibility(View.VISIBLE);
                            tvFaceGuideline.setVisibility(View.GONE);
                        }
                    }
                    if (doc.contains("glamMediaUrl")) {
                        userGlamModelUrl = doc.getString("glamMediaUrl");
                    }
                });
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("My Uploads"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                outfitAdapter.submitList(new ArrayList<>(userUploadedOutfits));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        TabLayout.Tab myUploadsTab = tabLayout.getTabAt(0);
        if (myUploadsTab != null) {
            myUploadsTab.select();
        }
    }

    private void uploadOutfitToImgBBAndSave(Uri imageUri) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || imageUri == null) return;

        uploadOutfitToImgBB(imageUri, imageUrl -> {
            if (imageUrl == null) {
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("user_uploaded_outfits").document();
            String docId = docRef.getId();

            Outfit outfit = new Outfit(imageUrl, uid);
            outfit.setId(docId);
            outfit.setUserUploaded(true);

            docRef.set(outfit)
                    .addOnSuccessListener(aVoid -> {
                        userUploadedOutfits.add(0, outfit);
                        outfitAdapter.submitList(new ArrayList<>(userUploadedOutfits));
                        recyclerOutfit.scrollToPosition(0);

                        Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void uploadOutfitToImgBB(Uri imageUri, Callback<String> callback) {
        new Thread(() -> {
            try {
                File imageFile = FileUtil.getFileFromUri(this, imageUri);

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", Base64.encodeToString(
                                FileUtil.readFileToBytes(imageFile), Base64.NO_WRAP))
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.imgbb.com/1/upload?key=0dc13dd04a87b46107b4bfba22cf39cd")
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        Log.e("ImgBB", "Upload failed: " + response.code());
                        runOnUiThread(() -> callback.call(null));
                        return;
                    }

                    String resp = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(resp);
                    String imageUrl = json.getJSONObject("data").getString("url");

                    runOnUiThread(() -> callback.call(imageUrl));
                }
            } catch (Exception e) {
                Log.e("ImgBB", "Exception uploading image", e);
                runOnUiThread(() -> callback.call(null));
            }
        }).start();
    }

    private void fetchUserUploadedOutfits() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("user_uploaded_outfits")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userUploadedOutfits.clear();
                    List<Outfit> tempFetchedOutfits = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Outfit outfit = doc.toObject(Outfit.class);
                        if (outfit == null) continue;

                        outfit.setId(doc.getId());
                        outfit.setUserUploaded(true);

                        if ((outfit.getImageUrl() == null || outfit.getImageUrl().isEmpty())
                                && outfit.getStoragePath() != null && !outfit.getStoragePath().isEmpty()) {

                            String cleanPath = outfit.getStoragePath().startsWith("/") ?
                                    outfit.getStoragePath().substring(1) : outfit.getStoragePath();

                            FirebaseStorage.getInstance().getReference(cleanPath)
                                    .getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        outfit.setImageUrl(uri.toString());

                                        userUploadedOutfits.add(outfit);

                                        outfitAdapter.submitList(new ArrayList<>(userUploadedOutfits));
                                    })
                                    .addOnFailureListener(e -> Log.e("fetchOutfits", "Failed to get downloadUrl", e));
                        } else {
                            tempFetchedOutfits.add(outfit);
                        }
                    }
                    userUploadedOutfits.addAll(tempFetchedOutfits);

                    outfitAdapter.submitList(new ArrayList<>(userUploadedOutfits));

                });
    }


    private void deleteOutfit(Outfit outfit) {
        String docId = outfit.getId();
        if (docId == null || docId.isEmpty()) {
            Toast.makeText(this, "Cannot delete: Outfit ID is missing.", Toast.LENGTH_SHORT).show();
            Log.e("DeleteOutfit", "Attempted to delete outfit with null/empty ID.");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("user_uploaded_outfits")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {

                    if (outfit.getStoragePath() != null && !outfit.getStoragePath().isEmpty()) {
                        StorageReference photoRef = FirebaseStorage.getInstance().getReference(outfit.getStoragePath());
                        photoRef.delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    userUploadedOutfits.remove(outfit);
                                    outfitAdapter.submitList(new ArrayList<>(userUploadedOutfits));
                                    Toast.makeText(this, "Outfit deleted (and image from Storage).", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Deleted from Firestore, but failed to delete image from Storage: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e("DeleteOutfit", "Failed to delete image from Storage: " + e.getMessage());
                                    userUploadedOutfits.remove(outfit);
                                    outfitAdapter.submitList(new ArrayList<>(userUploadedOutfits));
                                });
                    } else {
                        userUploadedOutfits.remove(outfit);
                        outfitAdapter.submitList(new ArrayList<>(userUploadedOutfits));
                        Toast.makeText(this, "Outfit deleted.", Toast.LENGTH_SHORT).show();
                        Log.d("DeleteOutfit", "Outfit deleted from Firestore. No Firebase Storage path to delete.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete outfit from Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("DeleteOutfit", "Failed to delete Firestore doc: " + e.getMessage());
                });
    }

    // upload face image to glam ai server, callback with uploaded image url or null if fail
    private void uploadFaceImageToGlamAI(File faceFile, Callback<String> callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || faceFile == null || !faceFile.exists()) {
            Log.e("GlamAI_Upload", "Invalid input: UID, faceFile or faceFile not exists.");
            runOnUiThread(() -> callback.call(null));
            return;
        }

        Log.d("GlamAI_Upload", "Attempting to upload face file: " + faceFile.getAbsolutePath());
        Log.d("GlamAI_Upload", "File size: " + faceFile.length() + " bytes");
        Log.d("GlamAI_Upload", "File name: " + faceFile.getName());

        new Thread(() -> {
            try {
                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", faceFile.getName(),
                                RequestBody.create(faceFile, MediaType.parse("image/jpeg")))
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.glam.ai/api/v1/upload")
                        .addHeader("X-API-Key", getApiKeyFromSecureStorage())
                        .addHeader("accept", "application/json")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String resp = response.body() != null ? response.body().string() : "";
                    Log.d("GlamAI_Upload", "Glam AI Upload Response Code: " + response.code());
                    Log.d("GlamAI_Upload", "Glam AI Upload Response Body: " + resp);

                    if (!response.isSuccessful()) {
                        Log.e("GlamAI_Upload", "Glam AI Upload failed: HTTP " + response.code() + ", Response: " + resp);
                        runOnUiThread(() -> callback.call(null));
                        return;
                    }

                    JSONObject json = new JSONObject(resp);
                    String uploadedUrl = json.optString("file_url", "");

                    if (!TextUtils.isEmpty(uploadedUrl)) {
                        Log.d("GlamAI_Upload", "Glam AI Upload Successful. Received URL: " + uploadedUrl);

                        // Firestore update and local variable update remain here
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update("glamMediaUrl", uploadedUrl)
                                .addOnSuccessListener(aVoid -> Log.d("GlamAI_Upload", "Firestore glamMediaUrl updated successfully."))
                                .addOnFailureListener(e -> Log.e("GlamAI_Upload", "Failed to update Firestore glamMediaUrl: " + e.getMessage()));

                        userGlamMediaUrl = uploadedUrl; // Update local variable for immediate use

                        runOnUiThread(() -> callback.call(uploadedUrl));
                    } else {
                        Log.e("GlamAI_Upload", "Glam AI Upload successful, but 'file_url' is empty in response. Response: " + resp);
                        runOnUiThread(() -> callback.call(null));
                    }
                }
            } catch (Exception e) {
                Log.e("GlamAI_Upload", "Exception during Glam AI upload process", e);
                runOnUiThread(() -> callback.call(null));
            }
        }).start();
    }

    private void startTryOnProcess(String outfitImageUrl) {
        showLoading();

        if (userGlamModelUrl != null && !userGlamModelUrl.isEmpty()) {

            sendTryOnRequest(userGlamModelUrl, outfitImageUrl, eventId -> {
                if (eventId == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Try-on request failed", Toast.LENGTH_SHORT).show());
                    return;
                }
                tryOnEventId = eventId;
                pollTryOnResult(eventId);
            });
        } else if (userFaceImageUrl != null && !userFaceImageUrl.isEmpty()) {
            // If we have a Firebase URL but not a Glam AI URL, then upload to Glam AI
            new Thread(() -> {
                try {
                    File faceFile = FileUtil.downloadImageToTempFile(this, userFaceImageUrl);
                    runOnUiThread(() -> uploadFaceImageToGlamAI(faceFile, uploadedGlamUrl -> {
                        if (uploadedGlamUrl == null) {
                            runOnUiThread(() -> Toast.makeText(this, "Failed to upload model to Glam AI", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        userGlamModelUrl = uploadedGlamUrl;
                        sendTryOnRequest(uploadedGlamUrl, outfitImageUrl, eventId -> {
                            if (eventId == null) {
                                runOnUiThread(() -> Toast.makeText(this, "Try-on request failed", Toast.LENGTH_SHORT).show());
                                return;
                            }
                            tryOnEventId = eventId;
                            pollTryOnResult(eventId);
                        });
                    }));
                } catch (IOException e) {
                    Log.e("startTryOnProcess", "Failed to download image: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to prepare model image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            Toast.makeText(this, "Please upload or take a front-facing photo first", Toast.LENGTH_SHORT).show();
        }
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private void sendTryOnRequest(String faceImageUrl, String outfitImageUrl, Callback<String> callback) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("media_url", faceImageUrl);
                json.put("garment_url", outfitImageUrl);
                json.put("mask_type", "overall");

                String requestBodyString = json.toString();
                Log.d("sendTryOnRequest", "Request URL: https://api.glam.ai/api/v1/tryon");
                Log.d("sendTryOnRequest", "Request Body: " + requestBodyString);

                RequestBody body = RequestBody.create(
                        requestBodyString,
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url("https://api.glam.ai/api/v1/tryon")
                        .addHeader("X-API-Key", getApiKeyFromSecureStorage())
                        .addHeader("accept", "application/json")
                        .addHeader("content-type", "application/json")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    Headers headers = response.headers();
                    for (int i = 0; i < headers.size(); i++) {
                        Log.d("ResponseHeader", headers.name(i) + ": " + headers.value(i));
                    }

                    String resp = response.body() != null ? response.body().string() : "null response body";
                    Log.e("sendTryOnRequest", "Response code: " + response.code());
                    Log.e("sendTryOnRequest", "Response body: " + resp);

                    if (response.isSuccessful()) {
                        JSONObject jsonResp = new JSONObject(resp);
                        String eventId = jsonResp.optString("event_id", "");
                        if (!eventId.isEmpty()) {
                            Log.d("sendTryOnRequest", "Successfully received event_id: " + eventId);
                            runOnUiThread(() -> callback.call(eventId));
                        } else {
                            Log.e("sendTryOnRequest", "Event ID is missing or empty in successful response. Response: " + resp);
                            runOnUiThread(() -> callback.call(null));
                        }
                    } else {
                        Log.e("sendTryOnRequest", "API call not successful. Code: " + response.code() + ", Body: " + resp);
                        runOnUiThread(() -> callback.call(null)); // API call unsuccessful
                    }
                }
            } catch (Exception e) {
                Log.e("sendTryOnRequest", "Exception during API call:", e);
                runOnUiThread(() -> callback.call(null)); // An exception occurred
            }
        }).start();
    }

    private void pollTryOnResult(String eventId) {
        new Thread(() -> {
            try {
                String url = "https://api.glam.ai/api/v1/tryon/" + eventId;
                int retries = 20;

                while (retries-- > 0) {
                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("X-API-Key", getApiKeyFromSecureStorage())
                            .addHeader("accept", "application/json")
                            .get()
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String resp = Objects.requireNonNull(response.body()).string();
                            JSONObject json = new JSONObject(resp);
                            String status = json.optString("status");

                            if ("READY".equalsIgnoreCase(status)) {
                                JSONArray urls = json.optJSONArray("media_urls");
                                if (urls != null && urls.length() > 0) {
                                    String outputUrl = urls.getString(0);
                                    Log.d("TryOnResult", "Final Try-on Result URL: " + outputUrl);

                                    runOnUiThread(() -> {
                                        hideLoading();

                                        Glide.with(AiTryOnActivity.this)
                                                .load(outputUrl)
                                                .listener(new RequestListener<>() {
                                                    @Override
                                                    public boolean onLoadFailed(
                                                            GlideException e,
                                                            Object model,
                                                            @NonNull Target<Drawable> target,
                                                            boolean isFirstResource) {
                                                        Log.e("GlideError", "Image load failed: " + e.getMessage(), e);
                                                        Toast.makeText(AiTryOnActivity.this, "Failed to load try-on result.", Toast.LENGTH_SHORT).show();
                                                        return false;
                                                    }

                                                    @Override
                                                    public boolean onResourceReady(
                                                            @NonNull Drawable resource,
                                                            @NonNull Object model,
                                                            @NonNull Target<Drawable> target,
                                                            @NonNull DataSource dataSource,
                                                            boolean isFirstResource) {
                                                        Log.d("GlideSuccess", "Image loaded successfully.");
                                                        return false;
                                                    }
                                                })
                                                .into(imgUserModel);

                                        imgUserModel.setVisibility(View.VISIBLE);
                                        placeholderContainer.setVisibility(View.GONE);
                                        btnUploadOverlay.setVisibility(View.GONE);
                                        btnChangeModel.setVisibility(View.VISIBLE);
                                        btnDownload.setVisibility(View.VISIBLE);
                                    });
                                }
                                return;
                            } else if ("FAILED".equalsIgnoreCase(status)) {
                                String reason = json.optString("error_message", "Try-on failed");
                                String finalReason = reason.isEmpty() ? "Try-on failed" : reason;

                                runOnUiThread(() -> {
                                    hideLoading();
                                    Toast.makeText(AiTryOnActivity.this, finalReason, Toast.LENGTH_LONG).show();
                                });
                                return;
                            }
                        }
                    }

                    Thread.sleep(1500);
                }

                runOnUiThread(() -> Toast.makeText(AiTryOnActivity.this, "Try-on timed out", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                Log.e("pollTryOnResult", "Exception:", e);
                String message = e.getMessage() != null ? e.getMessage() : "Unexpected error";
                runOnUiThread(() -> Toast.makeText(AiTryOnActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String getApiKeyFromSecureStorage() {
        return BuildConfig.VIRTUAL_TRYON_API_KEY;
    }

    interface Callback<T> {
        void call(T result);
    }
}
