package com.example.hoho.bioscope_android.Models;

import android.graphics.Bitmap;

import java.io.File;

public class CheckStatFiles {

    private File file;
    private boolean check;
    private Bitmap image;

    public CheckStatFiles(File file, boolean check, Bitmap image){
        this.file = file;
        this.check = check;
        this.image = image;
    }

    public File getFile(){
        return file;
    }

    public boolean getCheck() {
        return check;
    }

    public Bitmap getImage(){
        return image;
    }

    public void setFile(File file){
        this.file = file;
    }

    public void setCheck(boolean b){
        check = b;
    }

    public void setImage(Bitmap image){
        this.image = image;
    }
}
