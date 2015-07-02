package com.ridelogger.listners;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.CalculatedWheelDistanceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.CalculatedWheelSpeedReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.DataSource;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedPowerReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedTorqueReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IInstantaneousCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IRawPowerOnlyDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.ridelogger.R;
import com.ridelogger.RideService;

import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * Power
 * @author Chet Henry
 * Listen to and log Ant+ Power events
 */
public class Power extends Ant
{
    private final BigDecimal wheelCircumferenceInMeters; //size of wheel to calculate speed
    private final IPluginAccessResultReceiver<AntPlusBikePowerPcc> mResultReceiver;
    
    final SQLiteStatement insertPower;
    final SQLiteStatement insertTorque;
    final SQLiteStatement insertCadence;
    final SQLiteStatement insertSpeed;
    final SQLiteStatement insertDistance;

    //setup listeners and logging 
    public Power(int pDeviceNumber, RideService mContext) {
        super(pDeviceNumber, mContext);
        
        insertPower = context.db.compileStatement(
            "INSERT INTO AntPower (rt, aid, watts) " + 
            "VALUES (?, " + Integer.toString(context.aid) + ", ?)"
        );
        
        insertTorque = context.db.compileStatement(
            "INSERT INTO AntTorque (rt, aid, nm) " + 
            "VALUES (?, " + Integer.toString(context.aid) + ", ?)"
        );
        
        insertCadence = context.db.compileStatement(
            "INSERT INTO AntCadence (rt, aid, cad) " + 
            "VALUES (?, " + Integer.toString(context.aid) + ", ?)"
        );

        insertSpeed = context.db.compileStatement(
            "INSERT INTO AntSpeed (rt, aid, kph) " + 
            "VALUES (?, " + Integer.toString(context.aid) + ", ?)"
        );

        insertDistance = context.db.compileStatement(
            "INSERT INTO AntDistance (rt, aid, km) " + 
            "VALUES (?, " + Integer.toString(context.aid) + ", ?)"
        );

        wheelCircumferenceInMeters = new BigDecimal(
                PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.PREF_WHEEL_SIZE), "2.096")
        );
        
        //Handle messages
        mResultReceiver = new IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {
           //Handle the result, connecting to events on success or reporting failure to user.
           @Override
           public void onResultReceived(AntPlusBikePowerPcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
               if(resultCode == com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult.SUCCESS) {
                   deviceNumber = result.getAntDeviceNumber();
                   
                   result.subscribeCalculatedPowerEvent(new ICalculatedPowerReceiver() {
                           @Override
                           public void onNewCalculatedPower(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedPower) {
                               float val = getTS();
                               context.currentValues[RideService.SECS] = val;
                               insertPower.bindDouble(1, val);
  
                               val = calculatedPower.floatValue();
                               context.currentValues[RideService.WATTS] = val;
                               insertPower.bindDouble(2, val);

                               insertPower.execute();
                           }
                       }
                   );

                   result.subscribeCalculatedTorqueEvent(
                       new ICalculatedTorqueReceiver() {
                           @Override
                           public void onNewCalculatedTorque(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedTorque) {
                               float val = getTS();
                               context.currentValues[RideService.SECS] = val;
                               insertTorque.bindDouble(1, val);
  
                               val = calculatedTorque.floatValue();
                               context.currentValues[RideService.NM] = val;
                               insertTorque.bindDouble(2, val);

                               insertTorque.execute();
                           }
                       }
                   );

                   result.subscribeCalculatedCrankCadenceEvent(
                       new ICalculatedCrankCadenceReceiver() {
                           @Override
                           public void onNewCalculatedCrankCadence(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedCrankCadence) {
                               float val = getTS();
                               context.currentValues[RideService.SECS] = val;
                               insertCadence.bindDouble(1, val);
  
                               val = calculatedCrankCadence.floatValue();
                               context.currentValues[RideService.CAD] = val;
                               insertCadence.bindDouble(2, val);

                               insertCadence.execute();
                           }
                       }
                   );

                   result.subscribeCalculatedWheelSpeedEvent(
                       new CalculatedWheelSpeedReceiver(wheelCircumferenceInMeters) {
                           @Override
                           public void onNewCalculatedWheelSpeed(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedWheelSpeed)
                           {
                               float val = getTS();
                               context.currentValues[RideService.SECS] = val;
                               insertSpeed.bindDouble(1, val);
  
                               val = calculatedWheelSpeed.floatValue();
                               context.currentValues[RideService.KPH] = val;
                               insertSpeed.bindDouble(2, val);

                               insertSpeed.execute();
                           }
                       }
                   );

                   result.subscribeCalculatedWheelDistanceEvent(
                       new CalculatedWheelDistanceReceiver(wheelCircumferenceInMeters) {
                           @Override
                           public void onNewCalculatedWheelDistance(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedWheelDistance) 
                           {
                               float val = getTS();
                               context.currentValues[RideService.SECS] = val;
                               insertDistance.bindDouble(1, val);
  
                               val = calculatedWheelDistance.floatValue();
                               context.currentValues[RideService.KM] = val;
                               insertDistance.bindDouble(2, val);

                               insertDistance.execute();
                           }
                       }
                   );

                   result.subscribeInstantaneousCadenceEvent(
                       new IInstantaneousCadenceReceiver() {
                           @Override
                           public void onNewInstantaneousCadence(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final int instantaneousCadence)
                           {
                               float val = getTS();
                               context.currentValues[RideService.SECS] = val;
                               insertCadence.bindDouble(1, val);
  
                               val = (float) instantaneousCadence;
                               context.currentValues[RideService.CAD] = val;
                               insertCadence.bindDouble(2, val);

                               insertCadence.execute();
                           }
                       }
                   );

                   result.subscribeRawPowerOnlyDataEvent(
                       new IRawPowerOnlyDataReceiver() {
                           @Override
                           public void onNewRawPowerOnlyData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long powerOnlyUpdateEventCount, final int instantaneousPower, final long accumulatedPower)
                           {
                               float val = getTS();
                               context.currentValues[RideService.SECS] = val;
                               insertPower.bindDouble(1, val);
  
                               val = (float) instantaneousPower;
                               context.currentValues[RideService.WATTS] = val;
                               insertPower.bindDouble(2, val);

                               insertPower.execute();
                           }
                       }
                   );

                   /*result.subscribePedalPowerBalanceEvent(
                       new IPedalPowerBalanceReceiver() {
                           @Override
                           public void onNewPedalPowerBalance(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final boolean rightPedalIndicator, final int pedalPowerPercentage)
                           {
                               alterCurrentData(RideService.LTE, pedalPowerPercentage);
                           }
                       }
                   );

                   result.subscribeRawWheelTorqueDataEvent(
                       new IRawWheelTorqueDataReceiver() {
                           @Override
                           public void onNewRawWheelTorqueData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long wheelTorqueUpdateEventCount, final long accumulatedWheelTicks, final BigDecimal accumulatedWheelPeriod, final BigDecimal accumulatedWheelTorque)
                           {
                               alterCurrentData(RideService.NM, accumulatedWheelTorque);
                           }
                       }
                   );

                   result.subscribeRawCrankTorqueDataEvent(
                       new IRawCrankTorqueDataReceiver() {
                           @Override
                           public void onNewRawCrankTorqueData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long crankTorqueUpdateEventCount, final long accumulatedCrankTicks, final BigDecimal accumulatedCrankPeriod, final BigDecimal accumulatedCrankTorque)
                           {
                               alterCurrentData(RideService.NM, accumulatedCrankTorque);
                           }
                       }
                   );

                   result.subscribeTorqueEffectivenessEvent(
                       new ITorqueEffectivenessReceiver() {
                           @Override
                           public void onNewTorqueEffectiveness(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long powerOnlyUpdateEventCount, final BigDecimal leftTorqueEffectiveness, final BigDecimal rightTorqueEffectiveness)
                           {                            
                               int[] keys = {
                                       RideService.LTE,
                                       RideService.RTE
                               };
                               
                               float[] values = {
                                       leftTorqueEffectiveness,
                                       rightTorqueEffectiveness
                               }
                               
                               alterCurrentData(keys, values);
                           }
       
                       }
                   );

                   result.subscribePedalSmoothnessEvent(new IPedalSmoothnessReceiver() {
                           @Override
                           public void onNewPedalSmoothness(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long powerOnlyUpdateEventCount, final boolean separatePedalSmoothnessSupport, final BigDecimal leftOrCombinedPedalSmoothness, final BigDecimal rightPedalSmoothness)
                           {
                               int[] keys = {
                                       RideService.SNPLC,
                                       RideService.SNPR
                               };
                               
                               float[] values = {
                                       leftOrCombinedPedalSmoothness,
                                       rightPedalSmoothness
                               }

                               alterCurrentData(map);
                           }
                       }
                   );*/
               } else if(resultCode == com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult.SEARCH_TIMEOUT) {                   
                   if(deviceNumber != 0) {
                       requestAccess();
                   }
               }
           }
       };
       
       requestAccess();
    }
    
    protected void requestAccess() {
        releaseHandle = AntPlusBikePowerPcc.requestAccess(context, deviceNumber, 0, mResultReceiver, mDeviceStateChangeReceiver);
    }
    
    
    @Override
    public void zeroReadings()
    {   
    }
}


