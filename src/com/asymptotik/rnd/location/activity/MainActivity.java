package com.asymptotik.rnd.location.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.asymptotik.rnd.location.R;
import com.asymptotik.rnd.location.StringUtils;
import com.asymptotik.rnd.location.service.LocationBroadcastReceiver;
import com.asymptotik.rnd.location.service.LocationService;

public class MainActivity extends Activity implements LocationBroadcastReceiver.OnReceiveHandler {
	
	//
	// These here for the demo app only.
	//
	private TextView _lastUpdatedLabel;
	private TextView _latitudeLabel;
	private TextView _longitudeLabel;
	private TextView _samplesLabel;
	private TextView _samplesPerSecondLabel;
	private TextView _accuracyLabel;
	
	private LocationBroadcastReceiver _locationBroadcastReceiver;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        _lastUpdatedLabel = (TextView)this.findViewById(R.id.label_last_updated);
        _latitudeLabel = (TextView)this.findViewById(R.id.label_latitude);
        _longitudeLabel = (TextView)this.findViewById(R.id.label_longitude);
        _samplesLabel = (TextView)this.findViewById(R.id.label_samples);
        _samplesPerSecondLabel = (TextView)this.findViewById(R.id.label_samples_per_second);
        _accuracyLabel = (TextView)this.findViewById(R.id.label_accuracy);
        
        Button resetButton = (Button)this.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				MainActivity.this.sendResetStats();
			}
		});
        
        _locationBroadcastReceiver = new LocationBroadcastReceiver();
        _locationBroadcastReceiver.setOnReceiveHandler(this);
    }

    @Override
	protected void onResume() {
		super.onResume();
		
		//
		// You will only want to start the service if the user is logged in
		//
		this.startService();
		
		//
		// Setup the BroadcastReceiver to receive information from the LocationService.
		//
		
        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(LocationService.BROADCAST_ACTION);

        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(_locationBroadcastReceiver, statusIntentFilter);
    }
    
    @Override
    public void onPause() {
    	// Unregister the BroadcastReceiver while this Activity is paused.
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(_locationBroadcastReceiver);
    	super.onPause();
    }
    
    protected void startService() {
    	//
    	// Start the service as a 'started' service. Note we never bind to the service.
    	Intent intent = new Intent(this, LocationService.class);
		startService(intent);
    }

    protected void stopService() {
    	//
    	// We should stop the service if the user is ever logged out.
    	//
    	Intent intent = new Intent(this, LocationService.class);
    	stopService(intent);
    }
    
    //
    // This method sends data to the LocationService via an intent. The data signals
    // the intent to reset it's statistics data. This should only be 
    // called if the user is logged in as it will start the LocationService.
    //
	public void sendResetStats() {
		Intent intent = new Intent(this, LocationService.class);
		intent.putExtra(LocationService.SERVICE_DATA_RESET, true);
		startService(intent);
	}
	
    //
    // This method sends data to the LocationService via an intent. The data sent is the closest
	// location to the known locations to the user. This should only be 
    // called if the user is logged in as it will start the LocationService.
    //
	public void sendClosestLocation(double distanceInMeters) {
		Intent intent = new Intent(this, LocationService.class);
		intent.putExtra(LocationService.SERVICE_DATA_CLOSEST_LOCATION, distanceInMeters);
		startService(intent);
	}
	
    //
    // LocationBroadcastReceiver.OnReceiveHandler. This will receive data from our LocationService. This sample app
    // simply displays that data on the screen. The real app will send the lat/lon to the Whats Poppin server and get a list
    // of locations.
    //
	@Override
	public void onReceive(Context context, Intent intent) {
		// This is handled on the main thread unless we specify otherwise in registerReceiver
		Location location = (Location)intent.getParcelableExtra(LocationService.EXTENDED_DATA_LOCATION);
		if(location != null) {
			this.locationUpdated(location);
		}
		long count = intent.getLongExtra(LocationService.EXTENDED_DATA_COUNT, -1);
		if (count >= 0) {
			this.countUpdated(count);
		}
		
		LocationService.LocationStats stats = (LocationService.LocationStats)intent.getSerializableExtra(LocationService.EXTENDED_DATA_STATS);
		if(stats != null) {
			this.statsUpdated(stats);
		}
	}
	
    // 
	// These are convenience routines to update the UI labels.
	//

    public void locationUpdated(Location location) {
		_latitudeLabel.setText(String.format( "%.8f", location.getLatitude()));
		_longitudeLabel.setText(String.format( "%.8f", location.getLongitude()));
		_accuracyLabel.setText(String.format( "%.8f", location.getAccuracy()));
    }
    
    public void countUpdated(long count) {
		_samplesLabel.setText(String.format("%d", count));
    }
    
    public void statsUpdated(LocationService.LocationStats stats) {
    	if(stats.getCount() > 0) {
    		double frequency =((double)stats.getElapsedTimeSinceStarted() /  1000.0)/  (double)stats.getCount();
    		_samplesPerSecondLabel.setText(String.format("%f", frequency));
    	} else {
    		_samplesPerSecondLabel.setText("n/a");
    	}
    	
    	if(stats.getElapsedTimeSinceUpdated() > 0) {
    		_lastUpdatedLabel.setText(StringUtils.stringWithOffsetInSeconds(stats.getElapsedTimeSinceUpdated()));
    	} else {
    		_lastUpdatedLabel.setText("n/a");
    	}
    	
    	this.countUpdated(stats.getCount());
    }
}
