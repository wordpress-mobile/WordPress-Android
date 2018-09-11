package org.wordpress.android.util.analytics.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.analytics.service.InstallationReferrerServiceStarter;


public class PackageFirstLaunchReceiver extends BroadcastReceiver {
    private static PackageFirstLaunchReceiver sInstance;
    public void onReceive(Context context, Intent receivedIntent) {
        AppLog.i(AppLog.T.UTILS, "package first launch RECEIVER: received");
        InstallationReferrerServiceStarter.startService(context, null);
    }

    public static PackageFirstLaunchReceiver getInstance() {
        if (sInstance == null) {
            sInstance = new PackageFirstLaunchReceiver();
        }
        return sInstance;
    }
}


