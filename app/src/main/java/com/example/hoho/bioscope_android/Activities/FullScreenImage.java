package com.example.hoho.bioscope_android.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import com.example.hoho.bioscope_android.R;
import com.example.hoho.bioscope_android.Tools.StorageTools;

//TODO change so its easy to flip between different screens
public class FullScreenImage extends Activity {

    private ImageView myImage;
    private String fileName;
    private TextView imageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view);
        fileName = getIntent().getStringExtra("file");

        imageName = findViewById(R.id.textView_ImageName);
        imageName.setText(fileName);

        //pulls up file using passed file name
        //TODO change to handle both images and videos
        myImage = findViewById(R.id.myImage);
        File imgFile = new File(StorageTools.getDirectory().getAbsolutePath() + "/" + fileName);
        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        myImage.setImageBitmap(bitmap);
    }

    @Override
    public void onBackPressed() {
        //your method call
        super.onBackPressed();
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    //returns to previous activity
    public void Back(View v){
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    //starts activity with all info about the current item
    public void Info(View v){
        Intent intent = new Intent(this, ContentInfoActivity.class);
        intent.putExtra("file", fileName);
        startActivity(intent);
    }

    //gives options of adding labels and measurements
    public void Options(View v){
        //TODO add code for labeling and measurements
    }
}
