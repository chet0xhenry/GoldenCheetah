package com.ridelogger.formats;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.garmin.fit.DateTime;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.RecordMesg;
import com.ridelogger.RideService;

import android.os.Environment;

public class FitFormat extends BaseFormat<Object> {
    FileEncoder encoder;
    public FitFormat(RideService rideService) {
        super(rideService);
    }
    
    public void createFile() {
        File dir = new File(
            Environment.getExternalStorageDirectory(), 
            "Rides"
        );
        
        dir.mkdirs();
        
        Date             startDate = new Date(context.startTime);
        SimpleDateFormat filef     = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String           fileName  = filef.format(startDate) + ".fit";
        
        try {
            encoder = new FileEncoder(new File(dir, fileName));
        } catch (Exception e) {}
    }
    
    public void writeHeader() {
        
        FileIdMesg fileIdMesg = new FileIdMesg();

        fileIdMesg.setManufacturer(Manufacturer.DEVELOPMENT);
        fileIdMesg.setProduct(0);
        fileIdMesg.setSerialNumber(12345L);

        encoder.write(fileIdMesg);
    }
    
    
    public void writeValues() {
        RecordMesg msg = new RecordMesg();
        msg.setAltitude(context.currentValues[RideService.ALTITUDE]);
        msg.setCadence((short) context.currentValues[RideService.CAD]);
        msg.setHeartRate((short) context.currentValues[RideService.HR]);
        msg.setPower((int) context.currentValues[RideService.WATTS]);
        msg.setPositionLat((int) context.currentValues[RideService.LAT]);
        msg.setPositionLong((int) context.currentValues[RideService.LON]);
        msg.setSpeed(context.currentValues[RideService.KPH]);
        msg.setTimestamp(new DateTime((long) (context.currentValues[RideService.KPH] + context.startTime)));
        
        try {
            synchronized (encoder) {        
                encoder.write(msg);
            }
        } catch (Exception e) {}
    }
    
    
    public void writeFooter() {
        encoder.close();
    }
}
