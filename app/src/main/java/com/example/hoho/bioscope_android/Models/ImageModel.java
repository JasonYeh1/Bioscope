package com.example.hoho.bioscope_android.Models;

import android.graphics.Bitmap;

public class ImageModel {
    private String name;
    private Bitmap bitmapImg;

    public ImageModel(){
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bitmap getImg() {
        return bitmapImg;
    }
}