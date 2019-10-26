package com.example.hoho.bioscope_android.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.example.hoho.bioscope_android.R;

public class MainActivity extends Activity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void Begin (View v){
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }
}

