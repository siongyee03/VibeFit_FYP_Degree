package com.example.vibefitapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatTextView;

public class DraggableTextView extends AppCompatTextView {

    private Paint borderPaint;
    private RectF borderRect;
    private boolean selected = false;

    private float lastX, lastY;
    private float dX, dY;

    public DraggableTextView(Context context) {
        super(context);
        init();
    }

    public DraggableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6);
        borderPaint.setAntiAlias(true);
        borderRect = new RectF();
        setTextColor(Color.WHITE);
        setShadowLayer(6, 0, 0, Color.BLACK);
        setPadding(20, 10, 20, 10);
        setTypeface(Typeface.DEFAULT_BOLD);
        setTextSize(28);
    }

    public void setSelectedBorder(boolean sel) {
        selected = sel;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (selected) {
            borderRect.set(0, 0, getWidth(), getHeight());
            canvas.drawRoundRect(borderRect, 12, 12, borderPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getRawX();
                lastY = event.getRawY();
                dX = getX() - lastX;
                dY = getY() - lastY;
                return true;
            case MotionEvent.ACTION_MOVE:
                float newX = event.getRawX() + dX;
                float newY = event.getRawY() + dY;
                setX(newX);
                setY(newY);
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
