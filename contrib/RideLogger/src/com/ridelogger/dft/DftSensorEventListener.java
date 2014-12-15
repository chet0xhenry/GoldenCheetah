package com.ridelogger.dft;
import android.hardware.SensorEventListener;


public abstract class DftSensorEventListener<T> implements SensorEventListener{
    protected int       n       = 256; // window size
    protected int       levels;        // Equal to floor(log2(n))
    protected int       dims    = 3;   // Dimensions;         
    protected float[]   cosTable;
    protected int[]     jt;
    
    public    float[][] msss;          // values in window
    public    float[][] MSSS;          // DFT of mss(x,y,z)
    public    float[]   MAXS;          // max values of DFT of mss(x,y,z)    
    
    public DftSensorEventListener() {
        initTables();
    }
    
    
    public DftSensorEventListener(int dims) {
        this.dims = dims;
        initTables();
    }
    
    
    public DftSensorEventListener(int dims, int window_size) {
        this.dims = dims;
        this.n    = window_size;
        initTables();
    }
    
    protected void initTables() {
        levels   = 31 - Integer.numberOfLeadingZeros(n);
        msss     = new float[dims][n];                    // values in window
        MSSS     = new float[dims][n];                    // DFT of mss(x,y,z)
        MAXS     = new float[dims];                       // max values of DFT of mss(x,y,z)
        cosTable = new float[n / 2];
        jt       = new int[n];     
        
        for (int i = 0; i < n / 2; i++) {
            cosTable[i] = (float) Math.cos(2 * (float) Math.PI * i / n);
        }
        
        for (int i = 0; i < n; i++) {
            jt[i] = Integer.reverse(i) >>> (32 - levels);
        }
    }
    
    protected void shiftInsert(float[] values) {
        for (int i = 0; i < n - 1; i++) {
            for(int j = 0; j < dims; j++) {
                msss[j][i] = msss[j][i + 1];
            }
        }
        
        for(int j = 0; j < dims; j++) {
            msss[j][n - 1] = values[j];
            MAXS[j] = (float) 0.0;
        }
    }
    
    protected void transAndShiftformRadix2() {
        for(int dim = 0; dim < dims; dim++) {
            // Bit-reversed addressing permutation
            for (int i = 0; i < n; i++) {
                if (jt[i] > i) {
                    MSSS[dim][i] = msss[dim][jt[i]];
                    MSSS[dim][jt[i]] = msss[dim][i];
                }
            }
            
            // Cooley-Tukey decimation-in-time radix-2 FFT
            for (int s = 2; s <= n; s *= 2) {
                int hs  = s / 2;
                int ts = n / s;
                for (int i = 0; i < n; i += s) {
                    for (int j = i, k = 0; j < i + hs; j++, k += ts) {
                        float tp =  MSSS[dim][j+hs] * cosTable[k];
                        
                        MSSS[dim][j + hs] = MSSS[dim][j] - tp;
                        
                        if(MAXS[dim] < MSSS[dim][j + hs]) {
                            MAXS[dim] = MSSS[dim][j + hs];
                        }
                        
                        MSSS[dim][j] += tp;
                        
                        if(MAXS[dim] < MSSS[dim][j]) {
                            MAXS[dim] = MSSS[dim][j];
                        }
                    }
                }
                if (s == n)  // Prevent overflow in 's *= 2'
                    break;
            }
        }
    }
    
    protected float averageDims() {
        float sum = (float) 0.0;
        for(int j = 0; j < dims; j++) {
            sum = sum + MAXS[j];
        }
        
        return sum / dims;
    }
}
