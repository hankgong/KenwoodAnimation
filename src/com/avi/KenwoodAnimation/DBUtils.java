package com.avi.KenwoodAnimation;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: hankgong
 * Date: 01/12/12
 * Time: 5:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class DBUtils {
    // path and name
    //private static String DB_PATH = "/data/data/com.avi.kenwoodcanada/databases/";
//	private static String DB_NAME = "kenwood.db";

//	private static SQLiteDatabase _db;


    public enum DataType {
        STRING, INTEGER
    }


    //static constructor
//	static {
//
//		Log.i("tracing", "DBUtils::static constructor..");
//
//		_db = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
//
//		String sql = "select _id, name from search_tag";
//		Cursor cursor = _db.rawQuery(sql, null);
//    	cursor.moveToFirst();
//
//    	while(!cursor.isAfterLast()) {
//    		SearchTagID.put(cursor.getString(1), cursor.getInt(0));
//			cursor.moveToNext();
//		}
//
//	}

//	public DBUtils() {
//	}

    public static void createDatabaseIfNotExists(Context c, String dbname) throws IOException {
        boolean createDb = false;

        Log.i("tracing", "DBUtils::createDatabaseIfNotExists..");

//        File dbDir = new File(DB_PATH);
//        File dbFile = new File(DB_PATH + DB_NAME);
        SQLiteDatabase tdb = null;

        System.out.println(c.getPackageName() + " " + dbname);
        try {
            tdb = c.openOrCreateDatabase(dbname, 0, null);
        } catch (Exception e) {
            System.out.println(e);
        }
        tdb.close();

        String dbfullpath = c.getDatabasePath(dbname).getPath();

        createDb = true;

//        dbFile.delete();
//
//        if (!dbDir.exists()) {
//            dbDir.mkdir();
//            createDb = true;
//        }
//        else if (!dbFile.exists()) {
//            createDb = true;
//        }
//        else {
//            // Check that we have the latest version of the db
//            boolean doUpgrade = false;
//
//            // Insert your own logic here on whether to upgrade the db; I personally
//            // just store the db version # in a text file, but you can do whatever
//            // you want.  I've tried MD5 hashing the db before, but that takes a while.
//
//            // If we are doing an upgrade, basically we just delete the db then
//            // flip the switch to create a new one
//            if (doUpgrade) {
//                dbFile.delete();
//                createDb = true;
//            }
//        }

        if (createDb) {
            // Open your local db as the input stream
            InputStream myInput = c.getAssets().open(dbname);

            // Open the empty db as the output stream
            OutputStream myOutput = new FileOutputStream(dbfullpath);

            // transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }

            // Close the streams
            myOutput.flush();
            myOutput.close();
            myInput.close();
        }
    }


    public static ArrayList<String> getStringListbyDBQuery(SQLiteDatabase db, String sql, String fieldName){
        Cursor cursor = db.rawQuery(sql, null);
        int index = cursor.getColumnIndex(fieldName);
        ArrayList<String> retList = new ArrayList<String>();

        cursor.moveToFirst();

        while(!cursor.isAfterLast()) {
            retList.add(cursor.getString(index));
            cursor.moveToNext();
        }

        return retList;
    }

    public static ArrayList<Integer> getIntegerListbyDBQuery(SQLiteDatabase db, String sql, String fieldName){
        Cursor cursor = db.rawQuery(sql, null);
        int index = cursor.getColumnIndex(fieldName);
        ArrayList<Integer> retList = new ArrayList<Integer>();

        cursor.moveToFirst();

        while(!cursor.isAfterLast()) {
            retList.add(cursor.getInt(index));
            cursor.moveToNext();
        }

        return retList;
    }

    public static ArrayList<Float> getFloatListbyDBQuery(SQLiteDatabase db, String sql, String fieldName){
        Cursor cursor = db.rawQuery(sql, null);
        int index = cursor.getColumnIndex(fieldName);
        ArrayList<Float> retList = new ArrayList<Float>();

        cursor.moveToFirst();

        while(!cursor.isAfterLast()) {
            retList.add(cursor.getFloat(index));
            cursor.moveToNext();
        }

        return retList;
    }

}
