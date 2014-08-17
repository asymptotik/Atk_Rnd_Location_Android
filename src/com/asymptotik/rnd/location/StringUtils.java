package com.asymptotik.rnd.location;

import java.util.Locale;

public class StringUtils {

	private StringUtils() {}

	//
	// Converts millis to user friendly string
	//
	public static String stringWithOffsetInSeconds(double millis) {
	    double timeFromStart = millis / 1000.0;
	    long hours   = (long) Math.floor(timeFromStart / 3600);
	    long minutes = (long)(timeFromStart / 60) % 60;
	    long seconds = (long)timeFromStart % 60;
	    
	    String text;
	    if (hours == 0) {
	        if (minutes == 0) {
	            text = String.format(Locale.getDefault(), "%ds", seconds);
	        }
	        else {
	            text = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
	        }
	    }
	    else {
	        text = String.format(Locale.getDefault(), "%d:%d:02%d", hours, minutes, seconds);
	    }
	    
	    return text;
	}
}
