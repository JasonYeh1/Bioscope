package com.example.hoho.bioscope_android.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hoho.bioscope_android.Adapters.TagsAdapter;
import com.example.hoho.bioscope_android.Data.Model.ItemDataHolder;
import com.example.hoho.bioscope_android.Data.Model.Tags;
import com.example.hoho.bioscope_android.Data.Repo.ItemDataHolderRepo;
import com.example.hoho.bioscope_android.Data.Repo.TagsRepo;
import com.example.hoho.bioscope_android.R;
import com.example.hoho.bioscope_android.Tools.RecyclerViewClickListener;
import com.example.hoho.bioscope_android.Tools.RecyclerViewLongClickListener;
import com.example.hoho.bioscope_android.Tools.StorageTools;


public class ContentInfoActivity extends Activity {

    private String fileName;
    private String name;
    private String type;
    private EditText contentTitle;
    private EditText contentDescription;

    private ItemDataHolderRepo itemDataHolderRepo;
    private ItemDataHolder itemDataHolder;

    private TagsRepo tagsRepo;

    private RecyclerView recyclerViewTags;
    private RecyclerView.LayoutManager mLayoutManager;
    private TagsAdapter mAdapter;
    private RecyclerViewLongClickListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_info);
        fileName = getIntent().getStringExtra("file");

        //split up file name into name and type in order to smoothly change name of any passed file type
        final String[] s = splitFileNameAndType(fileName);
        name = s[0];
        type = "." + s[1];

        //point at xml layout
        contentTitle = (EditText) findViewById(R.id.editText_ContentTitle);
        contentDescription = (EditText) findViewById(R.id.editText_ContentDescription);

        //initialize instances of these repos in order to access data within these tables from the database
        itemDataHolderRepo = new ItemDataHolderRepo();
        tagsRepo = new TagsRepo();

        //handles long clicks (held down for a second) on tags to prompt user if they want to remove the tag
        listener = new RecyclerViewLongClickListener() {
            @Override
            public void onLongClick(View view, final String tag) {
                removeTag(tag);
            }
        };

        //point at xml layout and make sure size stays same
        recyclerViewTags = findViewById(R.id.recycleView_Tags);
        recyclerViewTags.setHasFixedSize(true);

        //makes the amount of columns in the recycler view 5
        mLayoutManager = new GridLayoutManager(getApplicationContext(),5);
        recyclerViewTags.setLayoutManager(mLayoutManager);
        recyclerViewTags.setNestedScrollingEnabled(false);

    }

    @Override
    protected void onResume(){
        super.onResume();
        //handles getting the info for the current content and puts the default if there is none
        itemDataHolder = new ItemDataHolder();
        if (!itemDataHolderRepo.databaseIsEmpty()) {
            itemDataHolder = itemDataHolderRepo.getItemDataHolder(fileName);
        }
            if (itemDataHolder.getFilename() == "None"){
                contentTitle.setText(name);
                itemDataHolder.setFilename(fileName);
            }else{
                String[] s = splitFileNameAndType(itemDataHolder.getFilename());
                name = s[0];
                type = "." + s[1];
                contentTitle.setText(name);
                contentDescription.setText(itemDataHolder.getDescription());
            }
        mAdapter = new TagsAdapter(getApplicationContext(), tagsRepo.getTagsByFile(fileName), listener);
        recyclerViewTags.setAdapter(mAdapter);
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        //your method call
        super.onBackPressed();
        Intent intent = new Intent(this, FullScreenImage.class);
        intent.putExtra("file", fileName);
        startActivity(intent);
    }

    public void Back(View v){
        Intent intent = new Intent(this, FullScreenImage.class);
        intent.putExtra("file", fileName);
        startActivity(intent);
    }

    //enables editing of the title and description
    public void Edit(View v){
        if (!contentTitle.isEnabled()){
            contentTitle.setEnabled(true);
            contentDescription.setEnabled(true);
            Toast.makeText(this, "You can now change attributes",Toast.LENGTH_SHORT).show();
        }

    }

    //adds a tag to the current content
    public void AddTag(View v){
        //makes sure the tag can save with the correct filename
        if (contentTitle.isEnabled()) {
            Toast.makeText(this, "Please save first before adding a new Tag", Toast.LENGTH_SHORT).show();
        } else {
            //creates a dialog that asks user to insert a new tag
            //TODO add restrictions on what kind of tags can be entered
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter a new Tag");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = input.getText().toString();
                    Tags tag = new Tags();
                    tag.setFilename(fileName);
                    tag.setTags(text);
                    tagsRepo.insert(tag);
                    mAdapter = new TagsAdapter(getApplicationContext(), tagsRepo.getTagsByFile(fileName), listener);
                    recyclerViewTags.setAdapter(mAdapter);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();

        }
    }

    //saves new name and/or description
    //TODO add restrictions on what kind of words can be used for titles and descriptions i.e. obscenity, reserved char (.), reserved words
    public void Save(View v){
        if (contentTitle.isEnabled()) {
            contentTitle.setEnabled(false);
            contentDescription.setEnabled(false);
            ItemDataHolder idh = new ItemDataHolder();
            name = contentTitle.getText().toString();
            if (fileName != name+type){
                //handles file name updates
                idh.setFilename(name+type);
                idh.setDescription(contentDescription.getText().toString());
                //update all tags with new filename
                Tags tag = new Tags();
                tag.setFilename(name+type);
                tagsRepo.updateFileName(tag, fileName);
                //remove the old title and description and add the new one
                itemDataHolderRepo.delete(fileName);
                itemDataHolderRepo.insert(idh);

                StorageTools.renameFile(fileName, name+type);
                fileName = name+type;

            }else{
                //handles description updates
                idh.setFilename(fileName);
                idh.setDescription(contentDescription.getText().toString());
                itemDataHolderRepo.update(idh);
            }
        } else {
            Toast.makeText(this, "Click Edit to change title and description", Toast.LENGTH_SHORT).show();
        }
    }

    //separates filename by period
    public String[] splitFileNameAndType(String s){
        String [] n = s.split("\\.");
        return n;
    }

    //makes a dialog to ask user if they want to delete the tag
    public void removeTag(final String tag){
        AlertDialog.Builder builder = new AlertDialog.Builder(ContentInfoActivity.this);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure you want to delete this tag?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                tagsRepo.delete(fileName, tag);
                mAdapter = new TagsAdapter(getApplicationContext(), tagsRepo.getTagsByFile(fileName), listener);
                recyclerViewTags.setAdapter(mAdapter);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
