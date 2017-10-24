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

/**
 * 
 */
package pl.wownow.augmented;

import java.util.Timer;
import java.util.TimerTask;

import pl.wownow.networking.*;
import pl.wownow.view.ScreenCoordinates;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

/**
 * @author sbk
 *
 */
public abstract class AugmentedActivity extends FragmentActivity implements LocationListener,SensorEventListener {
	private static final int ONE_SECOND = 1000;
	private static final int TEN_METERS = 10;
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	public static final float OMEGA_MAGNITUDE_EPSILON = 0.0001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    public static  float FILTER_COEFFICIENT = 0.5f;
    public static  float ACCMAG_SMOOTHING_COEFFICIENT = 0.25f;
	public static  int REFRESH_INTERVAL = 25;
	public static  int REFRESH_CONNECTIONS_INTERVAL = 1000;
    public static final String API_KEY = "YOUR_OPENMAPQUESTAPI_KEY";
	private static final String OPENMAPQUESTAPI = "http://open.mapquestapi.com/elevation/v1/profile?key="+API_KEY+"&outFormat=xml&latLngCollection=";
	
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private Sensor mSensorAcc;
    private Sensor mSensorMgn;
    private Sensor mSensorGrv;
    private Sensor mSensorGyro;
    
    private float gyroTimestamp;
    private Timer fuseTimer;
    private boolean initState = true;
    
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    // GPS On
    private static boolean gpsProviderOn = false;
    // Network Provider On
    private static boolean networkProviderOn = false;
   

	// senso Accuracy
    private static int sensorAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    
    
    
    private Location currentLocation;
    
    float cameraHorizFOV;
    float cameraVertFOV;
    
   
    private float[] remapedFusedOrientation;
    private float[] fusedOrientation;
	private float[] mRotationM = new float[9];     
	private float[] cameraRotation = new float[9];	
	private float[] gyroOrientation = new float[3];
	private float[] accMagOrientation;// = new float[3];
    private float mGravs[] = new float[3];
    private float mGeoMags[] = new float[3];
    private float[] gyroMatrix =  {1.0f, 0.0f, 0.0f, 0.0f, 1.0f,0.0f, 0.0f, 0.0f,1.0f};
    private boolean actualStateChanged;
	
	
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorMgn = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorGrv = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	    fusedOrientation = new float[3];
	    remapedFusedOrientation = new float[3];
	    actualStateChanged = true;
	    
	    
        Camera camera = Camera.open();
		Camera.Parameters params = camera.getParameters();
		cameraVertFOV = params.getVerticalViewAngle();
		cameraHorizFOV = params.getHorizontalViewAngle();
		camera.release();

	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		 Location gpsLocation = null;
         Location networkLocation = null;
         mLocationManager.removeUpdates(this);


         if(mSensorGrv == null){
    		 mSensorManager.registerListener(this, mSensorAcc, SensorManager.SENSOR_DELAY_NORMAL);
    	 }else{
    		 mSensorManager.registerListener(this, mSensorGrv, SensorManager.SENSOR_DELAY_NORMAL);
    	 }
    	 mSensorManager.registerListener(this, mSensorMgn, SensorManager.SENSOR_DELAY_NORMAL);
         
         mSensorManager.registerListener(this, mSensorGyro, SensorManager.SENSOR_DELAY_NORMAL);

     	// Request updates from both fine (gps) and coarse (network) providers.
         gpsLocation = requestUpdatesFromProvider(
                 LocationManager.GPS_PROVIDER);
         networkLocation = requestUpdatesFromProvider(
                 LocationManager.NETWORK_PROVIDER);

         // If both providers return last known locations, compare the two and use the better
         // one to update the UI.  If only one provider returns a location, use it.
         if (gpsLocation != null && networkLocation != null) {
        	 updateCoordinates(getBetterLocation(gpsLocation, networkLocation));
             onStateChanged();
         } else if (gpsLocation != null) {
        	 updateCoordinates(gpsLocation);
        	 onStateChanged();
         } else if (networkLocation != null) {
        	 updateCoordinates(networkLocation);
        	 onStateChanged();
         }
	}
	
	
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
    	
		// Check if the GPS setting is currently enabled on the device.
        // This verification should be done during onStart() because the system calls this method
        // when the user returns to the activity, which ensures the desired location provider is
        // enabled each time the activity resumes from the stopped state.
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsProviderOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkProviderOn = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsProviderOn) {
        	//Raise exception
        	onGPSDisabled();
        }
        
		updateConnectedFlags();
		fuseTimer = new Timer();
	    fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                1000, REFRESH_INTERVAL);
	    fuseTimer.scheduleAtFixedRate(new updateConnectionFlagsTask(),
	    		REFRESH_CONNECTIONS_INTERVAL, REFRESH_CONNECTIONS_INTERVAL);
    	
	}
	
	protected abstract void onProviderNotAvailable(final String provider);
	protected abstract void onGPSDisabled();
	public abstract void onStateChanged();
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mLocationManager.removeUpdates(this);
    	mSensorManager.unregisterListener(this);
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		mLocationManager.removeUpdates(this);
    	mSensorManager.unregisterListener(this);
    	fuseTimer.cancel();
    	fuseTimer.purge();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		
	}
	
    public ScreenCoordinates getAugmentationCoordinates(Location target, int screenWidth, int screenHeight)
    		throws LocationNotFixedException{ 
    	Location cl = getCurrentLocation();
    	float curBearingTo = cl.bearingTo(target);
    	float targetDistance = cl.distanceTo(target);
    	float tanRad =(float)(getAltitude()-target.getAltitude())/targetDistance;
    	float heightBearingTo = (float) Math.toDegrees(Math.atan(tanRad));
    	float azimuth = getAzimuth();
    	//FIXME issue #13
    	if(azimuth < 0 && curBearingTo > 0){
    		azimuth += 360;
    	}else if(curBearingTo < 0 && azimuth > 0){
    		curBearingTo += 360;
    	}
    	
    	float azimuthDifferenceDegrees = (azimuth-curBearingTo);
    	
    	if(azimuthDifferenceDegrees > 180){
    		azimuthDifferenceDegrees -= 360;
    	}
    	
    	synchronized(gyroOrientation){
	    	float x = screenWidth/2.f - Math.round((float) ( (screenWidth / getCameraVertFOV()) *azimuthDifferenceDegrees));
			float y = screenHeight/2.f - Math.round((float) ( (screenHeight / getCameraHorizFOV()) * (getPitch()-heightBearingTo))) ;
			return new ScreenCoordinates(x,y);
     	}
    }
    
	


	// Checks the network connection and sets the wifiConnected and mobileConnected
    // variables accordingly.
    private void updateConnectedFlags() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            wifiConnected = false;
            mobileConnected = false;
        }
    }
    
    /**
     * Method to register location updates with a desired location provider.  If the requested
     * provider is not available on the device, the app displays a Toast with a message referenced
     * by a resource id.
     *
     * @param provider Name of the requested provider.
     * @return A previously returned {@link android.location.Location} from the requested provider,
     *         if exists.
     */
    private Location requestUpdatesFromProvider(final String provider) {
        Location location = null;
        mLocationManager.requestLocationUpdates(provider, ONE_SECOND, TEN_METERS, this);
        if (mLocationManager.isProviderEnabled(provider)) {
            location = mLocationManager.getLastKnownLocation(provider);
        } else{
        	onProviderNotAvailable(provider);
        }
        return location;
    }
    
    private void updateCoordinates(Location location){
    	setCurrentLocation(location);
        if((wifiConnected || mobileConnected) && location.getAltitude() == 0){
        	//openmapquest query if network is available
        	//DONT if the GPS Location delta is small
        	String urlString  = OPENMAPQUESTAPI +location.getLatitude()+","+location.getLongitude();
        	try{
        		new LoadAltitudeFromNetwork().updateAltitude(urlString, this);
        	}catch(Exception e){
        		e.printStackTrace();
            }
        }
    }
    
    
    /** Determines whether one Location reading is better than the current Location fix.
     * Code taken from
     * http://developer.android.com/guide/topics/location/obtaining-user-location.html
     *
     * @param newLocation  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new
     *        one
     * @return The better Location object based on recency and accuracy.
     */
   protected Location getBetterLocation(Location newLocation, Location currentBestLocation) {
       if (currentBestLocation == null) {
           // A new location is always better than no location
           return newLocation;
       }

       // Check whether the new location fix is newer or older
       long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
       boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
       boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
       boolean isNewer = timeDelta > 0;

       // If it's been more than two minutes since the current location, use the new location
       // because the user has likely moved.
       if (isSignificantlyNewer) {
           return newLocation;
       // If the new location is more than two minutes older, it must be worse
       } else if (isSignificantlyOlder) {
           return currentBestLocation;
       }

       // Check whether the new location fix is more or less accurate
       int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation.getAccuracy());
       boolean isLessAccurate = accuracyDelta > 0;
       boolean isMoreAccurate = accuracyDelta < 0;
       boolean isSignificantlyLessAccurate = accuracyDelta > 200;

       // Check if the old and new location are from the same provider
       boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
               currentBestLocation.getProvider());

       // Determine location quality using a combination of timeliness and accuracy
       if (isMoreAccurate) {
           return newLocation;
       } else if (isNewer && !isLessAccurate) {
           return newLocation;
       } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
           return newLocation;
       }
       return currentBestLocation;
   }

   /** Checks whether two providers are the same */
   private boolean isSameProvider(String provider1, String provider2) {
       if (provider1 == null) {
         return provider2 == null;
       }
       return provider1.equals(provider2);
   }
   
   
    public void onLocationChanged(Location location) {
        // A new location update is received.  Do something useful with it.  Update the UI with
        // the location update.
    	try{
    		updateCoordinates(getBetterLocation(location,getCurrentLocation()));
    		onStateChanged();
    	}catch(LocationNotFixedException lnf){}
    }

    public void onProviderDisabled(String provider) {
    	if(provider.equals(LocationManager.GPS_PROVIDER))
    		gpsProviderOn = false;
    	else if(provider.equals(LocationManager.NETWORK_PROVIDER))
    		networkProviderOn = false;
    }

    public void onProviderEnabled(String provider) {
    	if(provider.equals(LocationManager.GPS_PROVIDER)){
    		gpsProviderOn = true;
    	}
    	else if(provider.equals(LocationManager.NETWORK_PROVIDER))
    		networkProviderOn = true;
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    
    }

    
    public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub  
		sensorAccuracy = arg1;
	}

	public void onSensorChanged(SensorEvent event) {
		synchronized(gyroOrientation){
			switch (event.sensor.getType()) {
				case Sensor.TYPE_GRAVITY:
					//System.arraycopy(event.values, 0, mGravs, 0, 3);
					mGravs[0]+=(event.values[0] - mGravs[0])*ACCMAG_SMOOTHING_COEFFICIENT;//(mGravs[0]*2+event.values[0])*0.33334f;
					mGravs[1]+=(event.values[1] - mGravs[1])*ACCMAG_SMOOTHING_COEFFICIENT;//(mGravs[1]*2+event.values[1])*0.33334f;
					mGravs[2]+=(event.values[2] - mGravs[2])*ACCMAG_SMOOTHING_COEFFICIENT;//(mGravs[2]*2+event.values[2])*0.33334f;
					break;
	            case Sensor.TYPE_ACCELEROMETER:
	            	//if(gravity) break; // We have better readings from gravity sensor
	            	//System.arraycopy(event.values, 0, mGravs, 0, 3);
	            	mGravs[0]+=(event.values[0] - mGravs[0])*ACCMAG_SMOOTHING_COEFFICIENT;//(mGravs[0]*2+event.values[0])*0.33334f;
	            	mGravs[1]+=(event.values[1] - mGravs[1])*ACCMAG_SMOOTHING_COEFFICIENT;//(mGravs[1]*2+event.values[1])*0.33334f;
	            	mGravs[2]+=(event.values[2] - mGravs[2])*ACCMAG_SMOOTHING_COEFFICIENT;//(mGravs[2]*2+event.values[2])*0.33334f;
	            	break;
	            case Sensor.TYPE_MAGNETIC_FIELD:
	                //System.arraycopy(event.values, 0, mGeoMags, 0, 3);
	            	mGeoMags[0]+=(event.values[0] - mGeoMags[0])*ACCMAG_SMOOTHING_COEFFICIENT;//(mGeoMags[0]*1+event.values[0])*0.5f;
	            	mGeoMags[1]+=(event.values[1] - mGeoMags[1])*ACCMAG_SMOOTHING_COEFFICIENT;//(mGeoMags[1]*1+event.values[1])*0.5f;
	            	mGeoMags[2]+=(event.values[2] - mGeoMags[2])*ACCMAG_SMOOTHING_COEFFICIENT;//(mGeoMags[2]*1+event.values[2])*0.5f;
	                break;
	            case Sensor.TYPE_GYROSCOPE:
	            	 // copy the new gyro values into the gyro array
	                // convert the raw gyro data into a rotation vector
	            	if (accMagOrientation == null)
	                    return;

	                // initialisation of the gyroscope based rotation matrix
	                if(initState) {
	                    float[] initMatrix = new float[9];
	                    initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
	                    float[] test = new float[3];
	                    SensorManager.getOrientation(initMatrix, test);
	                    gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
	                    initState=false;
	                }
	                
	                float[] deltaVector = new float[4];
	                float[] gyro = new float[3];
	                if(gyroTimestamp != 0) {
	                    final float dT = (event.timestamp - gyroTimestamp) * NS2S;
	                    //Since we remapCoordinates, the z and y axis are swaped we need to swap the gyro readings
	                    System.arraycopy(event.values, 0, gyro, 0, 3);
	                    getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
	                }
	             
	                // measurement done, save current time for next interval
	                gyroTimestamp = event.timestamp;
	             
	                // convert rotation vector into rotation matrix
	                float[] deltaMatrix = new float[9];
	                SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
	                // apply the new rotation interval on the gyroscope based rotation matrix
	                gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);
	                SensorManager.getOrientation(gyroMatrix, gyroOrientation);
	                
	            	break;
	            default:
	                return;
            } 
			calculateAccMagOrientatoin();
			if(actualStateChanged){
				onStateChanged();
				actualStateChanged = false;
			}
		}
	}
	
    
    
    
    
    private void calculateAccMagOrientatoin(){
	    if (SensorManager.getRotationMatrix(mRotationM, null, mGravs, mGeoMags)){
	    		if(accMagOrientation == null) accMagOrientation = new float[3]; 
	    		
				SensorManager.getOrientation(mRotationM, accMagOrientation);
			}
    }
    
    class updateConnectionFlagsTask extends TimerTask {
    	@Override
    	public void run() {
    		// TODO Auto-generated method stub
    		updateConnectedFlags();
    	}
    }
    
    class calculateFusedOrientationTask extends TimerTask {
        public void run() { 
			synchronized(gyroOrientation){
				if(accMagOrientation != null){
					float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
		            /*
		             * Fix for 179ďż˝ <--> -179ďż˝ transition problem:
		             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
		             * If so, add 360ďż˝ (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360ďż˝ from the result
		             * if it is greater than 180ďż˝. This stabilizes the output in positive-to-negative-transition cases.
		             */  
		            // azimuth
		            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
		            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
		        		fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
		            }
		            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
		            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
		            	fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
		            }
		            else {
		            	fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
		            }
		            
		            // pitch
		            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
		            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
		        		fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
		            }
		            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
		            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
		            	fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
		            }
		            else {
		            	fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
		            }
		            
		            // roll
		            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
		            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
		        		fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
		            }
		            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
		            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
		            	fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
		            }
		            else {
		            	fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
		            }
					
					
		            // overwrite gyro matrix and orientation with fused orientation
		            // to comensate gyro drift          

		            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
		            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
		            //Remaping
		            float [] flatRotation = getRotationMatrixFromOrientation(fusedOrientation);
		            // get orientation
		            
		            Display display = getWindowManager().getDefaultDisplay(); 
	                int deviceRot = display.getRotation();

	                //Following code opens issue 12
	                switch (deviceRot)
	                {
		                // portrait - normal
		                case Surface.ROTATION_0: SensorManager.remapCoordinateSystem(flatRotation,
		                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
		                        cameraRotation);
		                break;
		                // rotated left (landscape - keys to bottom)
		                case Surface.ROTATION_90: SensorManager.remapCoordinateSystem(flatRotation,
		                        SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X,
		                        cameraRotation); 
		                break;
		                // upside down
		                case Surface.ROTATION_180: SensorManager.remapCoordinateSystem(flatRotation,
		                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
		                        cameraRotation); 
		                break;
		                // rotated right
		                case Surface.ROTATION_270: SensorManager.remapCoordinateSystem(flatRotation,
		                        SensorManager.AXIS_MINUS_Z, SensorManager.AXIS_X,
		                        cameraRotation); 
		                break;
	
		                default:  break;
	                }
		            
			        //SensorManager.remapCoordinateSystem(flatRotation,
			    	//			SensorManager.AXIS_X, SensorManager.AXIS_Z,
			    	//			cameraRotation);
	                
		    		SensorManager.getOrientation(cameraRotation, remapedFusedOrientation);
		    		//UpdateUI
		    		actualStateChanged = true;
				}
			}
        }

        
    };
    
    
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];
     
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
     
        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
     
        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
     
        return result;
    }
    
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];
   
        float sinX = android.util.FloatMath.sin(o[1]);
        float cosX = android.util.FloatMath.cos(o[1]);
        float sinY = android.util.FloatMath.sin(o[2]);
        float cosY = android.util.FloatMath.cos(o[2]);
        float sinZ = android.util.FloatMath.sin(o[0]);
        float cosZ = android.util.FloatMath.cos(o[0]);
     
        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
     
        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
     
        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
     
        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }
    
    // This function is borrowed from the Android reference
	// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	// It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues,
            float[] deltaRotationVector,
            float timeFactor)
	{
		float[] normValues = new float[3];
		
		// Calculate the angular speed of the sample
		float omegaMagnitude = android.util.FloatMath.sqrt(gyroValues[0] * gyroValues[0] +
		gyroValues[1] * gyroValues[1] +
		gyroValues[2] * gyroValues[2]);
		
		// Normalize the rotation vector if it's big enough to get the axis
		if(omegaMagnitude > OMEGA_MAGNITUDE_EPSILON) {
		normValues[0] = gyroValues[0] / omegaMagnitude;
		normValues[1] = gyroValues[1] / omegaMagnitude;
		normValues[2] = gyroValues[2] / omegaMagnitude;
		}
		
		// Integrate around this axis with the angular speed by the timestep
		// in order to get a delta rotation from this sample over the timestep
		// We will convert this axis-angle representation of the delta rotation
		// into a quaternion before turning it into the rotation matrix.
		float thetaOverTwo = omegaMagnitude * timeFactor;
		float sinThetaOverTwo = android.util.FloatMath.sin(thetaOverTwo);
		float cosThetaOverTwo = android.util.FloatMath.cos(thetaOverTwo);
		deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
		deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
		deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
		deltaRotationVector[3] = cosThetaOverTwo;
	}
    

 	public synchronized double getAltitude() throws LocationNotFixedException{
 		return getCurrentLocation().getAltitude();
 	}


 	public synchronized void setAltitude(double altitude) throws LocationNotFixedException {
 		getCurrentLocation().setAltitude(altitude);
 	}

 	public boolean isGPSProviderOn(){
 		return gpsProviderOn;
 	}
 	
 	public boolean isNetworkProviderOn(){
 		return networkProviderOn;
 	}
 	
 	public float[] getOrientation(){
 		synchronized(gyroOrientation){
 			return remapedFusedOrientation;
 		}
 	}
 	
 	
 	public float getAzimuth(){
 		synchronized(gyroOrientation){
 			return (float)Math.toDegrees(remapedFusedOrientation[0]);
 		}
 		
 	}
 	
 	public float getPitch(){
 		synchronized(gyroOrientation){
 			return (float)Math.toDegrees(remapedFusedOrientation[1]);
 		}
 	}
 	
 	public float getRoll(){
 		synchronized(gyroOrientation){
 			return (float)Math.toDegrees(remapedFusedOrientation[2]);
 		}
 	}

	public synchronized Location getCurrentLocation() throws LocationNotFixedException{
		if(currentLocation == null) throw new LocationNotFixedException();
		return currentLocation;
	}
	
	public synchronized void setCurrentLocation(Location currentLocation) {
		this.currentLocation = currentLocation;
	}
	
	public static int getSensorAccuracy() {
		return sensorAccuracy;
	}
	
	public  boolean isMobileConnected() {
		return mobileConnected;
	}
	
	public  boolean isWifiConnected() {
			return wifiConnected;
	}

	protected double getCameraHorizFOV() {
		int rotation = getWindowManager().getDefaultDisplay()
				.getRotation(); 
		if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180){
			return cameraHorizFOV;
		}else{
			return cameraVertFOV;
		}

	}

	protected double getCameraVertFOV() {
		int rotation = getWindowManager().getDefaultDisplay()
				.getRotation(); 
		if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180){
			return cameraVertFOV;
		}else{
			return cameraHorizFOV;
		}
	}
 
 	

}
