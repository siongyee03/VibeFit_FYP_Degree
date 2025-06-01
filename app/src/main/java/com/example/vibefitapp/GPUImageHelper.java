package com.example.vibefitapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class GPUImageHelper {
    public interface FilterCallback {
        void onFiltered(Bitmap result);
    }

    public static void applyFilterAsync(Context context, Bitmap original, GPUImageFilter filter, FilterCallback callback) {
        new Thread(() -> {
            GPUImage gpuImage = new GPUImage(context);
            gpuImage.setImage(original);
            if (filter != null) gpuImage.setFilter(filter);
            Bitmap result = gpuImage.getBitmapWithFilterApplied();

            ((Activity) context).runOnUiThread(() -> callback.onFiltered(result));
        }).start();
    }
}
