package com.example.hoho.bioscope_android.Data.Repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Adapter;
import android.widget.ArrayAdapter;

import com.example.hoho.bioscope_android.Data.DatabaseManager;
import com.example.hoho.bioscope_android.Data.Model.Tags;

import java.util.ArrayList;

public class TagsRepo {

    private final String TAG = ItemDataHolderRepo.class.getSimpleName();

    public static String createTable(){
        return "CREATE TABLE " + Tags.TABLE + "("
                + Tags.KEY_PRIMARY + " INTEGER not null PRIMARY KEY AUTOINCREMENT , "
                + Tags.KEY_FileName + " TEXT not null , "
                + Tags.KEY_Tag + " TEXT ) ";
    }

    public int insert(Tags tags){
        int compId = -1;
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        ContentValues values = new ContentValues();
        values.put(Tags.KEY_FileName, tags.getFilename());
        values.put(Tags.KEY_Tag, tags.getTags());

        compId = (int) db.insertWithOnConflict(tags.TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        DatabaseManager.getInstance().closeDatabase();

        return compId;
    }

    public void updateFileName(Tags tags, String file){
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        ContentValues values = new ContentValues();
        values.put(Tags.KEY_FileName, tags.getFilename());

        db.update(Tags.TABLE, values, Tags.KEY_FileName + " = \"" + file + "\"", null);
        DatabaseManager.getInstance().closeDatabase();
    }

    public void deleteAll(){
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        db.delete(Tags.TABLE, null, null);
        DatabaseManager.getInstance().closeDatabase();
    }

    public void delete(String file, String tag){
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        db.delete(Tags.TABLE, Tags.KEY_FileName + " =  \"" + file +  "\" AND "
                + Tags.KEY_Tag + " = \"" + tag + "\"", null);
        DatabaseManager.getInstance().closeDatabase();
    }

    public void deleteByFile(String file){
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        db.delete(Tags.TABLE, Tags.KEY_FileName + " =  \"" + file +  "\"", null);
        DatabaseManager.getInstance().closeDatabase();
    }

    //get all unique tags from database
    public ArrayList<String> getAllTags(){
        ArrayList<String> tags = new ArrayList<>();

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        String selectQuery = " SELECT DISTINCT Tags." + Tags.KEY_Tag
                + " FROM " + Tags.TABLE;

        Log.d(TAG, selectQuery);
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);
            if ( cursor.moveToFirst()){
                do{
                    tags.add(cursor.getString(cursor.getColumnIndex(Tags.KEY_Tag)));
                }while(cursor.moveToNext());

            }
            cursor.close();
            DatabaseManager.getInstance().closeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tags;
    }

    //used to get all tags for a single file
    public ArrayList<String> getTagsByFile(String file){
        ArrayList<String> tags = new ArrayList<>();

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        String selectQuery = " SELECT Tags." + Tags.KEY_FileName
                + ", Tags." + Tags.KEY_Tag
                + " FROM " + Tags.TABLE
                + " WHERE Tags." + Tags.KEY_FileName + " = \"" + file + "\"";

        Log.d(TAG, selectQuery);
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);
            if ( cursor.moveToFirst()){
                do{
                    tags.add(cursor.getString(cursor.getColumnIndex(Tags.KEY_Tag)));
                }while(cursor.moveToNext());

            }
            cursor.close();
            DatabaseManager.getInstance().closeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tags;
    }


    //used to get all files with a specified tag
    public ArrayList<String> getFileByTags(String tag){
        ArrayList<String> files = new ArrayList<>();

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        String selectQuery = " SELECT Tags." + Tags.KEY_FileName
                + ", Tags." + Tags.KEY_Tag
                + " FROM " + Tags.TABLE
                + " WHERE Tags." + Tags.KEY_Tag + " = \"" + tag + "\"";

        Log.d(TAG, selectQuery);
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);
            if ( cursor.moveToFirst()){
                do{
                    files.add(cursor.getString(cursor.getColumnIndex(Tags.KEY_FileName)));
                }while(cursor.moveToNext());

            }
            cursor.close();
            DatabaseManager.getInstance().closeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }
}
