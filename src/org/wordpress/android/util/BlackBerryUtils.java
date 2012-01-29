package org.wordpress.android.util;

/**
 * As of January 20 2012:
 * The BlackBerry Runtime for Android Apps supports Android 2.3.3 applications. 
 * 
 * Unsupported App Types:
 * - Widget apps : Apps that are only meant to be widgets are not supported
 * - Apps that include no launchable Activity
 * - Apps that include more than one launchable Activity
 * - Apps whose minimum required Android API level is more than 10, and whose maximum supported level is less than 10
 *  
 *  Unsupported Hardware Features:
 *  - Telephony (including SMS and MMS)
 *  - Bluetooth
 *  - Camera:  The intent to launch the camera is supported. However, currently the Camera class in the Android SDK is not supported. 
 *  As a result, although you can launch the camera application, you cannot access the Camera hardware.
 * - NFC
 * - Barometers
 * - Ambient light sensor
 * - Proximity sensor
 * - VoIP
 * 
 * Unsupported Software Features:
 * - Vending (In App Payments): com.android.vending
 * - Cloud To Device Messaging (Push): com.google.android.c2dm
 * - Google Maps: com.google.android.maps
 * - Text to Speech: com.google.tts
 * 
 * 
 * Major Details here: https://bdsc.webapps.blackberry.com/android/apisupport
 * 
 * 
 * @author daniloercoli
 *
 */


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