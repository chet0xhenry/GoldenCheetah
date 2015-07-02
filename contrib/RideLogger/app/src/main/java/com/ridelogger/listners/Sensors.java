package com.ridelogger.listners;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;

import com.ridelogger.R;
import com.ridelogger.RideService;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Sensors
 * @author Chet Henry
 * Listen to android sensor events and log them
 */
public class Sensors extends Base<Object>
{
    private static final double  CRASHMAGNITUDE = 30.0;
    
    private SensorEventListener luxListner;
    private final SQLiteStatement insertLux;
    
    private SensorEventListener accelListner;
    private final SQLiteStatement insertAccel;
   
    private SensorEventListener pressListner;
    private final SQLiteStatement insertPress;
    
    private SensorEventListener tempListner;
    private final SQLiteStatement insertTemp;
    
    private SensorEventListener fieldListner;
    private final SQLiteStatement insertField;
    
    public static void onCreate(SQLiteDatabase db, Context context) {
        executeSQLScript(db, "Sensors.sql", context);
    }

    public Sensors(RideService mContext) 
    {
        super(mContext);
        
        SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        
        Sensor mLight         = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Sensor mAccel         = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor mPress         = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        Sensor mTemp          = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        Sensor mField         = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
          
        if(mLight != null) {
            insertLux = context.db.compileStatement(
                "INSERT OR REPLACE INTO AndroidLux (rt, aid, lux) " +
                "VALUES (?, " + Long.toString(context.aid) + ", ?)"
            );

            luxListner = new SensorEventListener() {
                @Override
                public final void onAccuracyChanged(Sensor sensor, int accuracy) {}
                
                @Override
                public final void onSensorChanged(SensorEvent event) {
                    // The light sensor returns a single value.
                    // Many sensors return 3 values, one for each axis.
                    float val = getTs();
                    context.currentValues[RideService.SECS] = val;
                    insertLux.bindDouble(1, val);
  
                    val = event.values[0];
                    context.currentValues[RideService.lux] = val;
                    insertLux.bindDouble(2, val);

                    insertLux.execute();
                    insertLux.clearBindings();
                }
            };
            
            mSensorManager.registerListener(luxListner,   mLight, 3000000);
        } else {
            insertLux = null;
        }
        
        if(mAccel != null) {
            insertAccel = context.db.compileStatement(
                "INSERT OR REPLACE INTO AndroidAccel (rt, aid, ms2x, ms2y, ms2z) " +
                "VALUES (?, " + Long.toString(context.aid) + ", ?, ?, ?)"
            );
            
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            if(settings.getBoolean(context.getString(R.string.PREF_DETECT_CRASH), false)) {
                accelListner = new SensorEventListener() {
                    private boolean crashed = false;
                    private final Timer   timer   = new Timer();
                    private final double[] St     = new double[3];
        
                    @Override
                    public final void onAccuracyChanged(Sensor sensor, int accuracy) {}
                      
                    @Override
                    public final void onSensorChanged(SensorEvent event) {                
                        float val = getTs();
                        context.currentValues[RideService.SECS] = val;
                        insertAccel.bindDouble(1, val);
  
                        val = event.values[0];
                        context.currentValues[RideService.ms2x] = val;
                        insertAccel.bindDouble(2, val);
                        
                        val = event.values[1];
                        context.currentValues[RideService.ms2y] = val;
                        insertAccel.bindDouble(2, val);
                        
                        val = event.values[2];
                        context.currentValues[RideService.ms2z] = val;
                        insertAccel.bindDouble(3, val);

                        insertAccel.execute();
                        insertAccel.clearBindings();
                        
                        if(St.length == 0) {
                            St[0] = event.values[0];
                            St[1] = event.values[1];
                            St[2] = event.values[2];
                        }
                        
                        St[0] = 0.6 * event.values[0] + 0.4 * St[0];
                        St[1] = 0.6 * event.values[1] + 0.4 * St[1];
                        St[2] = 0.6 * event.values[2] + 0.4 * St[2];
                        
                        double amag = Math.sqrt(St[0]*St[0] + St[1]*St[1] + St[2]*St[2]);
                        
                        if(amag > CRASHMAGNITUDE && !crashed) {
                            crashed = true;
                            context.phoneCrash(amag);

                            if(!Float.isNaN(context.currentValues[RideService.KPH])) {
                                timer.schedule(
                                    new TimerTask() {              
                                        @Override  
                                        public void run() {
                                            //if we are traveling less then 1km/h at 5 seconds after crash detection
                                            // confirm the crash
                                            if(1.0 > context.currentValues[RideService.KPH]) {
                                                context.phoneCrashConfirm();
                                            } else {
                                                crashed = false;
                                                context.phoneHome();
                                            }
                                        }  
                                    }, 
                                    5000
                                ); //in five sec reset
                            } else {
                                timer.schedule(
                                    new TimerTask() {              
                                        @Override  
                                        public void run() {
                                            crashed = false;
                                        }  
                                    }, 
                                    180000
                                ); //in three min reset
                            }
                        }
                    }
                };
            } else {
                accelListner = new SensorEventListener() {
                    @Override
                    public final void onAccuracyChanged(Sensor sensor, int accuracy) {}
                      
                    @Override
                    public final void onSensorChanged(SensorEvent event) {                                        
                        float val = getTs();
                        context.currentValues[RideService.SECS] = val;
                        insertAccel.bindDouble(1, val);
  
                        val = event.values[0];
                        context.currentValues[RideService.ms2x] = val;
                        insertAccel.bindDouble(2, val);
                        
                        val = event.values[1];
                        context.currentValues[RideService.ms2y] = val;
                        insertAccel.bindDouble(2, val);
                        
                        val = event.values[2];
                        context.currentValues[RideService.ms2z] = val;
                        insertAccel.bindDouble(3, val);

                        insertAccel.execute();
                        insertAccel.clearBindings();
                    }
                };
            }

            mSensorManager.registerListener(accelListner, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            insertAccel = null;
        }
        
        if(mPress != null) {
            insertPress = context.db.compileStatement(
                "INSERT OR REPLACE INTO AndroidPress (rt, aid, press) " +
                "VALUES (?, " + Long.toString(context.aid) + ", ?)"
            );
            
            pressListner = new SensorEventListener() {
                @Override
                public final void onAccuracyChanged(Sensor sensor, int accuracy) {}
                
                @Override
                public final void onSensorChanged(SensorEvent event) {
                    float val = getTs();
                    context.currentValues[RideService.SECS] = val;
                    insertPress.bindDouble(1, val);
  
                    val = event.values[0];
                    context.currentValues[RideService.press] = val;
                    insertPress.bindDouble(2, val);

                    insertPress.execute();
                    insertPress.clearBindings();
                }
            };
            
            mSensorManager.registerListener(pressListner, mPress, 3000000);
        } else {
            insertPress = null;
        }
        
        if(mTemp != null) {
            insertTemp = context.db.compileStatement(
                "INSERT OR REPLACE INTO AndroidTemp (rt, aid, temp) " +
                "VALUES (?, " + Long.toString(context.aid) + ", ?)"
            );
            
            tempListner = new SensorEventListener() {
                @Override
                public final void onAccuracyChanged(Sensor sensor, int accuracy) {}
                
                @Override
                public final void onSensorChanged(SensorEvent event) {
                    float val = getTs();
                    context.currentValues[RideService.SECS] = val;
                    insertTemp.bindDouble(1, val);
  
                    val = event.values[0];
                    context.currentValues[RideService.temp] = val;
                    insertTemp.bindDouble(2, val);

                    insertTemp.execute();
                    insertTemp.clearBindings();
                }
            };
            
            mSensorManager.registerListener(tempListner,  mTemp,  3000000);
        } else {
            insertTemp = null;
        }
        
        if(mField != null) {
            insertField = context.db.compileStatement(
                "INSERT OR REPLACE INTO AndroidField (rt, aid, uTx, uTy, uTz) " +
                "VALUES (?, " + Long.toString(context.aid) + ", ?, ?, ?)"
            );
            
            fieldListner = new SensorEventListener() {
                @Override
                public final void onAccuracyChanged(Sensor sensor, int accuracy) {}
                
                @Override
                public final void onSensorChanged(SensorEvent event) {                  
                    float val = getTs();
                    context.currentValues[RideService.SECS] = val;
                    insertField.bindDouble(1, val);
  
                    val = event.values[0];
                    context.currentValues[RideService.uTx] = val;
                    insertField.bindDouble(2, val);

                    val = event.values[1];
                    context.currentValues[RideService.uTy] = val;
                    insertField.bindDouble(3, val);
                    
                    val = event.values[2];
                    context.currentValues[RideService.uTz] = val;
                    insertField.bindDouble(4, val);
                    
                    insertField.execute();
                    insertField.clearBindings();
                }
            };
            
            mSensorManager.registerListener(fieldListner, mField, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            insertField = null;
        }
    }
    
    @Override
    public void onDestroy()
    {
        SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if(luxListner != null) {
            mSensorManager.unregisterListener(luxListner);
        }
        if(accelListner != null) {
            mSensorManager.unregisterListener(accelListner);
        }
        if(pressListner != null) {
            mSensorManager.unregisterListener(pressListner);
        }
        if(tempListner != null) {
            mSensorManager.unregisterListener(tempListner);
        }
        if(fieldListner != null) {
            mSensorManager.unregisterListener(fieldListner);
        }
    }
}
