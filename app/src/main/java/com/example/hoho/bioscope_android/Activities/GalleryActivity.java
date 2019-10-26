package com.example.hoho.bioscope_android.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.hoho.bioscope_android.Adapters.GalleryAdapter;
import com.example.hoho.bioscope_android.Data.Repo.ItemDataHolderRepo;
import com.example.hoho.bioscope_android.Data.Repo.TagsRepo;
import com.example.hoho.bioscope_android.Models.CheckStatFiles;
import com.example.hoho.bioscope_android.R;
import com.example.hoho.bioscope_android.Tools.FocusStacker;
import com.example.hoho.bioscope_android.Tools.RecyclerViewLongClickListener;
import com.example.hoho.bioscope_android.Tools.RecyclerViewTouchListener;
import com.example.hoho.bioscope_android.Tools.StorageTools;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.util.ArrayList;

public class GalleryActivity extends Activity {

    //TODO change activity to make memory usage more efficient (loading recyclerView uses too much memory)
    static{
        System.loadLibrary("native-lib");
    }
    private String m_Text = "";
    private RecyclerView mRecyclerView;
    private GalleryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private BaseLoaderCallback mLoaderCallback;

    private Spinner tagSpinner;
    private ArrayAdapter<String> tagAdapter;
    private TagsRepo tagsRepo;
    private ArrayList<String> mTags;
    private ProgressBar progressBar;

    private ItemDataHolderRepo itemDataHolderRepo;

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;

    private static final int DELAY_TIME = 5000;

    //default tag to show initially
    private final String defaultTag = "Tags";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        //creates file in internal app storage to store all pics and vids in
        StorageTools.createDirectoryOnInternalStorage(getApplicationContext(), "BioscopeFiles");

        //create instance of TagsRepo and itemDataHolderRepo to access tags table in sql database
        tagsRepo = new TagsRepo();
        itemDataHolderRepo = new ItemDataHolderRepo();
        //point spinner at spinner from xml layout
        tagSpinner = (Spinner) findViewById(R.id.spinner_Tags);
        //progressBar = findViewById(R.id.loadingCircle);

        //create new string arraylist to handle all tags
        mTags = new ArrayList<>();
        mTags.add(defaultTag);
        mTags.addAll(tagsRepo.getAllTags());

        //set up spinner to contain all values in mTags
        tagAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mTags);
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagSpinner.setAdapter(tagAdapter);

        //make sure spinner starts at the default Tag
        tagSpinner.setSelection(tagAdapter.getPosition(defaultTag));

        //sets all items in spinner on listener
        tagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //get the current selected item
                String current = mTags.get(position);
                if (!current.equals(defaultTag)) {
                    //get all files names with the selected tags and covert them to files
                    ArrayList<String> stringfiles = tagsRepo.getFileByTags(current);
                    File[] files = new File[stringfiles.size()];
                    for (int i = 0; i < stringfiles.size(); i++) {
                        files[i] = new File(stringfiles.get(i));
                    }
                    //load files with tag
                    mAdapter = new GalleryAdapter(getApplicationContext(), changeFileData(files));
                    mRecyclerView.setAdapter(mAdapter);
                } else{
                    //if the default tag is selected load every file
                    mAdapter = new GalleryAdapter(getApplicationContext(), changeFileData(StorageTools.getImageNames()));
                    mRecyclerView.setAdapter(mAdapter);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //point recycler view at xml layout
        mRecyclerView = findViewById(R.id.recycleView);
        mRecyclerView.setHasFixedSize(true);

        //set up recycler view to be 1 columns wide and have a scroll view
        mLayoutManager = new GridLayoutManager(getApplicationContext(),1);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setNestedScrollingEnabled(false);

        progressBar = findViewById(R.id.progressBar_stacker);

        //makes sure camera permissions are enabled if they are not ask for permission
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                Toast.makeText(this, "App required access to camera", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
        }

        //made sure opencv is loaded so we can use it
        mLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                    {
                        Log.i("OpenCV", "OpenCV loaded successfully");
                    } break;
                    default:
                    {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        };
    }

    //changes file data to CheckStatFile Object that holds files, check state, and image as bitmap
    public ArrayList<CheckStatFiles> changeFileData(File[] files){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        ArrayList<CheckStatFiles> fileNames = new ArrayList<>();
        for (File file: files){
            fileNames.add(new CheckStatFiles(file, false,
                    BitmapFactory.decodeFile(file.getAbsolutePath(), options)));
        }
        return fileNames;
    }

    //removes checked files from storage
    public void Delete(View view){
        //get all the images that are checked
        final ArrayList<File> checkedFiles = new ArrayList<>();
        for(CheckStatFiles s: mAdapter.statFiles) {
            if(s.getCheck()){
                checkedFiles.add(s.getFile());
            }
        }
        //pop-up dialog to prompt user to ask if they want to delete selected images
        AlertDialog.Builder builder = new AlertDialog.Builder(GalleryActivity.this);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure you want to delete these files?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                for (File file: checkedFiles) {
                    itemDataHolderRepo.delete(file.getName());
                    tagsRepo.deleteByFile(file.getName());
                    StorageTools.deleteFile(file.getName());
                }
                mAdapter = new GalleryAdapter(getApplicationContext(), changeFileData(StorageTools.getImageNames()));
                mRecyclerView.setAdapter(mAdapter);
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

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI();

    //displays short message about app
    public void Info(View view){
        AlertDialog.Builder messageAbout = new AlertDialog.Builder(this);
        messageAbout.setMessage(R.string.infoMessage).show();

    }

    public void Stack(View view){
        //Toast.makeText(this, "Stacking selected Images", Toast.LENGTH_SHORT).show();
        final ArrayList<File> checkedFiles = new ArrayList<>();
        for(CheckStatFiles s: mAdapter.statFiles) {
            if(s.getCheck()){
                checkedFiles.add(s.getFile());
            }
        }
        //reset progress bar to zero with start of async task
        progressBar.setProgress(0);
        new GalleryActivity.AsyncStack().execute(checkedFiles);
    }


    @Override
    protected void onResume(){
        super.onResume();
        startBackgroundThread();
        //pulls up updated gallery every time the main activity is opened
        mAdapter = new GalleryAdapter(getApplicationContext(), changeFileData(StorageTools.getImageNames()));
        mRecyclerView.setAdapter(mAdapter);
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        endBackgroundThread();
    }

    @Override
    public void onBackPressed() {
        //your method call
        super.onBackPressed();
    }

    //handles permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "Application will not run without camera services", Toast.LENGTH_SHORT ).show();
            }
        }
    }

    //starts the camera activity
    public void startCamera(View view){
        //check one last time before starting camera activity if permissions are available
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                Toast.makeText(this, "App required access to camera", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
        }else {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        }
    }

    public class BackRunner implements Runnable{

        @Override
        public void run() {

        }
    }


    private void startBackgroundThread(){
        mBackgroundHandlerThread = new HandlerThread("Handler");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void endBackgroundThread(){
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //process all selected images
    public void stackImages(ArrayList<File> Files){
        ArrayList<Mat> input = new ArrayList<Mat>();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        for(File f: Files){
            Mat mat = new Mat();
            Utils.bitmapToMat(BitmapFactory.decodeFile(f.getAbsolutePath(), options),
                    mat);
            input.add(mat);
        }
        FocusStacker focusStacker = new FocusStacker(StorageTools.getDirectory().getAbsolutePath(), input);
        focusStacker.focus_stack();
    }

    private class AsyncStack extends AsyncTask<ArrayList<File>, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //make the progress bar visible when the task starts
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected Void doInBackground(ArrayList<File>... Files) {
            stackImages(Files[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //when stacking is finished make progress bar finished and reload the images
            Toast.makeText(getApplicationContext(), "Stacking Finished", Toast.LENGTH_LONG ).show();
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            //Refresh the gallery
            mAdapter = new GalleryAdapter(getApplicationContext(), changeFileData(StorageTools.getImageNames()));
            mRecyclerView.setAdapter(mAdapter);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);

        }
    }
}