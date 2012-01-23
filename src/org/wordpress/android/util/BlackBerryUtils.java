package org.wordpress.android.util;

public class BlackBerryUtils {

	private static BlackBerryUtils instance;

	private boolean isPlayBook = false;
	
	public boolean isPlayBook() {
		return isPlayBook;
	}

	public static BlackBerryUtils getInstance() {
		if (instance == null) {
			instance = new BlackBerryUtils();
		}
		return instance;
	}

	private BlackBerryUtils() {
		isPlayBook =  android.os.Build.MANUFACTURER.equalsIgnoreCase( "Research in Motion" ) &&  
				android.os.Build.MODEL.equalsIgnoreCase("BlackBerry Runtime for Android" );
	};
}