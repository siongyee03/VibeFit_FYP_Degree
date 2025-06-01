package com.example.vibefitapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TrimActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URI = "video_uri";
    public static final String RESULT_MEDIA_PATH = "edited_media_path";

    private Uri videoUri;

    private TextView tvStartTime, tvEndTime, tvDuration;
    private Button btnTrim;

    private RangeSeekBarView rangeSeekBarView;
    private RecyclerView thumbnailRecyclerView;
    private ThumbnailAdapter thumbnailAdapter;
    private final List<Bitmap> thumbnailList = new ArrayList<>();

    private long videoDurationMs = 0;
    private long trimStartMs = 0;
    private long trimEndMs = 0;
    private VideoView videoView;
    private ImageButton btnPlayPause;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private GestureDetector gestureDetector;
    private FrameLayout videoFrameLayout;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trim);

        videoView = findViewById(R.id.videoView);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnTrim = findViewById(R.id.btnTrim);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);
        tvDuration = findViewById(R.id.tvDuration);
        rangeSeekBarView = findViewById(R.id.rangeSeekBarView);
        thumbnailRecyclerView = findViewById(R.id.thumbnailRecyclerView);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        videoFrameLayout = videoView.getParent() != null
                ? (FrameLayout) videoView.getParent()
                : null;

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                if (btnPlayPause.getVisibility() == View.VISIBLE) {
                    btnPlayPause.setVisibility(View.GONE);
                } else {
                    btnPlayPause.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });

        if (videoFrameLayout != null) {
            videoFrameLayout.setClickable(true);
            videoFrameLayout.setOnClickListener(v -> {
            });
            videoFrameLayout.setOnTouchListener((v, event) -> {
                boolean handled = gestureDetector.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return handled;
            });
        }


        String videoUriStr = getIntent().getStringExtra(EXTRA_VIDEO_URI);
        if (videoUriStr == null) {
            Toast.makeText(this, "Invalid video", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        videoUri = Uri.parse(videoUriStr);

        videoView.setVideoURI(videoUri);
        videoView.setOnPreparedListener(mp -> {
            videoDurationMs = mp.getDuration();
            trimStartMs = 0;
            trimEndMs = videoDurationMs;

            updateTimeTexts();

            mainHandler.post(() -> {
                rangeSeekBarView.setInitialPositions(0, rangeSeekBarView.getWidth());
                rangeSeekBarView.invalidate();
            });

            generateThumbnails(videoUri, videoDurationMs);
        });

        rangeSeekBarView.setOnRangeChangeListener((startFraction, endFraction) -> {
            trimStartMs = (long) (startFraction * videoDurationMs);
            trimEndMs = (long) (endFraction * videoDurationMs);

            if (trimEndMs - trimStartMs < 1000) {
                trimEndMs = trimStartMs + 1000;
            }

            updateTimeTexts();

            videoView.seekTo((int) trimStartMs);
        });

        btnPlayPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                btnPlayPause.setVisibility(View.VISIBLE);
            } else {
                videoView.seekTo((int) trimStartMs);
                videoView.start();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                btnPlayPause.setVisibility(View.GONE);
                mainHandler.post(updateRunnable);
            }
        });


        btnTrim.setOnClickListener(v -> {
            btnTrim.setEnabled(false);
            Toast.makeText(this, "Starting to trim, please wait a moment...", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                try {
                    String outputPath = getTrimmedVideoPath();
                    trimVideo(videoUri, trimStartMs, trimEndMs, outputPath);

                    mainHandler.post(() -> {
                        Toast.makeText(this, "Trimmed successfully!", Toast.LENGTH_SHORT).show();
                        Intent result = new Intent();
                        result.putExtra(RESULT_MEDIA_PATH, outputPath);
                        setResult(RESULT_OK, result);
                        finish();
                    });
                } catch (Exception e) {
                Log.e("TrimActivity", "Video trimming failed", e);
                mainHandler.post(() -> {
                    Toast.makeText(this, "Trim failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnTrim.setEnabled(true);
                });
            }
        }).start();
        });
    }

    private void updateTimeTexts() {
        tvStartTime.setText(formatTime(trimStartMs));
        tvEndTime.setText(formatTime(trimEndMs));
        tvDuration.setText(String.format(Locale.getDefault(), "Duration: %s", formatTime(trimEndMs - trimStartMs)));
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    private void generateThumbnails(Uri videoUri, long durationMs) {
        thumbnailList.clear();
        int intervalMs = 1000;

        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(this, videoUri);

            for (int timeMs = 0; timeMs < durationMs; timeMs += intervalMs) {
                Bitmap bitmap = retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (bitmap != null) {
                    thumbnailList.add(bitmap);
                }
            }
        } catch (Exception e) {
            Log.e("TrimActivity", "Error generating thumbnails", e);
        }

        thumbnailAdapter = new ThumbnailAdapter(thumbnailList);
        thumbnailRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        thumbnailRecyclerView.setAdapter(thumbnailAdapter);
    }

    private String getTrimmedVideoPath() {
        File dir = getExternalFilesDir(null);
        if (dir == null) {
            dir = getFilesDir();
        }
        return new File(dir, "trimmed_" + UUID.randomUUID() + ".mp4").getAbsolutePath();
    }

    private void trimVideo(Uri srcUri, long startMs, long endMs, String outputPath) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(this, srcUri, null);

        MediaMuxer muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        int videoTrackIndex = -1;
        int audioTrackIndex = -1;

        // 找视频轨和音频轨
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime == null) continue;

            if (mime.startsWith("video/") && videoTrackIndex == -1) {
                videoTrackIndex = muxer.addTrack(format);
            } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                audioTrackIndex = muxer.addTrack(format);
            }
        }

        if (videoTrackIndex == -1) {
            extractor.release();
            muxer.release();
            throw new IOException("Video track not found");
        }

        muxer.start();

        // 裁剪范围转换为微秒
        long startUs = startMs * 1000;
        long endUs = endMs * 1000;

        // 先写视频轨
        writeSampleData(extractor, muxer, videoTrackIndex, startUs, endUs);

        // 再写音频轨
        if (audioTrackIndex != -1) {
            writeSampleData(extractor, muxer, audioTrackIndex, startUs, endUs);
        }

        muxer.stop();
        muxer.release();
        extractor.release();
    }

    private void writeSampleData(MediaExtractor extractor, MediaMuxer muxer, int trackIndex, long startUs, long endUs) {
        extractor.selectTrack(trackIndex);
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        final int MAX_SAMPLE_SIZE = 256 * 1024;
        android.media.MediaCodec.BufferInfo bufferInfo = new android.media.MediaCodec.BufferInfo();
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(MAX_SAMPLE_SIZE);

        while (true) {
            int sampleSize = extractor.readSampleData(buffer, 0);
            if (sampleSize < 0) break;

            long sampleTime = extractor.getSampleTime();
            if (sampleTime > endUs) break;
            if (sampleTime < startUs) {
                extractor.advance();
                continue;
            }

            bufferInfo.offset = 0;
            bufferInfo.size = sampleSize;
            int sampleFlags = extractor.getSampleFlags();
            if ((sampleFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
            } else {
                bufferInfo.flags = 0;
            }
            bufferInfo.presentationTimeUs = sampleTime;

            muxer.writeSampleData(trackIndex, buffer, bufferInfo);

            extractor.advance();
        }

        extractor.unselectTrack(trackIndex);
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (videoView.isPlaying()) {
                int currentPos = videoView.getCurrentPosition();
                if (currentPos >= trimEndMs) {
                    videoView.pause();
                    btnPlayPause.setVisibility(View.VISIBLE);
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    mainHandler.postDelayed(this, 200);
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
        mainHandler.removeCallbacksAndMessages(null);
    }

}
