package com.asymptotik.rnd.location.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class LocationService extends Service implements LocationListener {

	//
	// The max error a location can have before it is sent to the BroadcastReceiver.
	//
	public static final double LOCATION_ACCURACY_THREASHOLD = 20.0;
	//
	// The min distance a location must be from the prior location before it is
	// sent to the BroadcastReceiver.
	//
	public static final double LOCATION_DISTANCE_THREASHOLD = 3.0;
	
	private long _count;
	private long _timeLastLocationUpdate;
	private long _timeSinceStarted;
	
	private Location _lastValidLocation;
	
	//
	// We use a timer to update the statistic for Last Updated. We want to update this statistic even
	// if the LocationManager does not provide an update. This is for the sample application only.
	//
	private Timer _timer = new Timer();
	//
	// Defines a custom Intent action
	//
    public static final String BROADCAST_ACTION = "com.asymptotik.rnd.location.activity.BROADCAST";
    //
    // Defines the key for the status "extra" in an Intent that is sent to the LocationBroadcastListener
    //
    // The location as received from the LocationManager
    //
    public static final String EXTENDED_DATA_LOCATION = "com.asymptotik.rnd.location.activity.LOCATION";
    // The number of calls to the LocationListener made by the LocationManager. Only used for stats.
    public static final String EXTENDED_DATA_COUNT = "com.asymptotik.rnd.location.activity.COUNT";
    // The stats sent for every tick of the _timer. Only used for stats.
    public static final String EXTENDED_DATA_STATS = "com.asymptotik.rnd.location.activity.STATS";
    //
    // Defines the key for the status "extra" in an Intent that is send to this service via startService(Intent)
    //
    public static final String SERVICE_DATA_RESET = "com.asymptotik.rnd.location.activity.RESET";
    public static final String SERVICE_DATA_CLOSEST_LOCATION = "com.asymptotik.rnd.location.activity.CLOSEST_LOCATION";
    
    // The location manager.
	private LocationManager _locationManager;
	// The service looper on which we will receive Location updates on. We want to receive updates on
	// a background thread and as such this Looper is created on it's own thread.
	private Looper _serviceLooper;
	//
	// Only used as a handler for the _timer. Since the timer callback uses the data that is
	// also updated by the LocationListener, we make the timer callback run on the same 
	// thread as the LocationListener to prevent any concurrency issues. Only needed for statistics.
	//
	private Handler _serviceHandler;
	
	//
	// A flag to indicate this service has been initialized.
	//
	private boolean _isInitialized;
	
	//
	// the LocationListener onStatusChanged callback seems to be called even if the status has not
	// changed. This map allows us to save the last status and really tell if it's changed.
	// This is for informational purposes only.
	//
	private HashMap<String, Integer> _locationProviderStatus = new HashMap<String, Integer>();
	
	public LocationService() {
		super();
	}

	@Override
	public void onCreate() {

		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		_serviceLooper = thread.getLooper();
		_serviceHandler = new Handler(_serviceLooper);
		_locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	}

	//
	// This is called every time startService(Intent) is called. It is called whether or not this
	// service is already running. Thus, it also provides a way for an Activity or another service
	// to send messages to this service. Message are sent in the intent extra data.
	//
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if(!_isInitialized) {
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
			_locationManager.requestLocationUpdates(30 * 1000l, 5.0f, criteria, this, _serviceLooper);
			_timeSinceStarted = SystemClock.elapsedRealtime();
			
			Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

			//
			// Schedule the timer to run ever seconds to update our statistics. This should not be used
			// in the real real app and is only for our statistics display.
			//
			_timer.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					_serviceHandler.post(new Runnable() {

						@Override
						public void run() {
							
							LocationStats stats = new LocationStats();
							stats.setCount(_count);
							stats.setElapsedTimeSinceUpdated(_timeLastLocationUpdate == 0 ? 0 : SystemClock.elapsedRealtime() - _timeLastLocationUpdate);
							stats.setElapsedTimeSinceStarted(SystemClock.elapsedRealtime() - _timeSinceStarted);
							
							Intent localIntent =
					                new Intent(BROADCAST_ACTION)
					                // Puts the status into the Intent
					                .putExtra(EXTENDED_DATA_STATS, stats);
					        
					        // Broadcasts the Intent to receivers in this app
					        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(localIntent);
						}
					});
				}
				
			}, 0, 1000);
			
			_isInitialized = true;
		}
		
		//
		// Check for a reset. If present, reset our statistics.
		//
		boolean reset = intent.getBooleanExtra(SERVICE_DATA_RESET, false);
		if(reset) {
			_count = 0;
			_timeLastLocationUpdate = 0;
			_timeSinceStarted = SystemClock.elapsedRealtime();
		}
		
		//
		// Look for the closest location data. If preset, we can update the accuracy of the
		// LocationManager updates. 
		// 
		double closestLocation = intent.getDoubleExtra(SERVICE_DATA_CLOSEST_LOCATION, -1);
		if(closestLocation >= 0) {
			
		}
		
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// no binding
		return null;
	}

	@Override
	public void onDestroy() {
		
		//
		// Remove ourself from the LocationManager callback.
		//
		if(_locationManager != null) {
			_locationManager.removeUpdates(this);
		}
		
		//
		// Quite the service looper thread.
		//
		if(_serviceLooper != null) {
			_serviceLooper.quit();
		}
		
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
	}

	//
	// LocationListener implementation called when the LocationManager updates
	// based on our Criteria.
	//
	@Override
	public void onLocationChanged(Location location) {

        //
        // A location must have a certain accuracy before it is sent to the server.
		// It must also be LOCATION_DISTANCE_THREASHOLD from the previous Location
        //
        if(location.getAccuracy() < LOCATION_ACCURACY_THREASHOLD && this.meetsDistanceThreashold(location)) {
        	
        	_count++;
        	_lastValidLocation = location;
        	
	        Intent localIntent =
	                new Intent(BROADCAST_ACTION)
	                // Puts the status into the Intent
	                .putExtra(EXTENDED_DATA_LOCATION, location)
	                .putExtra(EXTENDED_DATA_COUNT, _count);
	        
	        // Broadcasts the Intent to receivers in this app.
	
	        _timeLastLocationUpdate = SystemClock.elapsedRealtime();

        	LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }
    }
	
	private boolean meetsDistanceThreashold(Location location) {
		boolean ret = false;
		if(_lastValidLocation == null || location.distanceTo(_lastValidLocation) > LOCATION_DISTANCE_THREASHOLD) {
			ret = true;
		}
		
		return ret;
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		// nothing to do
	}

	@Override
	public void onProviderEnabled(String provider) {
		// nothing to do
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
		Integer oldStatus = _locationProviderStatus.get(provider);
		if(oldStatus == null || oldStatus.intValue() != status) {
			String message;
			
			switch(status) {
			case LocationProvider.AVAILABLE:
				message = "Available";
				break;
			case LocationProvider.OUT_OF_SERVICE:
				message = "Out of Service";
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				message = "Temporarily Unavailable";
				break;
			default:
				message = "Unknown";
				break;
			}
			
			this.showMessageOnUI("LocationManager onStatusChanged for: " + provider + " to: " + message);
			_locationProviderStatus.put(provider, Integer.valueOf(status));
		}
	}

	public void showMessageOnUI(final String message) {
		this.showMessageOnUI(message, Toast.LENGTH_LONG);
	}
	
	//
	// This shows a message as a Toast but first transfers the message to the UI thread/Looper.
	//
	Handler _mainLoopHandler = new Handler(Looper.getMainLooper());
	public void showMessageOnUI(final String message, final int length) {
		_mainLoopHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), message, length).show();

			}
		});
	}
	
	//
	// POJO for statistics data so we can send them as a single object.
	//
	public static class LocationStats implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private long count;
		private long elapsedTimeSinceUpdated;
		private long elapsedTimeSinceStarted;
		
		public LocationStats() {
			
		}

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}

		public long getElapsedTimeSinceUpdated() {
			return elapsedTimeSinceUpdated;
		}

		public void setElapsedTimeSinceUpdated(long elapsedTimeSinceUpdated) {
			this.elapsedTimeSinceUpdated = elapsedTimeSinceUpdated;
		}

		public long getElapsedTimeSinceStarted() {
			return elapsedTimeSinceStarted;
		}

		public void setElapsedTimeSinceStarted(long elapsedTimeSinceStarted) {
			this.elapsedTimeSinceStarted = elapsedTimeSinceStarted;
		}
	}
}
