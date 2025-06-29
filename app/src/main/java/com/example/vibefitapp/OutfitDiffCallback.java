package com.example.vibefitapp;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class OutfitDiffCallback extends DiffUtil.ItemCallback<Outfit> {
    @Override
    public boolean areItemsTheSame(@NonNull Outfit oldItem, @NonNull Outfit newItem) {
        return oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Outfit oldItem, @NonNull Outfit newItem) {
        return oldItem.equals(newItem);
    }
}

