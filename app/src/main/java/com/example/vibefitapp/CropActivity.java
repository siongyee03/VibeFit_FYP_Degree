package com.example.vibefitapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.UUID;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.composer.Mp4Composer;

public class CropActivity extends AppCompatActivity {

    private boolean isVideo;
    private Uri mediaUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        Intent intent = getIntent();
        mediaUri = Uri.parse(intent.getStringExtra("media_uri"));
        isVideo = intent.getBooleanExtra("is_video", false);

        if (isVideo) {
            cropVideo(mediaUri);
        } else {
            cropImage(mediaUri);
        }
    }

    private void cropImage(Uri imageUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_" + UUID.randomUUID() + ".jpg"));

        UCrop.Options options = new UCrop.Options();
        options.setToolbarTitle("Crop Image");
        options.setFreeStyleCropEnabled(true);
        options.setHideBottomControls(false);
        options.setShowCropGrid(true);
        options.setShowCropFrame(true);

        UCrop.of(imageUri, destinationUri)
                .withOptions(options)
                .start(CropActivity.this);
    }


    private void cropVideo(Uri videoUri) {
        String outputPath = getExternalFilesDir(Environment.DIRECTORY_MOVIES) + "/cropped_" + UUID.randomUUID() + ".mp4";

        new Mp4Composer(videoUri.toString(), outputPath)
                .size(720, 720)
                .fillMode(FillMode.PRESERVE_ASPECT_CROP)
                .listener(new Mp4Composer.Listener() {
                    @Override
                    public void onProgress(double progress) {
                    }

                    @Override
                    public void onCompleted() {
                        Intent result = new Intent();
                        result.putExtra("edited_media_path", outputPath);
                        setResult(RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onCanceled() {
                        Toast.makeText(CropActivity.this, "Video crop canceled", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }

                    @Override
                    public void onFailed(Exception exception) {
                        Toast.makeText(CropActivity.this, "Crop failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }

                    @Override
                    public void onCurrentWrittenVideoTime(long timeUs) {
                        Log.d("Mp4Composer", "Written video time (us): " + timeUs);
                    }
                })
                .start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null) {
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    Intent result = new Intent();
                    result.putExtra("edited_media_path", resultUri.getPath());
                    setResult(RESULT_OK, result);
                } else {
                    Toast.makeText(this, "Failed to get cropped image URI", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                }
            } else {
                Toast.makeText(this, "No data returned from crop", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
            }

        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                final Throwable cropError = UCrop.getError(data);
                if (cropError != null) {
                    Toast.makeText(this, "Crop error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Unknown crop error occurred", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No data returned on error", Toast.LENGTH_SHORT).show();
            }
            setResult(RESULT_CANCELED);
        }

        finish();
    }
}
