package com.example.hoho.bioscope_android.Data;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Programming701-A on 12/18/2017.
 */

public class DatabaseManager {

    private Integer mOpenCounter = 0;

    private static DatabaseManager instance;
    private static SQLiteOpenHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    public static synchronized void initializedInstance(SQLiteOpenHelper helper){
        if (instance == null){
            instance = new DatabaseManager();
            mDatabaseHelper = helper;
        }
    }

    public static synchronized DatabaseManager getInstance(){
        if(instance == null){
            throw new IllegalStateException(DatabaseManager.class.getSimpleName() +
                " is not initialized, call initializeInstance(...) method first.");
        }
        return instance;
    }

    public synchronized SQLiteDatabase openDatabase(){
        mOpenCounter++;
        if(mOpenCounter == 1){
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase(){
        mOpenCounter--;
        if(mOpenCounter == 0){
            mDatabase.close();
        }
    }
}
