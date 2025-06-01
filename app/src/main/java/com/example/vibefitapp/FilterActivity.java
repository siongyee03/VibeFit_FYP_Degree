package com.example.vibefitapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter;

public class FilterActivity extends Activity {

    private GPUImageView gpuImageView;
    private RecyclerView filtersRecyclerView;
    private Uri imageUri;
    private Bitmap originalBitmap;
    private GPUImage gpuImage;

    private GPUImageFilter selectedFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        gpuImageView = findViewById(R.id.gpu_image_view);
        filtersRecyclerView = findViewById(R.id.filters_recycler_view);

        imageUri = getIntent().getParcelableExtra("imageUri");
        String mediaType = getIntent().getStringExtra("mediaType"); // "image" or "video"

        if (!"image".equals(mediaType)) {
            finish();
            return;
        }

        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            originalBitmap = BitmapFactory.decodeStream(inputStream);
            gpuImage = new GPUImage(this);
            gpuImage.setImage(originalBitmap);
            gpuImageView.setImage(originalBitmap);
            setupFilters();
        } catch (Exception e) {
            Log.e("FilterActivity", "Error loading image", e);
        }

        findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            gpuImage.setFilter(selectedFilter);
            gpuImage.setImage(originalBitmap);
            Bitmap filteredBitmap = gpuImage.getBitmapWithFilterApplied();

            Uri resultUri = null;
            try {
                resultUri = ImageUtils.saveBitmapToCache(this, filteredBitmap);
            } catch (IOException e) {
                Log.e("FilterActivity", "Failed to save filtered image", e);
            }

            Intent resultIntent = new Intent();
            if (resultUri != null) {
                resultIntent.putExtra("editedUri", resultUri.toString());
            }
            resultIntent.putExtra("mediaType", "image");
            setResult(RESULT_OK, resultIntent);

            finish();
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupFilters() {
        List<FilterItem> filterItems = new ArrayList<>();
        filterItems.add(new FilterItem("Original", null));
        filterItems.add(new FilterItem("GrayScale", new GPUImageGrayscaleFilter()));
        filterItems.add(new FilterItem("Invert", new GPUImageColorInvertFilter()));

        FilterAdapter adapter = new FilterAdapter(this, filterItems, originalBitmap, filter -> {
                    selectedFilter = filter;
                    gpuImageView.setFilter(filter);
                });

        filtersRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        filtersRecyclerView.setAdapter(adapter);
    }
}
