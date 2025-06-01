package com.example.vibefitapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.*;

public class UploadPostActivity extends AppCompatActivity {

    private static final int MAX_IMAGE_COUNT = 9;
    private Uri videoUri = null;
    private final List<Uri> imageUris = new ArrayList<>();
    private RecyclerView imageRecycler;
    private VideoView previewVideo;
    private ImageView addMoreImageButton;
    private EditText titleInput, descriptionInput;
    private Spinner categorySpinner;
    private ProgressBar uploadProgress;
    private Button postButton;
    private ImageButton backButton;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> mediaEditLauncher;
    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private ImagePreviewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_post);

        imageRecycler = findViewById(R.id.image_recycler);
        previewVideo = findViewById(R.id.previewVideo);
        addMoreImageButton = findViewById(R.id.add_more_image_button);
        titleInput = findViewById(R.id.title_input);
        descriptionInput = findViewById(R.id.description_input);
        categorySpinner = findViewById(R.id.category_spinner);
        uploadProgress = findViewById(R.id.uploadProgress);
        postButton = findViewById(R.id.post_button);
        backButton = findViewById(R.id.back_button);

        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupCategorySpinner();

        imageRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        adapter = new ImagePreviewAdapter(
                this,
                imageUris,
                position -> {
                    imageUris.remove(position);
                    adapter.notifyItemRemoved(position);
                },
                this::showImagePreviewDialog
        );
        imageRecycler.setAdapter(adapter);


        Intent intent = getIntent();
        if (intent != null) {
            String videoUriStr = intent.getStringExtra("video_uri");
            ArrayList<String> imageUriStrs = intent.getStringArrayListExtra("image_uris");

            if (videoUriStr != null) {
                videoUri = Uri.parse(videoUriStr);
                imageUris.clear();
            } else if (imageUriStrs != null && !imageUriStrs.isEmpty()) {
                videoUri = null;
                imageUris.clear();
                for (String uriStr : imageUriStrs) {
                    imageUris.add(Uri.parse(uriStr));
                }
            }
        }

        updateMediaPreview();

        addMoreImageButton.setOnClickListener(v -> {
            if (videoUri != null) {
                Toast.makeText(this, "Video posts cannot add more images.", Toast.LENGTH_SHORT).show();
                return;
            }
            selectMedia();
        });

        postButton.setOnClickListener(v -> uploadPost());
        backButton.setOnClickListener(v -> finish());

        Button editButton = findViewById(R.id.edit_media_button);
        editButton.setOnClickListener(v -> {
            if (videoUri != null) {
                launchEditor(videoUri);
            } else if (!imageUris.isEmpty()) {
                showImageSelectionDialog();
            } else {
                Toast.makeText(this, "Please select media", Toast.LENGTH_SHORT).show();
            }
        });


        // Optional: auto-suggest category from originating tab
        String sourceTab = getIntent().getStringExtra("source_tab");
        if (sourceTab != null) {
            suggestCategory(sourceTab);
        }

        mediaEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String newPath = result.getData().getStringExtra(MediaEditActivity.RESULT_MEDIA_PATH);
                        int editIndex = result.getData().getIntExtra("edit_index", -1);

                        if (newPath != null) {
                            Uri newUri = Uri.parse(newPath);
                            if (checkIfVideo(newPath)) {
                                videoUri = newUri;
                                imageUris.clear();
                            } else {
                                RecyclerView.Adapter<?> adapter = imageRecycler.getAdapter();
                                if (editIndex >= 0 && editIndex < imageUris.size()) {
                                    imageUris.set(editIndex, newUri);
                                    if (adapter != null) {
                                        adapter.notifyItemChanged(editIndex);
                                    }
                                } else {
                                    imageUris.clear();
                                    imageUris.add(newUri);
                                    if (adapter != null) {
                                        adapter.notifyItemInserted(0);
                                    }
                                }
                                updateMediaPreview();
                            }
                        }
                    }
                }
        );

        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();

                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count && imageUris.size() < MAX_IMAGE_COUNT; i++) {
                                Uri uri = data.getClipData().getItemAt(i).getUri();
                                String type = getContentResolver().getType(uri);
                                if (type != null && type.startsWith("image")) {
                                    imageUris.add(uri);
                                }
                            }
                        } else if (data.getData() != null) {
                            Uri uri = data.getData();
                            String type = getContentResolver().getType(uri);
                            if (type != null && type.startsWith("image")) {
                                imageUris.add(uri);
                            }
                        }
                        updateMediaPreview();
                    }
                }
        );
    }

    private void launchEditor(Uri mediaUri) {
        Intent intent = new Intent(this, MediaEditActivity.class);
        intent.putExtra(MediaEditActivity.EXTRA_MEDIA_URI, mediaUri);
        intent.putExtra(MediaEditActivity.EXTRA_IS_VIDEO, true);
        mediaEditLauncher.launch(intent);
    }

    private void showImageSelectionDialog() {
        String[] imageNames = new String[imageUris.size()];
        for (int i = 0; i < imageUris.size(); i++) {
            imageNames[i] = "Image " + (i + 1);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select the image to be edited.")
                .setItems(imageNames, (dialog, which) -> {
                    Uri selectedUri = imageUris.get(which);
                    Intent intent = new Intent(this, MediaEditActivity.class);
                    intent.putExtra(MediaEditActivity.EXTRA_MEDIA_URI, selectedUri);
                    intent.putExtra(MediaEditActivity.EXTRA_IS_VIDEO, false);
                    intent.putExtra("edit_index", which);
                    mediaEditLauncher.launch(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showImagePreviewDialog(int position) {
        Uri imageUri = imageUris.get(position);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        ImageView imageView = dialogView.findViewById(R.id.preview_image);
        Button editButton = dialogView.findViewById(R.id.edit_button);
        imageView.setImageURI(imageUri);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        editButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, MediaEditActivity.class);
            intent.putExtra(MediaEditActivity.EXTRA_MEDIA_URI, imageUri);
            intent.putExtra(MediaEditActivity.EXTRA_IS_VIDEO, false);
            intent.putExtra("edit_index", position);
            mediaEditLauncher.launch(intent);
        });

        dialog.show();
    }


    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Tutorial", "Pattern", "Forum")
        );
        categorySpinner.setAdapter(adapter);
    }

    private void suggestCategory(String tab) {
        switch (tab.toLowerCase()) {
            case "explore":
                categorySpinner.setSelection(0); // Tutorial
                break;
            case "forum":
                categorySpinner.setSelection(2); // Forum
                break;
        }
    }

    private void selectMedia() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        mediaPickerLauncher.launch(intent);
    }


    private void uploadPost() {
        if ((imageUris.isEmpty() && videoUri == null) || titleInput.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select media and enter title.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to post.", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadProgress.setVisibility(View.VISIBLE);
        postButton.setEnabled(false);

        String userId = currentUser.getUid();
        List<String> uploadedUrls = new ArrayList<>();
        List<Uri> uploadTargets = videoUri != null ? Collections.singletonList(videoUri) : imageUris;

        uploadRecursive(uploadTargets, 0, uploadedUrls, () -> {
            Map<String, Object> post = new HashMap<>();
            post.put("userId", userId);
            post.put("username", currentUser.getDisplayName());
            post.put("userAvatar", currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
            post.put("title", titleInput.getText().toString());
            post.put("content", descriptionInput.getText().toString());
            post.put("mediaUrls", uploadedUrls);
            post.put("mediaType", videoUri != null ? "video" : "image");
            post.put("category", categorySpinner.getSelectedItem().toString());
            post.put("timestamp", FieldValue.serverTimestamp());
            post.put("likeCount", 0);

            firestore.collection("posts").add(post)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Upload Successful", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(this::showError);
        });
    }


    private void uploadRecursive(List<Uri> uris, int index, List<String> urls, Runnable onComplete) {
        if (index >= uris.size()) {
            onComplete.run();
            return;
        }

        Uri uri = uris.get(index);
        String filename = UUID.randomUUID().toString();
        StorageReference ref = storage.getReference().child("posts/" + auth.getUid() + "/" + filename);

        ref.putFile(uri).addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(url -> {
            urls.add(url.toString());
            uploadRecursive(uris, index + 1, urls, onComplete);
        })).addOnFailureListener(this::showError);
    }

    private void showError(Exception e) {
        uploadProgress.setVisibility(View.GONE);
        postButton.setEnabled(true);
        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private boolean checkIfVideo(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi");
    }

    private void updateMediaPreview() {
        if (videoUri != null) {
            previewVideo.setVisibility(View.VISIBLE);
            imageRecycler.setVisibility(View.GONE);
            addMoreImageButton.setVisibility(View.GONE);
            previewVideo.setVideoURI(videoUri);
            previewVideo.start();
        } else {
            previewVideo.setVisibility(View.GONE);
            imageRecycler.setVisibility(View.VISIBLE);
            addMoreImageButton.setVisibility(View.VISIBLE);
        }
        findViewById(R.id.edit_media_button).setVisibility(View.VISIBLE);
    }
}
