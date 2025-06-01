package com.example.vibefitapp;

public class TextOverlayData {
    public String text;
    public int color;
    public float x, y;
    public String fontPath;

    public TextOverlayData(String text, int color, float x, float y, String fontPath) {
        this.text = text;
        this.color = color;
        this.x = x;
        this.y = y;
        this.fontPath = fontPath;
    }
}

