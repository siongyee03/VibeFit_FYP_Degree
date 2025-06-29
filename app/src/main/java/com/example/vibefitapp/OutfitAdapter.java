package com.example.vibefitapp;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import android.view.LayoutInflater;
import android.view.ViewGroup;

public class OutfitAdapter extends ListAdapter<Outfit, RecyclerView.ViewHolder> {

    public interface OnOutfitClick {
        void onTryClick(Outfit outfit);
        void onUploadClick();
        void onDeleteClick(Outfit outfit);
    }

    private static final int VIEW_TYPE_UPLOAD = 0;
    private static final int VIEW_TYPE_OUTFIT = 1;

    private final OnOutfitClick listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public OutfitAdapter(OnOutfitClick listener) {
        super(new OutfitDiffCallback());
        this.listener = listener;
    }

    public static class OutfitViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        Button btnTry;
        ImageButton btnDelete;

        public OutfitViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.img_outfit);
            btnTry = itemView.findViewById(R.id.btn_try_on);
            btnDelete = itemView.findViewById(R.id.btn_delete_outfit);
        }

        public void bind(Outfit outfit, OnOutfitClick listener, boolean isSelected) {
            Glide.with(itemView.getContext()).load(outfit.getImageUrl()).into(image);
            btnTry.setOnClickListener(v -> listener.onTryClick(outfit));
            itemView.setBackgroundResource(isSelected ? R.drawable.bg_outfit_selected : android.R.color.transparent);

            if (outfit.isUserUploaded()) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> listener.onDeleteClick(outfit));
            } else {
                btnDelete.setVisibility(View.GONE);
                btnDelete.setOnClickListener(null);
            }
        }
    }

    public static class UploadViewHolder extends RecyclerView.ViewHolder {
        public UploadViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(OnOutfitClick listener) {
            itemView.setOnClickListener(v -> listener.onUploadClick());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_UPLOAD : VIEW_TYPE_OUTFIT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_UPLOAD) {
            View v = inflater.inflate(R.layout.item_upload_card, parent, false);
            return new UploadViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_outfit_card, parent, false);
            return new OutfitViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UploadViewHolder) {
            ((UploadViewHolder) holder).bind(listener);

        } else {
            Outfit outfit = getItem(position - 1);
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            boolean isSelected = adapterPosition == selectedPosition;

            OutfitViewHolder outfitHolder = (OutfitViewHolder) holder;
            outfitHolder.bind(outfit, listener, isSelected);

            holder.itemView.setOnClickListener(v -> setSelectedPosition(adapterPosition));
            setItemWidth(holder.itemView);
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1;
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(position);
    }

    private void setItemWidth(View itemView) {
        RecyclerView recyclerView = (RecyclerView) itemView.getParent();
        if (recyclerView == null) return;

        int recyclerWidth = recyclerView.getWidth();
        if (recyclerWidth == 0) {
            itemView.post(() -> setItemWidth(itemView));
            return;
        }

        int desiredWidth = (int) (recyclerWidth / 2.5);
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        params.width = desiredWidth;
        itemView.setLayoutParams(params);
    }
}
