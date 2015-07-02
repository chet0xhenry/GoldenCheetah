package com.ridelogger;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class StartActivity extends FragmentActivity
{                
    private MenuItem startMenu;
    private MenuItem stopMenu;
    private SQLiteDatabase db;
    
    private ServiceConnection mConnection;
    private static CurrentValuesAdapter currentValuesAdapter;

    private List<Integer> activity_ids;
    private List<String> activities;


    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.start_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        if(item.getItemId() == R.id.Settings) {
            setupSettings();
        } else if(item.getItemId() == R.id.Start) {
            startRide();
        } else {
            stopRide();
        }
        
        return true;
    }

    
    /**
     * start up our class
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {    
        super.onCreate(savedInstanceState);
        Db dbHelper = new Db(this);

        db = dbHelper.getReadableDatabase();

        activities = new ArrayList<String>();
        activity_ids = new ArrayList<Integer>();

        refreshRideList();
    }
    
    
    @Override
    protected void onResume() {
        super.onResume();
        bindToService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unBindToService();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        unBindToService();
    }


    private void refreshRideList(){
        Cursor c = db.rawQuery("SELECT id, name, start_ts FROM Activity ORDER BY start_ts DESC", null);
        activities.clear();
        activity_ids.clear();

        if (c != null ) {
            if  (c.moveToFirst()) {
                do {
                    activity_ids.add(c.getInt(0));
                    String name = c.getString(1);
                    String start_ts = c.getString(2);
                    activities.add(name + " - " + start_ts);
                }while (c.moveToNext());
            }
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                activities );

        setContentView(R.layout.activity_list);
        ListView layout = (ListView) findViewById(R.id.listView);

        layout.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int aid = activity_ids.get(position);
            }

        });

        layout.setAdapter(arrayAdapter);

        getActionBar().setTitle("Ride Logger - Rides");
    }
    
    /**
     * setup the settings for the user
     */
    private void setupSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    
    /**
     * stop ride and clean up references 
     */
    private void stopRide() {
        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.stopping_ride), Toast.LENGTH_LONG);
        toast.show();
        
        Intent rsi = new Intent(this, RideService.class);
        this.stopService(rsi);
        refreshRideList();
    }
    
    private static final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg_internal) {
            updateValues(msg_internal.getData());
        }
    };
    
    
    /**
     * tell the service to start sending us messages with current values
     */
    private void bindToService() {
        RunningServiceInfo service = getServiceRunning(RideService.class);
        
        if(service != null) {
            mConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName className, IBinder service) {
                    Messenger mService = new Messenger(service);
                    try {
                        Message msg = Message.obtain();
                        msg.replyTo = new Messenger(mHandler);
                        
                        mService.send(msg);
                    } catch (RemoteException e) {
                        // In this case the service has crashed before we could even do anything with it
                    }
                }

                public void onServiceDisconnected(ComponentName className) {}
            };
            
            bindService(new Intent(StartActivity.this, RideService.class), mConnection, Context.BIND_AUTO_CREATE);

            setContentView(R.layout.activity_dashboard);
            GridView layout = (GridView) findViewById(R.id.LayoutData);
            currentValuesAdapter = new CurrentValuesAdapter(this, layout);
            layout.setAdapter(currentValuesAdapter);
        } else {
            refreshRideList();
        }
    }
    
    
    /**
     * tell the service to stop sending us messages with current values
     */
    private void unBindToService() {
        if (mConnection != null) {
            // Detach our existing connection.
            unbindService(mConnection);
            mConnection = null;
        }
    }
    
    
    /**
     * update the text fields with current values
     * @param bundle 
     */
    private static void updateValues(Bundle bundle) {
        currentValuesAdapter.update((float[]) bundle.getSerializable("currentValues"));       
    }
    
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        RunningServiceInfo service = getServiceRunning(RideService.class);
        startMenu = menu.findItem(R.id.Start);
        stopMenu  = menu.findItem(R.id.Stop);
        if(service == null) {
            startMenu.setVisible(true);
            stopMenu.setVisible(false);
        } else {
            startMenu.setVisible(false);
            stopMenu.setVisible(true);
        }
        return true;
    }
    

    /**
     * start the ride and notify the user of success
     */
    private void startRide() {
        Intent rsi = new Intent(this, RideService.class);
        this.startService(rsi);
        
        startMenu.setVisible(false);
        stopMenu.setVisible(true);
        
        bindToService();
        
        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.starting_ride), Toast.LENGTH_LONG);
        toast.show();

        getActionBar().setTitle("Ride Logger - Riding");
    }
    
    
    /**
     * is a service running or not
     * @param serviceClass
     * @return
     */
    private RunningServiceInfo getServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return service;
            }
        }
        return null;
    }
}
