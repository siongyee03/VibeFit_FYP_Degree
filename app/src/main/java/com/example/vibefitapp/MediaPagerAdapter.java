package com.example.vibefitapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MediaPagerAdapter extends RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder> {
    private List<String> mediaUrls;
    private Context context;

    public MediaPagerAdapter(List<String> mediaUrls, Context context) {
        this.mediaUrls = mediaUrls;
        this.context = context;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_slide, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        String url = mediaUrls.get(position);
        Glide.with(context).load(url).into(holder.mediaImage);
        holder.mediaImage.setOnClickListener(v -> {
            Intent intent = new Intent(context, ZoomableImageActivity.class);
            intent.putExtra("imageUrl", url);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mediaUrls.size();
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaImage;
        MediaViewHolder(View itemView) {
            super(itemView);
            mediaImage = itemView.findViewById(R.id.slide_media_image);
        }
    }
}

