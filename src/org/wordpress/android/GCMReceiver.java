package org.wordpress.android;

/*
 * This class is used in "zbetagroup" buildType only, and it's a workaround to fix problems on starting the GCM service.
 * 
 * The default implementation of GCMBroadcastReceiver, available in the GCM API - com.google.android.gcm.GCMBroadcastReceiver, 
 * was trying to start the service with a wrong package name (org.wordpress.android.beta/.GCMIntentService). 
 * Details here: http://dexxtr.com/post/28188228252/rename-or-change-package-of-gcmintentservice-class
 * 
 * Error msg we were seeing:
 * W/ActivityManager(1866): Unable to start service Intent { act=com.google.android.c2dm.intent.REGISTRATION flg=0x10 pkg=org.wordpress.android.beta cmp=org.wordpress.android.beta/.GCMIntentService (has extras) } U=0: not found
 * 
 */

import android.content.Context;

import com.google.android.gcm.GCMBroadcastReceiver;

public class GCMReceiver extends GCMBroadcastReceiver { 
    @Override
    protected String getGCMIntentServiceClassName(Context context) { 
        return "org.wordpress.android.GCMIntentService"; 
    } 
}