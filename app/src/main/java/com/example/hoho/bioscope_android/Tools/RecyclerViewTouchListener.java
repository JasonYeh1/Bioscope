package com.example.hoho.bioscope_android.Tools;

import android.view.MotionEvent;
import android.view.View;

public interface RecyclerViewTouchListener {

    void onTouch(View v, MotionEvent event, String text);
}
