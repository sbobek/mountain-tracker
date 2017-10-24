/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2017 by RIOT (http://riot.agency)
 *
 */

package pl.wownow.main;

import java.util.LinkedList;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import pl.wownow.augmented.AugmentedActivity;
import pl.wownow.augmented.LocationNotFixedException;
import pl.wownow.datasource.DataBaseHelper;
import pl.wownow.datasource.Peak;
import pl.wownow.view.ArDisplayView;
import pl.wownow.view.AugmentedData;
import pl.wownow.view.RangeVisualisationView;
import pl.wownow.view.VertRangeSeekBar.OnRangeSeekBarChangeListener;
import pl.wownow.view.ScreenCoordinates;
import pl.wownow.view.VertRangeSeekBar;


public class LocationActivity extends AugmentedActivity{
	static final String STATE_MIN_RANGE = "minRange";
	static final String STATE_MAX_RANGE = "maxRange";
	
	private ArLocationContent arc;
	private LinkedList<Peak> peaks = new LinkedList<Peak>();
	private DataBaseHelper myDbHelper;
	
	private static double LOCATION_RADIUS = 0.5;
	private static final int KM2M = 1000;
	private static final int MAX_RANGE = 100;
	private static final int MIN_RANGE = 0;
	private int min = MIN_RANGE;
	private int max = MIN_RANGE+10;
	
	private static final int UI_HIDEOUT_DELAY = 5000;
	private long hideoutTimestamp ;
	
	VertRangeSeekBar<Integer> seekBar;
	RangeVisualisationView rangeView;
	
	
    /**
     * This sample demonstrates how to incorporate location based services in your app and
     * process location updates.  The app also shows how to convert lat/long coordinates to
     * human-readable addresses.
     */
    //@SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.augmented_layout);
        FrameLayout arViewPane = (FrameLayout) findViewById(R.id.ar_view_pane);
        
        ArDisplayView arDisplay = new ArDisplayView(getApplicationContext(), this);
        arViewPane.addView(arDisplay);

        arc = new ArLocationContent(getApplicationContext());
        arViewPane.addView(arc);
        
        // create HorizRangeSeekBar as Integer range between 20 and 75
        seekBar = new VertRangeSeekBar<Integer>(MIN_RANGE, MAX_RANGE, getApplicationContext());
        seekBar.setSelectedMinValue(min);
        seekBar.setSelectedMaxValue(max);
        seekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Integer>() {
                public void onRangeSeekBarValuesChanged(VertRangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                        // handle changed range values
                        min = minValue.intValue();
                        max = maxValue.intValue();
                        loadPeaks();
                }
        });

        // add HorizRangeSeekBar to pre-defined layout
        LinearLayout rangeBaView = (LinearLayout) findViewById(R.id.ar_seek_bar);
        rangeBaView.addView(seekBar);
        
        
        rangeView = new RangeVisualisationView(getApplicationContext());
        // add RangeVisualisation to pre-defined layout
        LinearLayout rangeVisualisationView = (LinearLayout) findViewById(R.id.range_visualisation);
        rangeVisualisationView.addView(rangeView);

        askForPermission(android.Manifest.permission.ACCESS_FINE_LOCATION,1);
        
        arc.invalidate();
        
    }

	private void askForPermission(String permission, Integer requestCode) {
		if (ContextCompat.checkSelfPermission(LocationActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(LocationActivity.this, permission)) {

				//This is called if user has denied the permission before
				//In this case I am just asking the permission again
				ActivityCompat.requestPermissions(LocationActivity.this, new String[]{permission}, requestCode);

			} else {

				ActivityCompat.requestPermissions(LocationActivity.this, new String[]{permission}, requestCode);
			}
		} else {
			Toast.makeText(this, "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
		}
	}
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	// TODO Auto-generated method stub
    	outState.putInt(STATE_MIN_RANGE, min);
    	outState.putInt(STATE_MAX_RANGE, max);

    	super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	// TODO Auto-generated method stub
    	super.onRestoreInstanceState(savedInstanceState);
    	min = savedInstanceState.getInt(STATE_MIN_RANGE);
        max = savedInstanceState.getInt(STATE_MAX_RANGE);
        seekBar.setSelectedMinValue(min);
        seekBar.setSelectedMaxValue(max);
    }
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
        myDbHelper = new DataBaseHelper(this);
        
        try { 
        	myDbHelper.createDataBase(); 
        	myDbHelper.openDataBase(); 
        }catch(Exception sqle){
        	sqle.printStackTrace();
        }
        loadPeaks();
        
        //Timer task to fade out UI
	    View seekBarView = findViewById(R.id.overlay_ui);
	    if(seekBarView.getVisibility() != View.VISIBLE) {
	    	seekBarView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
	    	seekBarView.setVisibility(View.VISIBLE);
	    	//Trigger timer task that fires fading out in 5 sec
	    } 
        hideoutTimestamp =  (System.currentTimeMillis());
    }
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	myDbHelper.close();
    }
    
    @Override
    protected void onStop() {
    	// TODO Auto-generated method stub
    	super.onStop();
    	myDbHelper.close();
    }
    
    
   @Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
	    View seekBarView = findViewById(R.id.overlay_ui);
	    if(seekBarView.getVisibility() != View.VISIBLE) {
	    	seekBarView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
	    	seekBarView.setVisibility(View.VISIBLE);
	    	//Trigger timer task that fires fading out in 5 sec
	    }  
	    hideoutTimestamp =  (System.currentTimeMillis());
		return super.onTouchEvent(event);
	}
    
    // Method to launch Settings
    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }
    
    /**
     * Load all peaks in a radius of 1 degree (just to be safe - it's about 112km)
     */
    private void loadPeaks(){
    	try{
	    	Location current = getCurrentLocation();
	    	LinkedList<Peak> temp = myDbHelper.getPeaks(current, LOCATION_RADIUS);
	    	//filter and leave only these that fits current range limit
	    	peaks.clear();
	    	for(Peak p: temp){
	    		double distance =p.getDistanceTo(current); 
	    		if(distance >= min*KM2M && distance <= max*KM2M){
	    			//Add to peaks;
	    			peaks.add(p);
	    		}
	    	}
    	}catch(LocationNotFixedException lnfe){

    	}
    
    }

    
    @Override
    public  void onLocationChanged(Location location) {
    	// TODO Auto-generated method stub
    	super.onLocationChanged(location);

    	loadPeaks();
    	//UPDATE PEAKS
    }
    
 
    protected void onProviderNotAvailable(final String provider) {
    	// TODO Auto-generated method stub
    	if(provider == LocationManager.GPS_PROVIDER)
			Toast.makeText(this, R.string.gps_provider_disabled, Toast.LENGTH_LONG).show();
		else if(provider == LocationManager.NETWORK_PROVIDER)
			Toast.makeText(this, R.string.network_provider_disabled, Toast.LENGTH_LONG).show();
    	
    }

	@Override
	protected void onGPSDisabled() {
		// TODO Auto-generated method stub
		FragmentManager fm = getSupportFragmentManager();
        Fragment dialogFragment = fm.findFragmentByTag("enableGpsDialog"); 
        if(dialogFragment != null){
        	FragmentTransaction ft = fm.beginTransaction();
            ft.remove(dialogFragment);
            ft.commitAllowingStateLoss();
        }
		EnableGpsDialogFragment gpsDialog= new EnableGpsDialogFragment();
		gpsDialog.setListener( new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                enableLocationSettings();
            }
        });
		gpsDialog.show(getSupportFragmentManager(), "enableGpsDialog");
	}
	

	
	
	

	@Override
	public void onStateChanged() {	
		
		LinkedList<AugmentedData> adl = new LinkedList<AugmentedData>();
		try{
			// DEBUG INFO
			String[] info = {"Azimuth: "+getAzimuth(),"Pitch: "+getPitch(),"Roll: "+getRoll(),
					"Altitude: "+getAltitude(),
					"Wifi: "+isWifiConnected(),"Mobile: "+isMobileConnected(),"GPS On: "+isGPSProviderOn(),"Network Provider: "+isNetworkProviderOn(),
					"Sensor Accuracy: "+getSensorAccuracy(),"Location Accuracy: "+getCurrentLocation().getAccuracy(),"Location source: "+getCurrentLocation().getProvider()};
			arc.setDebugInformation(info);
			// DEBUG INFO
			for(Peak p: peaks){
				int screenHeight  = arc.getViewHeight(); 
		    	int screenWidth = arc.getViewWidth();
		    	ScreenCoordinates sc =getAugmentationCoordinates(p.getLocation(), screenWidth, screenHeight);
		    	AugmentedData ad = new AugmentedData(p, sc);
		    	adl.add(ad);
			}
			arc.setDataToDraw(adl);
		}catch(LocationNotFixedException lnfe){
		}
		
		if((System.currentTimeMillis()) - hideoutTimestamp  > UI_HIDEOUT_DELAY){
		    View seekBarView = findViewById(R.id.overlay_ui);
		    if(seekBarView.getVisibility() == View.VISIBLE) {
		    	seekBarView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
		    	seekBarView.setVisibility(View.INVISIBLE);
		    	//Trigger timer task that fires fading out in 5 sec
		    }  
		}
		
	}
	
	
}

