package com.example.hoho.bioscope_android.Data.Model;

public class Tags {

    public static final String TABLE = "Tags";
    //KEYs to be referred to in the repo where the table will be made - used for column names
    public static final String KEY_PRIMARY = "ID";
    public static final String KEY_FileName = "FileName";
    public static final String KEY_Tag = "Tag";

    //all the values held in one comp row in the item table
    private String filename;
    private String tags;

    public Tags(){
        filename = "None";
        tags = "None";
    }

    public Tags(String s1, String s3){
        filename = s1;
        tags = s3;
    }

    //getters and setters for each of the values
    public String getFilename(){
        return filename;
    }

    public void setFilename(String s){
        filename = s;
    }

    public String getTags(){
        return tags;
    }

    public void setTags(String s){
        tags = s;
    }
}

