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

public class ImagePreviewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_IMAGE = 0;
    private static final int TYPE_ADD = 1;
    private final Context context;
    private final List<Uri> imageUris;
    private final OnImageDeleteListener deleteListener;
    private final OnImageClickListener clickListener;
    private final OnAddImageClickListener addImageClickListener;
    private final int MAX_IMAGES = 9;

    public interface OnImageDeleteListener {
        void onImageDeleted(int position);
    }

    public interface OnImageClickListener {
        void onImageClick(int position);
    }
    public interface OnAddImageClickListener {
        void onAddImageClick();
    }

    public ImagePreviewAdapter(Context context, List<Uri> imageUris,
                               OnImageDeleteListener deleteListener,
                               OnImageClickListener clickListener,
                               OnAddImageClickListener addImageClickListener) {
        this.context = context;
        this.imageUris = imageUris;
        this.deleteListener = deleteListener;
        this.clickListener = clickListener;
        this.addImageClickListener = addImageClickListener;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
            return new ImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_add_image_button, parent, false);
            return new AddViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ImageViewHolder) {
            Uri imageUri = imageUris.get(position);
            ImageViewHolder imageHolder = (ImageViewHolder) holder;
            imageHolder.imageView.setImageURI(imageUri);

            if (imageUris.size() > 1) {
                imageHolder.deleteButton.setVisibility(View.VISIBLE);
            } else {
                imageHolder.deleteButton.setVisibility(View.GONE);
            }

            imageHolder.deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    int adapterPosition = holder.getBindingAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        deleteListener.onImageDeleted(adapterPosition);
                    }
                }
            });

            imageHolder.imageView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int adapterPosition = holder.getBindingAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        clickListener.onImageClick(adapterPosition);
                    }
                }
            });
        } else if (holder instanceof AddViewHolder) {
            ((AddViewHolder) holder).addImage.setOnClickListener(v -> {
                if (addImageClickListener != null) {
                    addImageClickListener.onAddImageClick();
                }
            });
        }
    }


    @Override
    public int getItemCount() {
            return imageUris.size() < MAX_IMAGES ? imageUris.size() + 1 : imageUris.size();
    }

    @Override
    public int getItemViewType(int position) {
            if (position == imageUris.size() && imageUris.size() < MAX_IMAGES) {
                return TYPE_ADD;
            } else {
                return TYPE_IMAGE;
            }
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

    public static class AddViewHolder extends RecyclerView.ViewHolder {
        ImageView addImage;

        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
            addImage = itemView.findViewById(R.id.add_image);
        }
    }
}

