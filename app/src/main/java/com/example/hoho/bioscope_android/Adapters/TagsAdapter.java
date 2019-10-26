package com.example.hoho.bioscope_android.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hoho.bioscope_android.R;
import com.example.hoho.bioscope_android.Tools.RecyclerViewClickListener;
import com.example.hoho.bioscope_android.Tools.RecyclerViewLongClickListener;

import java.util.ArrayList;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder> {

    private RecyclerViewLongClickListener mListener;
    private Context context;
    private int count;
    private ArrayList<String> tags;

    public TagsAdapter(Context context, ArrayList<String> tags, RecyclerViewLongClickListener listener) {
        mListener = listener;
        this.context = context;
        this.tags = tags;
        count = tags.size();
    }

    @Override
    public void onBindViewHolder(TagsAdapter.ViewHolder holder, final int position) {
        holder.text.setText(tags.get(position));
        holder.img.setImageResource(R.mipmap.tag_icon_round);
    }

    @NonNull
    @Override
    //Creates a ViewHolder that holds the image
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.tag, parent, false);
        return new ViewHolder(v, mListener);
    }

    @Override
    public int getItemCount() {
        return count;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        private RecyclerViewLongClickListener mListener;
        private ImageView img;
        private TextView text;
        public ViewHolder(View v, RecyclerViewLongClickListener listener) {
            super(v);
            mListener = listener;
            img = v.findViewById(R.id.imageView_tag);
            text = v.findViewById(R.id.textView_tag);
            v.setOnLongClickListener(this);
        }
        @Override
        public boolean onLongClick(View view) {
            mListener.onLongClick(view, text.getText().toString());
            return false;
        }
    }

}
