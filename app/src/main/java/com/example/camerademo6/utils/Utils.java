package com.example.camerademo6.utils;

import android.content.Context;

/*
* 拿屏宽*/
public class Utils {
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }
}
