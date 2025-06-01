package com.example.vibefitapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {

    public interface OnFilterSelectedListener {
        void onFilterSelected(GPUImageFilter filter);
    }

    private final Context context;
    private final List<FilterItem> filterItems;
    private final Bitmap originalBitmap;
    private final OnFilterSelectedListener listener;

    public FilterAdapter(Context context, List<FilterItem> filterItems, Bitmap originalBitmap, OnFilterSelectedListener listener) {
        this.context = context;
        this.filterItems = filterItems;
        this.originalBitmap = originalBitmap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FilterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_filter_thumbnail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterAdapter.ViewHolder holder, int position) {
        FilterItem item = filterItems.get(position);
        holder.filterName.setText(item.name);
        GPUImageHelper.applyFilterAsync(context, originalBitmap, item.filter, result -> holder.thumbnail.setImageBitmap(result));

        holder.thumbnail.setOnClickListener(v -> listener.onFilterSelected(item.filter));
    }

    @Override
    public int getItemCount() {
        return filterItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView filterName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.image_thumbnail);
            filterName = itemView.findViewById(R.id.text_filter_name);
        }
    }
}
