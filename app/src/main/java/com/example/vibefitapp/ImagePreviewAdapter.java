package com.example.vibefitapp;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {

    private final Context context;
    private final List<Uri> imageUris;
    private final OnImageDeleteListener deleteListener;
    private final OnImageClickListener clickListener;

    public interface OnImageDeleteListener {
        void onImageDeleted(int position);
    }

    public interface OnImageClickListener {
        void onImageClick(int position);
    }

    public ImagePreviewAdapter(Context context, List<Uri> imageUris,
                               OnImageDeleteListener deleteListener,
                               OnImageClickListener clickListener) {
        this.context = context;
        this.imageUris = imageUris;
        this.deleteListener = deleteListener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        holder.imageView.setImageURI(imageUri);

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onImageDeleted(holder.getAdapterPosition());
            }
        });

        holder.imageView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onImageClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton deleteButton;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_preview);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}

