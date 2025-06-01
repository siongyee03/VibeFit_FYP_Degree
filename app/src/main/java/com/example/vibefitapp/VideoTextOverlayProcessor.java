package com.example.vibefitapp;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.daasuu.mp4compose.composer.Mp4Composer;
import com.daasuu.mp4compose.filter.GlOverlayFilter;

import java.util.List;

public class VideoTextOverlayProcessor {

    private final Context context;

    public VideoTextOverlayProcessor(Context context) {
        this.context = context;
    }

    public void overlayTextOnVideo(
            Uri videoUri,
            String outputPath,
            List<TextOverlay> textOverlays,
            Callback callback
    ) {
        new Mp4Composer(videoUri.toString(), outputPath)
                .filter(new GlOverlayFilter() {
                    @Override
                    public void drawCanvas(@NonNull Canvas canvas) {
                        for (TextOverlay overlay : textOverlays) {
                            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            paint.setColor(overlay.color);
                            paint.setTextSize(overlay.textSizePx);
                            paint.setTypeface(overlay.typeface);
                            paint.setShadowLayer(5f, 2f, 2f, Color.BLACK);
                            paint.setTextAlign(Paint.Align.LEFT);

                            float x = overlay.xRatio * canvas.getWidth();
                            float y = overlay.yRatio * canvas.getHeight();

                            canvas.drawText(overlay.text, x, y, paint);
                        }
                    }
                })
                .listener(new Mp4Composer.Listener() {
                    @Override public void onProgress(double progress) {}
                    @Override public void onCompleted() {
                        callback.onSuccess(outputPath);
                    }
                    @Override public void onCanceled() {
                        callback.onFailure(new Exception("Canceled"));
                    }
                    @Override public void onFailed(Exception e) {
                        callback.onFailure(e);
                    }
                    @Override public void onCurrentWrittenVideoTime(long timeUs) {}
                })
                .start();
    }

    public static class TextOverlay {
        public String text;
        public int color;
        public Typeface typeface;
        public float textSizePx;
        public float xRatio;
        public float yRatio;

        public TextOverlay(String text, int color, Typeface typeface, float textSizePx, float xRatio, float yRatio) {
            this.text = text;
            this.color = color;
            this.typeface = typeface;
            this.textSizePx = textSizePx;
            this.xRatio = xRatio;
            this.yRatio = yRatio;
        }
    }

    public interface Callback {
        void onSuccess(String outputPath);
        void onFailure(Exception e);
    }
}
