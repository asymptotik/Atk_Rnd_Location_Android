package com.asymptotik.rnd.location.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//
// This BroadcastReceiver simply delegates it's onReceive call to 
// an OnReceiveHandler. It can thus be used for both a LocalBroadcastReceiver and
// a regular BrodcastReceiver.
//
public class LocationBroadcastReceiver extends BroadcastReceiver {

	private OnReceiveHandler _onReceiveHandler = null;
	
	public LocationBroadcastReceiver() {
		
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(_onReceiveHandler != null) {
			_onReceiveHandler.onReceive(context, intent);
		}
	}
	
	public void setOnReceiveHandler(OnReceiveHandler handler) {
		_onReceiveHandler = handler;
	}
	
	public static interface OnReceiveHandler {
		public void onReceive(Context context, Intent intent);
	}
}
