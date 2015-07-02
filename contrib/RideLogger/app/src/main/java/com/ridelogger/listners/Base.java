package com.ridelogger.listners;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.ridelogger.RideService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Base
 * @author Chet Henry
 * Base sensor class that has methods to time stamp are write to buffer
 */
public class Base<T>
{    
    final RideService context;
    //final SQLiteStatement someStatment;

    //init statments and listners here
    Base(RideService mContext) {
        context = mContext;
        //initStatments
        //someStatment = context.db.compileStatement("PARAMED SQL...");
    }
    
    //get current time stamp
    float getTs() {
        return (float) ((System.currentTimeMillis() - context.startTime) / 1000.0);   
    }
    
    
    //Clean up my listeners and statments here
    public void onDestroy() {}
    
    public static void onCreate(SQLiteDatabase db, Context context) {
        executeSQLScript(db, "Base.sql", context);
    }
   
    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion, Context context) {
        onCreate(db, context);
    }

    public static void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion, Context context) {
    }


    protected static void executeSQLScript(SQLiteDatabase db, String filename, Context context) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte buf[] = new byte[1024];
        int len;
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;

        try{
            inputStream = assetManager.open(filename);
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();

            String[] createScript = outputStream.toString().split(";");
            for (int i = 0; i < createScript.length; i++) {
                String sqlStatement = createScript[i].trim();
                // TODO You may want to parse out comments here
                if (sqlStatement.length() > 0) {
                    db.execSQL(sqlStatement + ";");
                }
            }
        } catch (IOException e){
            // TODO Handle Script Failed to Load
        } catch (SQLException e) {
            // TODO Handle Script Failed to Execute
        }
    }
}


