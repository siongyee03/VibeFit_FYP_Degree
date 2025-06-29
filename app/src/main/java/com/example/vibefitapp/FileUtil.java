package com.example.vibefitapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static File from(Context context, Uri uri) throws IOException {
        String fileName = getFileName(context, uri);
        if (fileName == null) {
            throw new IOException("Cannot retrieve file name from URI.");
        }

        String prefix;
        String suffix;

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            prefix = fileName.substring(0, dotIndex);
            suffix = fileName.substring(dotIndex);
        } else {
            prefix = fileName;
            suffix = ".tmp";
        }

        if (prefix.length() < 3) {
            prefix = (prefix + "___").substring(0, 3);
        }

        File tempFile = File.createTempFile(prefix, suffix, context.getCacheDir());

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            if (inputStream == null) {
                throw new IOException("Unable to open InputStream from URI.");
            }

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }

        return tempFile;
    }

    private static String getFileName(Context context, Uri uri) {
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        if (returnCursor != null) {
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            String name = null;
            if (nameIndex != -1 && returnCursor.moveToFirst()) {
                name = returnCursor.getString(nameIndex);
            }
            returnCursor.close();
            return name;
        }
        return null;
    }

    public static Uri saveBitmapToUri(Context context, Bitmap bitmap) {
        OutputStream outputStream = null;
        try {
            String fileName = "user_face_" + System.currentTimeMillis() + ".png";
            Uri uri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/VibeFitApp");

                uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("Failed to create new MediaStore record.");
                outputStream = context.getContentResolver().openOutputStream(uri);
            } else {
                File file = new File(context.getExternalCacheDir(), fileName);
                outputStream = new FileOutputStream(file);
                uri = Uri.fromFile(file);
            }

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
            }

            return uri;
        } catch (IOException e) {
            Log.e(TAG, "Failed to save bitmap to Uri", e);
            return null;
        } finally {
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException ignored) {}
            }
        }
    }

    public static void saveBitmapToGallery(Context context, Bitmap bitmap, String filename) {
        OutputStream outputStream = null;
        try {
            Uri imageUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/VibeFitApp");

                imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri == null) {
                    Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show();
                    return;
                }

                outputStream = context.getContentResolver().openOutputStream(imageUri);
            } else {
                File imagesDir = new File(context.getExternalFilesDir(null), "VibeFitApp");
                boolean dirReady = imagesDir.exists() || imagesDir.mkdirs();
                if (!dirReady) {
                    Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show();
                    return;
                }

                File image = new File(imagesDir, filename + ".png");
                outputStream = new FileOutputStream(image);
            }

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Output stream is null", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving image", e);
            Toast.makeText(context, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException ignored) {}
        }
    }

    public static File downloadImageToTempFile(Context context, String imageUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(imageUrl).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Failed to download image: " + response);
            }

            File tempFile = File.createTempFile("temp_user_face_", ".jpg", context.getCacheDir());
            try (BufferedSink sink = Okio.buffer(Okio.sink(tempFile))) {
                sink.writeAll(response.body().source());
            }

            return tempFile;
        }
    }

    public static File getFileFromUri(Context context, Uri uri) throws IOException {
        return from(context, uri);
    }

    public static byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }
}