package com.example.hoho.bioscope_android.Data.Repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.hoho.bioscope_android.Data.DatabaseManager;
import com.example.hoho.bioscope_android.Data.Model.ItemDataHolder;

import java.util.ArrayList;

public class ItemDataHolderRepo {

    private final String TAG = ItemDataHolderRepo.class.getSimpleName();

    public static String createTable(){
        return "CREATE TABLE " + ItemDataHolder.TABLE + "("
                + ItemDataHolder.KEY_PRIMARY + " INTEGER not null PRIMARY KEY AUTOINCREMENT , "
                + ItemDataHolder.KEY_FileName + " TEXT not null , "
                + ItemDataHolder.KEY_Description + " TEXT ) ";
    }

    public int insert(ItemDataHolder itemDataHolder){
        int compId = -1;
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        ContentValues values = new ContentValues();
        values.put(ItemDataHolder.KEY_FileName, itemDataHolder.getFilename());
        values.put(ItemDataHolder.KEY_Description, itemDataHolder.getDescription());

        compId = (int) db.insertWithOnConflict(ItemDataHolder.TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        DatabaseManager.getInstance().closeDatabase();

        return compId;
    }

    public void update(ItemDataHolder itemDataHolder){
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        ContentValues values = new ContentValues();
        values.put(ItemDataHolder.KEY_FileName, itemDataHolder.getFilename());
        values.put(ItemDataHolder.KEY_Description, itemDataHolder.getDescription());

        db.update(ItemDataHolder.TABLE, values, ItemDataHolder.KEY_FileName + " = \"" + itemDataHolder.getFilename()+ "\"", null);
        DatabaseManager.getInstance().closeDatabase();
    }

    public void deleteAll(){
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        db.delete(ItemDataHolder.TABLE, null, null);
        DatabaseManager.getInstance().closeDatabase();
    }

    public void delete(String file){
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        db.delete(ItemDataHolder.TABLE, ItemDataHolder.KEY_FileName + " =  \"" + file + "\"", null) ;
        DatabaseManager.getInstance().closeDatabase();
    }

    public ItemDataHolder getItemDataHolder(String file){
        ItemDataHolder itemDataHolder = new ItemDataHolder();

        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        String selectQuery = " SELECT ItemData." + ItemDataHolder.KEY_FileName
                + ", ItemData." + ItemDataHolder.KEY_Description
                + " FROM " + ItemDataHolder.TABLE
                + " WHERE ItemData." + ItemDataHolder.KEY_FileName + " = \"" + file + "\"";

        Log.d(TAG, selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);
        if ( cursor.moveToFirst()){
            itemDataHolder.setFilename(cursor.getString(cursor.getColumnIndex(ItemDataHolder.KEY_FileName)));
            itemDataHolder.setDescription(cursor.getString(cursor.getColumnIndex(ItemDataHolder.KEY_Description)));

        }
        cursor.close();
        DatabaseManager.getInstance().closeDatabase();
        return itemDataHolder;
    }

    public boolean databaseIsEmpty(){
        int items = 0;
        boolean empty = true;
        SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
        String selectQuery = " SELECT ItemData." + ItemDataHolder.KEY_FileName
                + ", ItemData." + ItemDataHolder.KEY_Description
                + " FROM " + ItemDataHolder.TABLE;

        Log.d(TAG, selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()){
            do {
                items++;
            }while(cursor.moveToNext());
        }
        cursor.close();
        DatabaseManager.getInstance().closeDatabase();
        if(items > 0) empty = false;
        return empty;
    }

}
