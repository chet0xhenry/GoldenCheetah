package com.ridelogger.formats;

import android.content.res.AssetManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.ridelogger.GzipWriter;
import com.ridelogger.RideService;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class BaseFormat<T> {
    GzipWriter  buf;
    final RideService context;
    String      subExt = "";
    
    BaseFormat(RideService rideService) {
        context = rideService;
    }
    
    
    public void createFile() {
        File dir = new File(
            Environment.getExternalStorageDirectory(), 
            "Rides"
        );
        
        dir.mkdirs();
        
        Date             startDate = new Date(context.startTime);
        SimpleDateFormat filef     = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String           fileName  = filef.format(startDate) + subExt + ".gz";
        
        try {
            buf = new GzipWriter(new BufferedOutputStream(new FileOutputStream(new File(dir, fileName))));
        } catch (Exception e) {}
    }
    
    
    public void writeHeader(){
        try {
            synchronized (buf) {
                for(CharSequence key : RideService.KEYS) {
                    buf.write(key);
                    buf.write(",");
                }
            }
        } catch (Exception e) {}
    }
    
    
    public void writeValues() {
        try {
            synchronized (buf) {
                for(float value : context.currentValues) {
                    buf.write(value);
                }
            }
        } catch (Exception e) {}
    }


    public void writeFooter() {
        try {
            buf.close();
        } catch (Exception e) {
        }
    }
}
