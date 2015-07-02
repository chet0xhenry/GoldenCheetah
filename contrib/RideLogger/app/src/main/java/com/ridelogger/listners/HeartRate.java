package com.ridelogger.listners;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.DataState;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.ridelogger.RideService;

import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * HeartRate
 * @author Chet Henry
 * Listen to and log Ant+ HearRate events
 */
public class HeartRate extends Ant
{
    private final IPluginAccessResultReceiver<AntPlusHeartRatePcc> mResultReceiver;
    
    private SQLiteStatement insertHr;
    
    public static void onCreate(SQLiteDatabase db, Context context) {
        executeSQLScript(db, "HeartRate.sql", context);
    }
    
    public HeartRate(int pDeviceNumber, RideService mContext) {
        super(pDeviceNumber, mContext);
        
        insertHr = context.db.compileStatement(
            "INSERT OR REPLACE INTO AntHeartRate (rt, aid, hr) " +
            "VALUES (?, " + Long.toString(context.aid) + ", ?)"
        );

        mResultReceiver = new IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
            //Handle the result, connecting to events on success or reporting failure to user.
            @Override
            public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState)
            {
                if(resultCode == com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult.SUCCESS) {
                    deviceNumber = result.getAntDeviceNumber();
                    result.subscribeHeartRateDataEvent(
                        new IHeartRateDataReceiver() {
                            @Override
                            public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags, final int computedHeartRate, final long heartBeatCount, final BigDecimal heartBeatEventTime, final DataState dataState) {
                                float val = getTs();
                                context.currentValues[RideService.SECS] = val;
                                insertHr.bindDouble(1, val);
                    
                                val = (float) computedHeartRate;
                                context.currentValues[RideService.HR] = val;
                                insertHr.bindDouble(2, val);

                                insertHr.execute();
                                insertHr.clearBindings();
                            }
                        }
                    );
                } else if(resultCode == com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult.SEARCH_TIMEOUT) {
                    if(deviceNumber != 0) {
                        requestAccess();
                    }
                }
            }
        };
        
        
        requestAccess();
    }
    
    
    
    @Override
    protected void requestAccess() {
        releaseHandle = AntPlusHeartRatePcc.requestAccess(context, deviceNumber, 0, mResultReceiver, mDeviceStateChangeReceiver);
    }
}
