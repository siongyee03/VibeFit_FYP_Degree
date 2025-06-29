package com.example.vibefitapp;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private final int space;

    public SpacesItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, @NonNull View view, RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int spanCount = 2;

        int column = position % spanCount;

        outRect.left = space - column * space / spanCount;
        outRect.right = (column + 1) * space / spanCount;

        if (position >= spanCount) {
            outRect.top = space;
        }

        outRect.bottom = space;
    }
}

