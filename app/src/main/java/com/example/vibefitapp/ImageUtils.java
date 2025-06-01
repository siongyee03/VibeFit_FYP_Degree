package com.example.vibefitapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static Uri saveBitmapToCache(Context context, Bitmap bitmap) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "images");
        if (!cacheDir.exists()) {
            boolean created = cacheDir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directory: " + cacheDir.getAbsolutePath());
            }
        }
        File file = new File(cacheDir, "filtered_image_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
        }
        return Uri.fromFile(file);
    }

}
