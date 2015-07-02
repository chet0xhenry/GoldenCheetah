package com.ridelogger.listners;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.ridelogger.RideService;

/**
 * Gps 
 * @author henry
 * Listen and log gps events
 */
public class Gps extends Base<Gps>
{
    final SQLiteStatement insertGps;
    
    public Gps(RideService mContext) 
    {
        super(mContext);
        
        insertGps = context.db.compileStatement(
            "INSERT INTO Gps (rt, aid, alt, kph, bear, gpsa, lat, lon) " + 
            "VALUES (?, " + Integer.toString(context.aid) + ", ?, ?, ?, ?, ?, ?)"
        );
        
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //listen to gps events and log them
        LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    float val = getTS();
                    context.currentValues[RideService.SECS] = val;
                    insertGps.bindDouble(1, val);
                   
                    val = (float) location.getAltitude();
                    context.currentValues[RideService.ALTITUDE] = val;
                    insertGps.bindDouble(2, val);
                   
                    val = location.getSpeed();
                    context.currentValues[RideService.KPH] = val;
                    insertGps.bindDouble(3, val);
                   
                    val =  location.getBearing();
                    context.currentValues[RideService.bearing] = val;
                    insertGps.bindDouble(4, val);
                   
                    val = location.getAccuracy();
                    context.currentValues[RideService.gpsa] = val;
                    insertGps.bindDouble(5, val);
                   
                    val = (float) location.getLatitude();
                    context.currentValues[RideService.LAT] = val;
                    insertGps.bindDouble(6, val);
                   
                    val = (float) location.getLongitude();
                    context.currentValues[RideService.LON] = val;
                    insertGps.bindDouble(7, val);
                    
                    insertGps.execute();
                }
            
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
            
                @Override
                public void onProviderEnabled(String provider) {}
            
                @Override
                public void onProviderDisabled(String provider) {}
          };
          
          locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }
}
