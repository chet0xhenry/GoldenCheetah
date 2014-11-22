package com.ridelogger.listners;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.CalculatedWheelDistanceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.CalculatedWheelSpeedReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.DataSource;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedPowerReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedTorqueReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IInstantaneousCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IRawPowerOnlyDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IRawWheelTorqueDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult;
import com.ridelogger.RideService;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Power
 * @author Chet Henry
 * Listen to and log Ant+ Power events
 */
public class Power extends Ant
{
    public BigDecimal wheelCircumferenceInMeters = new BigDecimal("2.07"); //size of wheel to calculate speed
    
    //setup listeners and logging 
    public Power(MultiDeviceSearchResult result, RideService mContext) {
        super(result, mContext);
        releaseHandle = AntPlusBikePowerPcc.requestAccess(context, result.getAntDeviceNumber(), 0, mResultReceiver, mDeviceStateChangeReceiver);
    }
    
    
    public Power(MultiDeviceSearchResult result, RideService mContext, Boolean psnoop) {
        super(result, mContext, psnoop);
        releaseHandle = AntPlusBikePowerPcc.requestAccess(context, result.getAntDeviceNumber(), 0, mResultReceiver, mDeviceStateChangeReceiver);
    }
    
    
    //Handle messages
    protected IPluginAccessResultReceiver<AntPlusBikePowerPcc> mResultReceiver = new IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {
        //Handle the result, connecting to events on success or reporting failure to user.
        @Override
        public void onResultReceived(final AntPlusBikePowerPcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
            if(resultCode == com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult.SUCCESS) {
                result.subscribeCalculatedPowerEvent(new ICalculatedPowerReceiver() {
                        @Override
                        public void onNewCalculatedPower(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedPower) {
                            alterCurrentData("WATTS", reduceNumberToString(calculatedPower));
                        }
                    }
                );

                result.subscribeCalculatedTorqueEvent(
                    new ICalculatedTorqueReceiver() {
                        @Override
                        public void onNewCalculatedTorque(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedTorque) {
                            alterCurrentData("NM", reduceNumberToString(calculatedTorque));
                        }
                    }
                );

                result.subscribeCalculatedCrankCadenceEvent(
                    new ICalculatedCrankCadenceReceiver() {
                        @Override
                        public void onNewCalculatedCrankCadence(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedCrankCadence) {
                            alterCurrentData("CAD", reduceNumberToString(calculatedCrankCadence));
                        }
                    }
                );

                result.subscribeCalculatedWheelSpeedEvent(
                    new CalculatedWheelSpeedReceiver(wheelCircumferenceInMeters) {
                        @Override
                        public void onNewCalculatedWheelSpeed(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedWheelSpeed)
                        {
                            alterCurrentData("KPH", reduceNumberToString(calculatedWheelSpeed));
                        }
                    }
                );

                result.subscribeCalculatedWheelDistanceEvent(
                    new CalculatedWheelDistanceReceiver(wheelCircumferenceInMeters) {
                        @Override
                        public void onNewCalculatedWheelDistance(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final BigDecimal calculatedWheelDistance) 
                        {
                            alterCurrentData("KM", reduceNumberToString(calculatedWheelDistance));
                        }
                    }
                );

                result.subscribeInstantaneousCadenceEvent(
                    new IInstantaneousCadenceReceiver() {
                        @Override
                        public void onNewInstantaneousCadence(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final DataSource dataSource, final int instantaneousCadence)
                        {
                            alterCurrentData("CAD", reduceNumberToString(instantaneousCadence));
                        }
                    }
                );

                result.subscribeRawPowerOnlyDataEvent(
                    new IRawPowerOnlyDataReceiver() {
                        @Override
                        public void onNewRawPowerOnlyData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long powerOnlyUpdateEventCount, final int instantaneousPower, final long accumulatedPower)
                        {
                            alterCurrentData("WATTS", reduceNumberToString(instantaneousPower));
                        }
                    }
                );

                /*result.subscribePedalPowerBalanceEvent(
                    new IPedalPowerBalanceReceiver() {
                        @Override
                        public void onNewPedalPowerBalance(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final boolean rightPedalIndicator, final int pedalPowerPercentage)
                        {
                            alterCurrentData("LTE", reduceNumberToString(pedalPowerPercentage));
                        }
                    }
                );
                */
                result.subscribeRawWheelTorqueDataEvent(
                    new IRawWheelTorqueDataReceiver() {
                        private long        totalRevs;
                        private long        wheelZeros;
                        private long        lastCount;
                        private long        wheel_period;
                        private long        lastPeriod;
                        private long        lastRevCount;
                        private long        lastTorque;
                        private double      kph;
                        private double      watt;

                        @Override
                        public void onNewRawWheelTorqueData(
                            long estTimestamp, 
                            EnumSet<EventFlag> eventFlags, 
                            long wheelTorqueUpdateEventCount, 
                            long accumulatedWheelTicks, 
                            BigDecimal accumulatedWheelPeriodBD, 
                            BigDecimal accumulatedWheelTorqueBD
                        )
                        {
                            long revDiff                = 0;
                            long wheel_periodDiff       = 0;
                            long torqueDiff             = 0;
                            long eventCountDiff         = 0;
                            long accumulatedWheelPeriod = accumulatedWheelPeriodBD.longValue();
                            long accumulatedWheelTorque = accumulatedWheelTorqueBD.longValue();
                            
                            if (wheelTorqueUpdateEventCount < lastCount) { // it has rolled over
                                eventCountDiff = 256 + wheelTorqueUpdateEventCount - lastCount;
                            } else {
                                eventCountDiff = wheelTorqueUpdateEventCount - lastCount;
                            }
                        
                            if (accumulatedWheelTicks < lastRevCount) { // it has rolled over
                                revDiff = 256 + accumulatedWheelTicks - lastRevCount;
                            } else {
                                revDiff = accumulatedWheelTicks - lastRevCount;
                            }
                            lastRevCount = accumulatedWheelTicks;
                            totalRevs    = revDiff + totalRevs;
                        
                            if (accumulatedWheelPeriod < lastPeriod) { // it has rolled over
                                wheel_periodDiff = 65536 + accumulatedWheelPeriod - lastPeriod;
                            } else {
                                wheel_periodDiff = accumulatedWheelPeriod - lastPeriod;
                            }
                            
                            if (accumulatedWheelTorque < lastTorque) { // it has rolled over
                              torqueDiff = 65536 + accumulatedWheelTorque - lastTorque;
                            } else {
                              torqueDiff = accumulatedWheelTorque - lastTorque;
                            }
                            lastTorque = accumulatedWheelTorque;
                            
                            if (wheel_periodDiff > 0 && eventCountDiff > 0) {
                                lastPeriod = wheel_period;
                                lastCount  = wheelTorqueUpdateEventCount;
                                wheelZeros = 0;
                            } else {
                                if (wheelZeros > 4) {
                                    lastPeriod = wheel_period;
                                    lastCount  = wheelTorqueUpdateEventCount;
                                    return;
                                }
                                wheelZeros++;
                                return;
                            }
                        
                            if (torqueDiff <= 0 && wheel_periodDiff <= 0) {
                                kph  = 0;
                                watt = 0;
                            } else if (wheel_periodDiff > 0) {
                                // kph        = 3600* 1000 * wcm * ecd / (1/2048)
                                // 7372800000 = (3600*1000) / 1/2048s 
                                // 2108mm     = wheel circumference of a 700c X 25mm tire
                                
                                kph  = (double) (((7372800000.0) * wheelCircumferenceInMeters.doubleValue() * (double) eventCountDiff) / ((double) wheel_periodDiff));
                                watt = (double) 128 * Math.PI * ((double) torqueDiff / (double) wheel_periodDiff);
                            }
                        
                            
                            
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("KM",    reduceNumberToString(totalRevs * wheelCircumferenceInMeters.doubleValue() * 1000.0));
                            map.put("KPH",   reduceNumberToString(kph));
                            map.put("WATTS", reduceNumberToString(watt));
                            map.put("NM",    reduceNumberToString(torqueDiff));
                            alterCurrentData(map);
                        }
                    }
                );
                
                /*
                result.subscribeRawCrankTorqueDataEvent(
                    new IRawCrankTorqueDataReceiver() {
                        @Override
                        public void onNewRawCrankTorqueData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long crankTorqueUpdateEventCount, final long accumulatedCrankTicks, final BigDecimal accumulatedCrankPeriod, final BigDecimal accumulatedCrankTorque)
                        {
                            alterCurrentData("NM", reduceNumberToString(accumulatedCrankTorque));
                        }
                    }
                );

                result.subscribeTorqueEffectivenessEvent(
                    new ITorqueEffectivenessReceiver() {
                        @Override
                        public void onNewTorqueEffectiveness(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long powerOnlyUpdateEventCount, final BigDecimal leftTorqueEffectiveness, final BigDecimal rightTorqueEffectiveness)
                        {                            
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("LTE", reduceNumberToString(leftTorqueEffectiveness));
                            map.put("RTE", reduceNumberToString(rightTorqueEffectiveness));
                            alterCurrentData(map);
                        }
    
                    }
                );

                result.subscribePedalSmoothnessEvent(new IPedalSmoothnessReceiver() {
                        @Override
                        public void onNewPedalSmoothness(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long powerOnlyUpdateEventCount, final boolean separatePedalSmoothnessSupport, final BigDecimal leftOrCombinedPedalSmoothness, final BigDecimal rightPedalSmoothness)
                        {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("SNPLC", reduceNumberToString(leftOrCombinedPedalSmoothness));
                            map.put("SNPR",  reduceNumberToString(rightPedalSmoothness));
                            alterCurrentData(map);
                        }
                    }
                );*/
            }
        }
    };
    
    
    @Override
    public void zeroReadings()
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put("WATTS", "0");
        map.put("NM",    "0");
        map.put("CAD",   "0");
        map.put("KPH",   "0");
        map.put("KM",    "0");
        alterCurrentData(map);
    }
}


