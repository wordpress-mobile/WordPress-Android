package org.wordpress.android.util.analytics.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.analytics.service.InstallationReferrerServiceStarter;

public class InstallationReferrerReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent receivedIntent) {
        AppLog.i(AppLog.T.UTILS, "installation referrer RECEIVER: received");
        String referrer = receivedIntent.getStringExtra("referrer");
        InstallationReferrerServiceStarter.startService(context, referrer);
    }
}
