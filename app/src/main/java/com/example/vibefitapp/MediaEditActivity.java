package com.example.vibefitapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;

public class MediaEditActivity extends AppCompatActivity {

    public static final String EXTRA_MEDIA_URI = "media_uri";
    public static final String EXTRA_IS_VIDEO = "is_video";
    public static final String RESULT_MEDIA_PATH = "edited_media_path";

    private Uri mediaUri;
    private boolean isVideo;

    private ImageView imagePreview;
    private VideoView videoPreview;
    private ImageButton btnFilter, btnCrop, btnTrim, btnText;
    private Button btnCancel, btnDone;
    private int editIndex = -1;
    private String editedMediaPath = null;
    private ActivityResultLauncher<Intent> editLauncher;

    private final ArrayList<Uri> imageUris = new ArrayList<>();
    private final ArrayList<String> editedImagePaths = new ArrayList<>();
    private int currentImageIndex = 0;

    private FrameLayout overlayContainer;
    private final ArrayList<TextOverlayData> textOverlays = new ArrayList<>();
    private final ArrayList<DraggableTextView> textViews = new ArrayList<>();

    private int editingTextIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_edit);

        imagePreview = findViewById(R.id.image_preview);
        videoPreview = findViewById(R.id.video_preview);
        btnFilter = findViewById(R.id.btn_filter);
        btnCrop = findViewById(R.id.btn_crop);
        btnTrim = findViewById(R.id.btn_trim);
        btnText = findViewById(R.id.btn_text);
        btnCancel = findViewById(R.id.btn_cancel);
        btnDone = findViewById(R.id.btn_done);
        editIndex = getIntent().getIntExtra("edit_index", -1);
        ImageButton btnPrev = findViewById(R.id.btn_prev);
        ImageButton btnNext = findViewById(R.id.btn_next);
        TextView tvIndex = findViewById(R.id.tv_index);
        LinearLayout navBar = findViewById(R.id.image_nav_bar);
        overlayContainer = findViewById(R.id.overlay_container);

        if (!imageUris.isEmpty()) {
            navBar.setVisibility(View.VISIBLE);
            updateIndexDisplay(tvIndex);
        }

        if (!imageUris.isEmpty()) {
            navBar.setVisibility(View.VISIBLE);
            updateIndexDisplay(tvIndex);
        }

        btnPrev.setOnClickListener(v -> {
            if (currentImageIndex > 0) {
                saveCurrentEdit();
                currentImageIndex--;
                mediaUri = imageUris.get(currentImageIndex);
                editedMediaPath = editedImagePaths.get(currentImageIndex);
                reloadPreview();
                updateIndexDisplay(tvIndex);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentImageIndex < imageUris.size() - 1) {
                saveCurrentEdit();
                currentImageIndex++;
                mediaUri = imageUris.get(currentImageIndex);
                editedMediaPath = editedImagePaths.get(currentImageIndex);
                reloadPreview();
                updateIndexDisplay(tvIndex);
            }
        });

        ArrayList<String> imageUriStrings = getIntent().getStringArrayListExtra("image_uris");
        if (imageUriStrings != null && !imageUriStrings.isEmpty()) {
            for (String uriStr : imageUriStrings) {
                imageUris.add(Uri.parse(uriStr));
                editedImagePaths.add(null);
            }
            currentImageIndex = 0;
            mediaUri = imageUris.get(0);
            isVideo = false;
        } else {
            mediaUri = getIntent().getParcelableExtra(EXTRA_MEDIA_URI);
            isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);
        }

        setupMediaPreview();
        setupListeners();
        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String editType = data.getStringExtra("edit_type");

                        if (editType == null) return;

                        switch (editType) {
                            case "filter":
                                handleFilterResult(data);
                                break;
                            case "crop":
                                handleCropResult(data);
                                break;
                            case "trim":
                                handleTrimResult(data);
                                break;
                            case "text":
                                handleTextOverlayResult(data);
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

    private void setupMediaPreview() {
        if (isVideo) {
            videoPreview.setVisibility(View.VISIBLE);
            videoPreview.setVideoURI(mediaUri);
            videoPreview.setOnPreparedListener(mp -> mp.setLooping(true));
            videoPreview.start();

            imagePreview.setVisibility(View.GONE);
        } else {
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setImageURI(mediaUri);

            videoPreview.setVisibility(View.GONE);
        }

        btnTrim.setVisibility(isVideo ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        btnFilter.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilterActivity.class);
            intent.putExtra(EXTRA_MEDIA_URI, mediaUri.toString());
            intent.putExtra("mediaType", isVideo ? "video" : "image");
            intent.putExtra("edit_type", "filter");
            editLauncher.launch(intent);
        });

        btnCrop.setOnClickListener(v -> {
            Intent intent = new Intent(this, CropActivity.class);
            intent.putExtra(EXTRA_MEDIA_URI , mediaUri.toString());
            intent.putExtra(EXTRA_IS_VIDEO , isVideo);
            intent.putExtra("edit_type", "crop");
            editLauncher.launch(intent);
        });

        btnTrim.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrimActivity.class);
            intent.putExtra(TrimActivity.EXTRA_VIDEO_URI, mediaUri.toString());
            intent.putExtra("edit_type", "trim");
            editLauncher.launch(intent);
        });

        btnText.setOnClickListener(v -> {
            editingTextIndex = -1;
            Intent intent = new Intent(this, TextOverlayActivity.class);
            intent.putExtra(EXTRA_MEDIA_URI, mediaUri.toString());
            intent.putExtra(EXTRA_IS_VIDEO, isVideo);
            intent.putExtra("edit_type", "text");
            editLauncher.launch(intent);
        });

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnDone.setOnClickListener(v -> {
            saveCurrentEdit();

            Intent intent = new Intent(MediaEditActivity.this, UploadPostActivity.class);
            if (isVideo) {
                intent.putExtra("video_uri", editedMediaPath);
            } else {
                ArrayList<String> resultUris = new ArrayList<>();
                for (int i = 0; i < imageUris.size(); i++) {
                    String editedPath = editedImagePaths.get(i);
                    resultUris.add(editedPath != null ? editedPath : imageUris.get(i).toString());
                }
                intent.putStringArrayListExtra("image_uris", resultUris);
            }

            String recommendedCategory = getIntent().getStringExtra("recommended_category");
            if (recommendedCategory != null) {
                intent.putExtra("recommended_category", recommendedCategory);
            }

            startActivity(intent);
            finish();
        });
    }

    private void reloadPreview() {
        if (isVideo) {
            videoPreview.setVideoURI(mediaUri);
            videoPreview.start();
        } else {
            imagePreview.setImageBitmap(null);
            imagePreview.setImageURI(null);
            imagePreview.setImageURI(mediaUri);
        }
    }

    private void updateIndexDisplay(TextView tv) {
        String text = getString(R.string.image_index_format, currentImageIndex + 1, imageUris.size());
        tv.setText(text);
    }

    private void saveCurrentEdit() {
        if (editedMediaPath == null) {
            editedMediaPath = mediaUri.toString();
        }
        editedImagePaths.set(currentImageIndex, editedMediaPath);
    }

    private void handleFilterResult(Intent data) {
        String newPath = data.getStringExtra(RESULT_MEDIA_PATH);
        if (newPath != null) {
            editedMediaPath = newPath;
            mediaUri = Uri.fromFile(new File(newPath));
            if (!isVideo) editedImagePaths.set(currentImageIndex, editedMediaPath);
            reloadPreview();
        }
    }

    private void handleCropResult(Intent data) {
        String newPath = data.getStringExtra(RESULT_MEDIA_PATH);
        if (newPath != null) {
            editedMediaPath = newPath;
            mediaUri = Uri.fromFile(new File(newPath));
            if (!isVideo) editedImagePaths.set(currentImageIndex, editedMediaPath);
            reloadPreview();
        }
    }

    private void handleTrimResult(Intent data) {
        String newPath = data.getStringExtra(RESULT_MEDIA_PATH);
        if (newPath != null) {
            editedMediaPath = newPath;
            mediaUri = Uri.fromFile(new File(newPath));
            if (isVideo) {
                reloadPreview();
            }
        }
    }
    private void handleTextOverlayResult(Intent data) {
        String text = data.getStringExtra(TextOverlayActivity.RESULT_TEXT);
        int color = data.getIntExtra(TextOverlayActivity.RESULT_TEXT_COLOR, 0xFFFFFFFF);
        float x = data.getFloatExtra(TextOverlayActivity.RESULT_TEXT_X, 0f);
        float y = data.getFloatExtra(TextOverlayActivity.RESULT_TEXT_Y, 0f);
        String fontPath = data.getStringExtra(TextOverlayActivity.RESULT_TEXT_FONT_PATH);

        if (text == null || text.isEmpty()) return;

        if (editingTextIndex == -1) {
            TextOverlayData newData = new TextOverlayData(text, color, x, y, fontPath);
            textOverlays.add(newData);
            addTextView(newData, textOverlays.size() - 1);
        } else {
            TextOverlayData dataToEdit = textOverlays.get(editingTextIndex);
            dataToEdit.text = text;
            dataToEdit.color = color;
            dataToEdit.x = x;
            dataToEdit.y = y;
            dataToEdit.fontPath = fontPath;

            DraggableTextView tv = textViews.get(editingTextIndex);
            tv.setText(text);
            tv.setTextColor(color);
            if (fontPath != null) {
                tv.setTypeface(Typeface.createFromAsset(getAssets(), fontPath));
            }
            tv.setX(x);
            tv.setY(y);
        }
    }



    private void addTextView(TextOverlayData data, int index) {
        DraggableTextView tv = new DraggableTextView(this);
        tv.setText(data.text);
        tv.setTextColor(data.color);
        if (data.fontPath != null) {
            tv.setTypeface(Typeface.createFromAsset(getAssets(), data.fontPath));
        }
        tv.setX(data.x);
        tv.setY(data.y);
        tv.setTextSize(20);
        tv.setPadding(10, 10, 10, 10);
        tv.setBackgroundColor(0x55FFFFFF);

        tv.setOnClickListener(v -> {
            editingTextIndex = index;
            showEditDeleteDialog(index);
        });

        overlayContainer.addView(tv);
        textViews.add(tv);
    }

    private void showEditDeleteDialog(int index) {
        String[] options = {"Edit", "Delete"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("TextBox")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        editingTextIndex = index;
                        TextOverlayData data = textOverlays.get(index);
                        Intent intent = new Intent(this, TextOverlayActivity.class);
                        intent.putExtra(EXTRA_MEDIA_URI, mediaUri.toString());
                        intent.putExtra(EXTRA_IS_VIDEO, isVideo);
                        intent.putExtra(TextOverlayActivity.EXTRA_TEXT, data.text);
                        intent.putExtra(TextOverlayActivity.EXTRA_TEXT_COLOR, data.color);
                        intent.putExtra(TextOverlayActivity.EXTRA_TEXT_X, data.x);
                        intent.putExtra(TextOverlayActivity.EXTRA_TEXT_Y, data.y);
                        intent.putExtra(TextOverlayActivity.EXTRA_TEXT_FONT_PATH, data.fontPath);
                        editLauncher.launch(intent);
                    } else if (which == 1) {
                        // 删除
                        overlayContainer.removeView(textViews.get(index));
                        textViews.remove(index);
                        textOverlays.remove(index);
                        editingTextIndex = -1;
                    }
                })
                .show();
    }

    private Intent createTextOverlayIntent(int index) {
        TextOverlayData data = textOverlays.get(index);
        Intent intent = new Intent(this, TextOverlayActivity.class);
        intent.putExtra(EXTRA_MEDIA_URI, mediaUri.toString());
        intent.putExtra(EXTRA_IS_VIDEO, isVideo);
        intent.putExtra(TextOverlayActivity.EXTRA_TEXT, data.text);
        intent.putExtra(TextOverlayActivity.EXTRA_TEXT_COLOR, data.color);
        intent.putExtra(TextOverlayActivity.EXTRA_TEXT_X, data.x);
        intent.putExtra(TextOverlayActivity.EXTRA_TEXT_Y, data.y);
        intent.putExtra(TextOverlayActivity.EXTRA_TEXT_FONT_PATH, data.fontPath);
        return intent;
    }
}
