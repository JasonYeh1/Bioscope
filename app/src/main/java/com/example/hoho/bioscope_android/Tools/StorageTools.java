package com.example.hoho.bioscope_android.Tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class StorageTools {

    private static final File BASE_DIR = Environment.getExternalStorageDirectory();
    private static File mDir;

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    public static File createDirectory(String dir){
        File f = new File(BASE_DIR.getAbsolutePath(), dir);
        //if(!f.exists())
        f.mkdirs();
        return f;

    }

    public static boolean renameFile(String file, String name){
        boolean b = false;
        File directory = new File(StorageTools.getDirectory().getAbsolutePath());
        File[] images = directory.listFiles();
        for(File f: images){
            if(f.toString().equals(mDir.getAbsolutePath() + "/" + file)){
                f.renameTo(new File(mDir+"/"+name));
                b = true;
            }
        }
        return b;
    }

    public static File createDirectoryOnInternalStorage(Context context, String dir) {
        mDir = new File(context.getFilesDir(), dir);
        //if (!mDir.exists()) {
        mDir.mkdir();
        //}
        return mDir;
    }

//    public static void saveFiletoInternalStorage(String sFileName){
//        try{
//            File gpxfile = new File(mDir, sFileName);
//            FileWriter writer = new FileWriter(gpxfile);
//            writer.append(sBody);
//            writer.flush();
//            writer.close();
//
//        }catch (Exception e){
//            e.printStackTrace();
//
//        }
//    }

    public static File createFile(String dir, String filename){
        File path = createDirectory(dir);
        File f = new File(path, filename);
        if(!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e){
                e.printStackTrace();
            }
        return f;
    }

    public static boolean deleteFile(String file){
        File fileLocation = new File(mDir, file);
        boolean deleted = fileLocation.delete();
        return deleted;
    }

    public static void deleteFiles(String dir){
        deleteDirectory(new File(BASE_DIR.getAbsolutePath()+"/"+dir));
    }

    public static boolean deleteDirectory(File path) {
        if( path.exists() ) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
        return( path.delete() );
    }

    public static File getDirectory() {
        System.out.println(mDir);
        return mDir;
    }

    public static File[] getImageNames() {
        File[] files = mDir.listFiles();
        int i = 0;
        for (File file : files) {
            if (file.getName().endsWith(".jpg")) {
                //fileNames[i] = file;
                i++;
            }
        }
        File[] fileNames = new File[i];
        i = 0;
        for (File file : files) {
            if (file.getName().endsWith(".jpg")) {
                fileNames[i] = file;
                i++;
            }
        }
        return fileNames;
    }

    public static File[] getVidNames() {
        File[] files = mDir.listFiles();
        File[] fileNames = new File[files.length];
        int i = 0;
        for (File file : files) {
            if (file.getName().endsWith(".mp4")) {
                fileNames[i] = file;
                i++;
            }
        }
        return fileNames;
    }

    public static Bitmap loadImageBitmap(Context context, String name){
        name = name;
        FileInputStream fileInputStream;
        Bitmap bitmap = null;
        try{
            fileInputStream = context.openFileInput(name);
            bitmap = BitmapFactory.decodeStream(fileInputStream);
            fileInputStream.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}