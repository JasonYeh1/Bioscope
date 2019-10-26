package com.example.hoho.bioscope_android.Data;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.hoho.bioscope_android.Data.Model.ItemDataHolder;
import com.example.hoho.bioscope_android.Data.Model.Tags;
import com.example.hoho.bioscope_android.Data.Repo.ItemDataHolderRepo;
import com.example.hoho.bioscope_android.Data.Repo.TagsRepo;
import com.example.hoho.bioscope_android.app.App;


/**
 * Created by Programming701-A on 12/15/2017.
 */

public class DataBaseHelper extends SQLiteOpenHelper {

    //update when making any changes to tables or indexes
    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NAME = "imageDatabase.db";
    private static final String TAG = DataBaseHelper.class.getSimpleName();

    public DataBaseHelper(){
        super(App.getContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    //creates all tables using the string from each of the repo
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ItemDataHolderRepo.createTable());
        db.execSQL(TagsRepo.createTable());
    }

    @Override
    //drops all tables if there is any change to the database version
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, String.format("SQLiteDatabase.onUpgrade(%d -> %d)", oldVersion, newVersion));
        db.execSQL("Drop Table if Exists "+ ItemDataHolder.TABLE);
        db.execSQL("Drop Table if Exists "+ Tags.TABLE);
        onCreate(db);
    }


}
