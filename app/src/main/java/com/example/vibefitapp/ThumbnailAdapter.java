package com.example.vibefitapp;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder> {

    private List<Bitmap> thumbnailList;

    public ThumbnailAdapter(List<Bitmap> thumbnailList) {
        this.thumbnailList = thumbnailList;
    }

    @NonNull
    @Override
    public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        int width = (int) (parent.getContext().getResources().getDisplayMetrics().density * 60);
        int height = (int) (parent.getContext().getResources().getDisplayMetrics().density * 80);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new ThumbnailViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position) {
        holder.imageView.setImageBitmap(thumbnailList.get(position));
    }

    @Override
    public int getItemCount() {
        return thumbnailList.size();
    }

    static class ThumbnailViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ThumbnailViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}

