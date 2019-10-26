package com.example.hoho.bioscope_android.Data.Model;

public class ItemDataHolder {

    public static final String TABLE = "ItemData";
    //KEYs to be referred to in the repo where the table will be made - used for column names
    public static final String KEY_PRIMARY = "ID";
    public static final String KEY_FileName = "FileName";
    public static final String KEY_Description = "Description";

    //all the values held in one comp row in the item table
    private String filename;
    private String description;

    public ItemDataHolder(){
        filename = "None";
        description = "None";
    }

    public ItemDataHolder(String s1, String s2){
        filename = s1;
        description = s2;
    }

    //getters and setters for each of the values
    public String getFilename(){
        return filename;
    }

    public void setFilename(String s){
        filename = s;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String s){
        description = s;
    }
}

