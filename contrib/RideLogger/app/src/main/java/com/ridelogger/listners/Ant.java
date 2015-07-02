package com.ridelogger.listners;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.ridelogger.RideService;


/**
 * Ant
 * @author Chet Henry
 * Listen to and log Ant+ events base class
 */
public abstract class Ant extends Base<Object>
{
    PccReleaseHandle<?> releaseHandle;    //Handle class
    public              IPluginAccessResultReceiver<?> mResultReceiver;  //Receiver class
    int                 deviceNumber = 0;
    
    public static void onCreate(SQLiteDatabase db, Context context) {
        executeSQLScript(db, "Ant.sql", context);
    }
    
    //setup listeners and logging 
    Ant(int pDeviceNumber, RideService mContext)
    {
        super(mContext);
        deviceNumber = pDeviceNumber;
    }
    
    
    final IDeviceStateChangeReceiver mDeviceStateChangeReceiver = new IDeviceStateChangeReceiver()
    {
        @Override
        public void onDeviceStateChange(final DeviceState newDeviceState){
            //if we lose a device zero out its values
            /*if(newDeviceState.equals(DeviceState.DEAD)) {
                zeroReadings();
            }*/
        }
    };
    
    
    abstract protected void requestAccess();

    
    @Override
    public void onDestroy()
    {
        if(releaseHandle != null) {
            releaseHandle.close();
        }
    }
}


