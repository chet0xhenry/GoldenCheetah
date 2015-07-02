package com.ridelogger.listners;

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


    void alterCurrentData(int key, float value)
    {
        synchronized (context.currentValues) {
            context.currentValues[RideService.SECS] = getTs();
            context.currentValues[key] = value;
        }
    }

    
    void alterCurrentData(int[] keys, float[] values)
    {
        synchronized (context.currentValues) {
            context.currentValues[RideService.SECS] = getTs();
            
            int i = 0;
            for (int key : keys) {
                context.currentValues[key] = values[i];
                i++;
            }
        }
    }
    
    
    //get current time stamp
    private float getTs() {
        return (float) ((System.currentTimeMillis() - context.startTime) / 1000.0);   
    }
    
    
    //Clean up my listeners and statments here
    public void onDestroy() {}
    
    //zero any of my values
    void zeroReadings() {}

    public void onUpgrade(int oldVersion, int newVersion) {
        executeSQLScript("base.sql");
    }

    public void onDowngrade(int oldVersion, int newVersion) {
        onUpgrade(oldVersion, newVersion);
    }


    protected void executeSQLScript(String filename) {
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
                    context.db.execSQL(sqlStatement + ";");
                }
            }
        } catch (IOException e){
            // TODO Handle Script Failed to Load
        } catch (SQLException e) {
            // TODO Handle Script Failed to Execute
        }
    }
}


