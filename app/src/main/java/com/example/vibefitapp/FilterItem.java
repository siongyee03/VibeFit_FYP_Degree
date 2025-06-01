package com.example.vibefitapp;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class FilterItem {
    public String name;
    public GPUImageFilter filter;

    public FilterItem(String name, GPUImageFilter filter) {
        this.name = name;
        this.filter = filter;
    }
}
