package com.example.vibefitapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {
    private static final String TAG = "ImageUtils";

    public static Uri saveBitmapToCache(Context context, Bitmap bitmap) {
        File cacheDir = context.getCacheDir();
        File imageFile = new File(cacheDir, "captured_" + System.currentTimeMillis() + ".jpg");

        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save bitmap to cache", e);
            return null;
        }

        return Uri.fromFile(imageFile);
    }
}

