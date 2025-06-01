package com.example.vibefitapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class RangeSeekBarView extends View {

    private Paint paint;
    private float leftHandleX;
    private float rightHandleX;
    private final float handleWidth = 20;
    private boolean isDraggingLeft = false;
    private boolean isDraggingRight = false;
    private OnRangeChangeListener listener;

    public void setInitialPositions(float left, float right) {
        leftHandleX = left;
        rightHandleX = right;
        invalidate();
    }

    public RangeSeekBarView(Context context) {
        super(context);
        init();
    }

    public RangeSeekBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RangeSeekBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(4);
        leftHandleX = 0;
        rightHandleX = getWidth();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // Draw semi-transparent overlay outside the selected range
        paint.setColor(Color.parseColor("#AA000000"));
        canvas.drawRect(0, 0, leftHandleX, getHeight(), paint);
        canvas.drawRect(rightHandleX, 0, getWidth(), getHeight(), paint);

        // Draw handles
        paint.setColor(Color.WHITE);
        canvas.drawRect(leftHandleX - handleWidth / 2, 0, leftHandleX + handleWidth / 2, getHeight(), paint);
        canvas.drawRect(rightHandleX - handleWidth / 2, 0, rightHandleX + handleWidth / 2, getHeight(), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (Math.abs(x - leftHandleX) < handleWidth) {
                    isDraggingLeft = true;
                } else if (Math.abs(x - rightHandleX) < handleWidth) {
                    isDraggingRight = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDraggingLeft) {
                    leftHandleX = Math.min(x, rightHandleX - handleWidth);
                    if (listener != null) {
                        listener.onRangeChanged(getStartTime(), getEndTime());
                    }
                    invalidate();
                } else if (isDraggingRight) {
                    rightHandleX = Math.max(x, leftHandleX + handleWidth);
                    if (listener != null) {
                        listener.onRangeChanged(getStartTime(), getEndTime());
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                isDraggingLeft = false;
                isDraggingRight = false;
                performClick();
                break;
        }
        return true;
    }

    public void setOnRangeChangeListener(OnRangeChangeListener listener) {
        this.listener = listener;
    }

    public interface OnRangeChangeListener {
        void onRangeChanged(float startTime, float endTime);
    }

    public float getStartTime() {
        return leftHandleX / getWidth();
    }

    public float getEndTime() {
        return rightHandleX / getWidth();
    }
}

