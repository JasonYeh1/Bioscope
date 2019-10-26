package com.example.hoho.bioscope_android.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import com.example.hoho.bioscope_android.Activities.FullScreenImage;
import com.example.hoho.bioscope_android.Models.CheckStatFiles;
import com.example.hoho.bioscope_android.R;

//This class is required to use the RecyclerAdapter, retrieves images from internal storage and displays in the RecyclerAdapter
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private Context context;
    private int count;
    public ArrayList<CheckStatFiles> statFiles;

    public GalleryAdapter(Context context, ArrayList<CheckStatFiles> files) {
        this.context = context;
        count = files.size();
        statFiles = files;
    }

    @Override
    //Reads from internal storage and displays the image
    public void onBindViewHolder(GalleryAdapter.ViewHolder holder, final int position) {
        holder.img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.img.setImageBitmap(statFiles.get(position).getImage());
        holder.text.setText(statFiles.get(position).getFile().getName());
        holder.img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, FullScreenImage.class)
                .putExtra("file", statFiles.get(position).getFile().getName());
                context.startActivity(intent);
            }
        });
        holder.checkBox.setChecked(statFiles.get(position).getCheck());
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox b = (CheckBox) v;
                statFiles.get(position).setCheck(b.isChecked());
            }
        });

    }

    @NonNull
    @Override
    //Creates a ViewHolder that holds the image
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item, parent, false));
    }

    @Override
    public int getItemCount() {
        return count;
    }

    //Inner class that stores the actual image in a ViewHolder
    public class ViewHolder extends RecyclerView.ViewHolder{
        private ImageView img;
        private TextView text;
        private CheckBox checkBox;

        public ViewHolder(View view) {
            super(view);
            img = view.findViewById(R.id.img);
            text = view.findViewById(R.id.textView_name);
            checkBox = view.findViewById(R.id.checkBox_item);
        }
    }
}

