package com.example.vibefitapp;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.*;

import android.text.Editable;
import android.text.TextWatcher;

public class UploadPostActivity extends AppCompatActivity {

    private static final int MAX_IMAGE_COUNT = 9;
    private final List<Uri> imageUris = new ArrayList<>();
    private RecyclerView imageRecycler;
    private TextInputEditText titleInput, descriptionInput;
    private LinearLayout forumFieldsLayout;
    private TextInputEditText forumTopicInput, forumStepsInput;
    private Spinner forumDifficultySpinner;
    private Spinner categorySpinner;
    private ProgressBar uploadProgress;
    private Button postButton;
    private ImageButton backButton;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private ImagePreviewAdapter adapter;
    private boolean isEditMode = false;
    private String editingPostId = null;
    private Post editingPost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_post);

        imageRecycler = findViewById(R.id.image_recycler);
        titleInput = findViewById(R.id.title_input);
        descriptionInput = findViewById(R.id.description_input);
        categorySpinner = findViewById(R.id.category_spinner);
        uploadProgress = findViewById(R.id.uploadProgress);
        postButton = findViewById(R.id.post_button);
        backButton = findViewById(R.id.back_button);

        forumFieldsLayout = findViewById(R.id.forum_fields);
        forumTopicInput = findViewById(R.id.forum_topic);
        forumStepsInput = findViewById(R.id.forum_steps);
        forumDifficultySpinner = findViewById(R.id.forum_difficulty);

        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupCategorySpinner();

        imageRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        adapter = new ImagePreviewAdapter(
                this,
                imageUris,
                position -> {
                    if (imageUris.size() == 1) {
                        Toast.makeText(this, "Add at least one photo to continue.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    imageUris.remove(position);
                    adapter.notifyItemRemoved(position);
                    if (imageUris.isEmpty()) {
                        imageRecycler.setVisibility(View.GONE);
                    }
                },
                this::showImagePreviewDialog,
                this::selectMedia
        );
        if (!isEditMode) {
            imageRecycler.setAdapter(adapter);
        }

        Intent intent = getIntent();
        if (intent != null) {
            ArrayList<String> imageUriStrs = intent.getStringArrayListExtra("image_uris");

            if (imageUriStrs != null && !imageUriStrs.isEmpty()) {
                imageUris.clear();
                for (String uriStr : imageUriStrs) {
                    imageUris.add(Uri.parse(uriStr));
                }
            }
        }

        updateMediaPreview();

        postButton.setOnClickListener(v -> uploadPost());
        backButton.setOnClickListener(v -> finish());


        // Optional: auto-suggest category from originating tab
        String sourceTab = getIntent().getStringExtra("source_tab");
        if (sourceTab != null) {
            suggestCategory(sourceTab);
        }

        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            int currentImageCount = imageUris.size();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                if (currentImageCount + i < MAX_IMAGE_COUNT) {
                                    Uri uri = clipData.getItemAt(i).getUri();

                                    imageUris.add(uri);
                                } else {
                                    Toast.makeText(this, "You can select up to " + MAX_IMAGE_COUNT + " images.", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                            }
                        } else if (data.getData() != null) {
                            Uri uri = data.getData();
                            String type = getContentResolver().getType(uri);

                            if (type != null && type.startsWith("image")) {
                                if (imageUris.size() < MAX_IMAGE_COUNT) {
                                    imageUris.add(uri);
                                } else {
                                    Toast.makeText(this, "You can select up to " + MAX_IMAGE_COUNT + " images.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        updateMediaPreview();
                    }
                }
        );

        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    titleInput.setError("Title cannot be empty");
                } else {
                    titleInput.setError(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        descriptionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    descriptionInput.setError("Description cannot be empty");
                } else {
                    descriptionInput.setError(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        //edit post
        Post editingPost = getIntent().getParcelableExtra("editing_post");
        if (editingPost != null) {
            isEditMode = true;
            this.editingPost = editingPost;
            editingPostId = editingPost.getId();

            titleInput.setText(editingPost.getTitle());
            descriptionInput.setText(editingPost.getContent());

            if ("forum".equalsIgnoreCase(editingPost.getCategory())) {
                forumFieldsLayout.setVisibility(View.VISIBLE);

                titleInput.setVisibility(View.GONE);
                descriptionInput.setVisibility(View.GONE);
                imageRecycler.setVisibility(View.GONE);
                findViewById(R.id.titleInputLayout).setVisibility(View.GONE);
                findViewById(R.id.descriptionInputLayout).setVisibility(View.GONE);

                forumTopicInput.setText(editingPost.getForumTopic());
                forumStepsInput.setText(editingPost.getForumSteps());

                String difficulty = editingPost.getForumDifficulty();
                if (difficulty != null) {
                    SpinnerAdapter spinnerAdapter = forumDifficultySpinner.getAdapter();
                    if (spinnerAdapter instanceof ArrayAdapter) {
                        @SuppressWarnings("unchecked")
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerAdapter;
                        int index = adapter.getPosition(difficulty);
                        if (index >= 0) forumDifficultySpinner.setSelection(index);
                    }
                }

                SpinnerAdapter spinnerAdapter = categorySpinner.getAdapter();
                if (spinnerAdapter instanceof ArrayAdapter) {
                    @SuppressWarnings("unchecked")
                    ArrayAdapter<String> arrayAdapter = (ArrayAdapter<String>) spinnerAdapter;
                    int forumIndex = arrayAdapter.getPosition("Forum");
                    if (forumIndex >= 0) {
                        categorySpinner.setSelection(forumIndex);
                        categorySpinner.setEnabled(false);
                    }
                }

            } else {
                forumFieldsLayout.setVisibility(View.GONE);

                titleInput.setVisibility(View.VISIBLE);
                descriptionInput.setVisibility(View.VISIBLE);
                findViewById(R.id.titleInputLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.descriptionInputLayout).setVisibility(View.VISIBLE);
            }

            String category = editingPost.getCategory();
            if (category != null) {
                SpinnerAdapter rawAdapter = categorySpinner.getAdapter();
                if (rawAdapter instanceof ArrayAdapter) {
                    @SuppressWarnings("unchecked")
                    ArrayAdapter<String> adapterSpinner = (ArrayAdapter<String>) rawAdapter;
                    int pos = adapterSpinner.getPosition(category);
                    if (pos >= 0) categorySpinner.setSelection(pos);
                }
            }

            List<String> mediaUrls = editingPost.getMediaUrls();

            if (editingPost.getMediaType() != null && editingPost.getMediaType().equals("image")) {
                imageUris.clear();

                for (String url : mediaUrls) {
                    imageUris.add(Uri.parse(url));
                }
            }

            updateMediaPreview();

            postButton.setText(getString(R.string.update_post));
        }

        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Beginner", "Intermediate", "Advanced")
        );
        forumDifficultySpinner.setAdapter(difficultyAdapter);

    }

    private void showImagePreviewDialog(int position) {
        Uri imageUri = imageUris.get(position);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        ImageView imageView = dialogView.findViewById(R.id.preview_image);
        imageView.setImageURI(imageUri);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        dialog.show();
    }

    private void setupCategorySpinner() {
        List<String> displayCategories = new ArrayList<>();
        for (String display : CategoryUtil.displayToValueMap.keySet()) {
            if (!display.equals("Trends")) {
                displayCategories.add(display);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                displayCategories
        );
        categorySpinner.setAdapter(adapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (isEditMode && editingPost != null && "forum".equalsIgnoreCase(editingPost.getCategory())) {
                    return;
                }

                String selected = parent.getItemAtPosition(position).toString();

                boolean isForum = selected.equalsIgnoreCase("Forum");

                forumFieldsLayout.setVisibility(isForum ? View.VISIBLE : View.GONE);

                titleInput.setVisibility(isForum ? View.GONE : View.VISIBLE);
                descriptionInput.setVisibility(isForum ? View.GONE : View.VISIBLE);
                imageRecycler.setVisibility(isForum ? View.GONE : View.VISIBLE);
                findViewById(R.id.titleInputLayout).setVisibility(isForum ? View.GONE : View.VISIBLE);
                findViewById(R.id.descriptionInputLayout).setVisibility(isForum ? View.GONE : View.VISIBLE);

                if (!isForum && imageUris.isEmpty()) {
                    Toast.makeText(UploadPostActivity.this, "Don't forget to add at least one photo for this post!", Toast.LENGTH_SHORT).show();
                    selectMedia();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }

    private void suggestCategory(String tab) {
        if (tab == null) return;

        switch (tab.toLowerCase()) {
            case "explore":
                categorySpinner.setSelection(0); // Tutorial
                break;
            case "forum":
                categorySpinner.setSelection(2); // Forum
                break;
            default:
                categorySpinner.setSelection(0);
                break;
        }
    }


    private void selectMedia() {
        String selectedDisplayCategory = categorySpinner.getSelectedItem().toString();
        String actualCategory = CategoryUtil.displayToValueMap.getOrDefault(selectedDisplayCategory, "tutorial");

        if ("forum".equalsIgnoreCase(actualCategory)) {
            Toast.makeText(this, "Forum posts do not support media uploads.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditMode) {
            Toast.makeText(this, "Edit mode is on. You can’t add new media now.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUris.size() >= MAX_IMAGE_COUNT) {
            Toast.makeText(this, "You have reached the maximum of " + MAX_IMAGE_COUNT + " images.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        mediaPickerLauncher.launch(intent);
    }

    private void uploadPost() {
        String selectedDisplayCategory = categorySpinner.getSelectedItem().toString();
        String actualCategory = CategoryUtil.displayToValueMap.getOrDefault(selectedDisplayCategory, "tutorial");
        boolean isForum = "forum".equalsIgnoreCase(actualCategory);

        if (!postButton.isEnabled()) return;
        postButton.setEnabled(false);

        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        String forumTopic = forumTopicInput.getText().toString().trim();
        String forumSteps = forumStepsInput.getText().toString().trim();

        if (isForum) {
            if (forumTopic.isEmpty()) {
                forumTopicInput.setError("Topic cannot be empty");
                forumTopicInput.requestFocus();
                postButton.setEnabled(true);
                uploadProgress.setVisibility(View.GONE);
                return;
            }
            if (forumSteps.isEmpty()) {
                forumStepsInput.setError("Steps/Details cannot be empty");
                forumStepsInput.requestFocus();
                postButton.setEnabled(true);
                uploadProgress.setVisibility(View.GONE);
                return;
            }

        } else {
            if (title.isEmpty()) {
                titleInput.setError("Title cannot be empty");
                titleInput.requestFocus();
                postButton.setEnabled(true);
                uploadProgress.setVisibility(View.GONE);
                return;
            }

            if (description.isEmpty()) {
                descriptionInput.setError("Description cannot be empty");
                descriptionInput.requestFocus();
                postButton.setEnabled(true);
                uploadProgress.setVisibility(View.GONE);
                return;
            }

            if (imageUris.isEmpty()) {
                Toast.makeText(this, "Please select at least one media file.", Toast.LENGTH_SHORT).show();
                postButton.setEnabled(true);
                uploadProgress.setVisibility(View.GONE);
                return;
            }
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You're not logged in. Please sign in to post.", Toast.LENGTH_SHORT).show();
            postButton.setEnabled(true);
            uploadProgress.setVisibility(View.GONE);
            return;
        }

        uploadProgress.setVisibility(View.VISIBLE);
        String userId = currentUser.getUid();

        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    String username = snapshot.getString("username");
                    String avatarUrl = snapshot.getString("profileImageUrl");

                    if (isEditMode) {
                        Map<String, Object> updates = new HashMap<>();

                        updates.put("category", actualCategory);

                        if (isForum) {
                            updates.put("forumTopic", forumTopic);
                            updates.put("forumSteps", forumSteps);
                            updates.put("forumDifficulty", forumDifficultySpinner.getSelectedItem().toString());

                            updates.put("title", null);
                            updates.put("content", null);
                            updates.put("mediaUrls", new ArrayList<>());
                            updates.put("mediaType", "text");
                        } else {
                            updates.put("title", title);
                            updates.put("content", description);

                            updates.remove("forumTopic");
                            updates.remove("forumSteps");
                            updates.remove("forumDifficulty");

                            List<String> currentMediaUrls = new ArrayList<>();
                            for (Uri uri : imageUris) {
                                currentMediaUrls.add(uri.toString());
                            }
                            updates.put("mediaUrls", currentMediaUrls);
                            updates.put("mediaType", "image");
                        }

                        firestore.collection("posts").document(editingPostId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    uploadProgress.setVisibility(View.GONE);
                                    Toast.makeText(this, "Post updated!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    uploadProgress.setVisibility(View.GONE);
                                    postButton.setEnabled(true);
                                    Toast.makeText(this, "Update failed. Please try again.", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        List<String> uploadedUrls = new ArrayList<>();

                        if (!isForum && !imageUris.isEmpty()) {
                            uploadRecursive(imageUris, 0, uploadedUrls, () -> createAndSavePost(userId, username, avatarUrl, actualCategory, title, description, uploadedUrls, forumTopic, forumSteps, forumDifficultySpinner.getSelectedItem().toString()));
                        } else if (isForum) {
                            createAndSavePost(userId, username, avatarUrl, actualCategory, title, description, uploadedUrls, forumTopic, forumSteps, forumDifficultySpinner.getSelectedItem().toString());
                        } else {
                            uploadProgress.setVisibility(View.GONE);
                            postButton.setEnabled(true);
                            Toast.makeText(this, "Internal error: No media for non-forum post.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    uploadProgress.setVisibility(View.GONE);
                    postButton.setEnabled(true);
                    Toast.makeText(this, "Failed to get user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createAndSavePost(String userId, String username, String avatarUrl, String actualCategory,
                                   String title, String description, List<String> uploadedUrls,
                                   String forumTopic, String forumSteps, String forumDifficulty) {

        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("username", username != null ? username : "Unknown");
        post.put("userAvatar", avatarUrl);

        if ("forum".equalsIgnoreCase(actualCategory)) {
            post.put("forumTopic", forumTopic);
            post.put("forumSteps", forumSteps);
            post.put("forumDifficulty", forumDifficulty);
            post.put("mediaUrls", new ArrayList<>());
            post.put("mediaType", "text");
            post.put("title", null);
            post.put("content", null);
        } else {
            post.put("title", title);
            post.put("content", description);
            post.put("mediaUrls", uploadedUrls);
            post.put("mediaType", "image");
            post.remove("forumTopic");
            post.remove("forumSteps");
            post.remove("forumDifficulty");
        }

        post.put("category", actualCategory);
        post.put("timestamp", FieldValue.serverTimestamp());
        post.put("likeCount", 0);
        post.put("favouriteCount", 0);
        post.put("commentCount", 0);

        firestore.collection("posts").add(post)
                .addOnSuccessListener(ref -> {
                    uploadProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Uploaded successfully.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.putExtra("target_tab", getIntent().getStringExtra("source_tab"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    uploadProgress.setVisibility(View.GONE);
                    postButton.setEnabled(true);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadRecursive(List<Uri> uris, int index, List<String> urls, Runnable onComplete) {
        if (index >= uris.size()) {
            onComplete.run();
            return;
        }

        Uri uri = uris.get(index);
        String filename = UUID.randomUUID().toString();
        String folder = "images";
        StorageReference ref = storage.getReference().child("posts/" + auth.getUid() + "/" + folder + "/" + filename);

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

    private void updateMediaPreview() {
        if (isEditMode) {
            imageRecycler.setAdapter(null);
            imageRecycler.setVisibility(View.GONE);
            return;
        }

        if (!imageUris.isEmpty()) {
            imageRecycler.setVisibility(View.VISIBLE);

            if (imageRecycler.getAdapter() == null) {
                imageRecycler.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }

        } else {
            imageRecycler.setVisibility(View.GONE);
        }
    }
}
