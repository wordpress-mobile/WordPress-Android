package org.wordpress.android.util.analytics.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;


/**
 * Background service to connect to Google Play Store's Install Referrer API to
 * securely retrieve referral content from Google Play.
 * This could be done on the app's main activity but, as we are going to trigger this data gathering from
 * a BroadcastReceiver, we need a Service / JobService to keep it alive while this happens, even if it needs
 * to happen only once.
 * see https://developer.android.com/google/play/installreferrer/library
 * https://developer.android.com/reference/android/content/Intent.html#ACTION_PACKAGE_FIRST_LAUNCH
 * https://developer.android.com/guide/components/broadcasts.html#receiving_broadcasts
 * https://developer.android.com/guide/components/broadcasts#effects-on-process-state
 */
public class InstallationReferrerService extends Service implements
        InstallationReferrerServiceLogic.ServiceCompletionListener {
    private InstallationReferrerServiceLogic mInstallationReferrerServiceLogic;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(T.UTILS, "installation referrer service created");
        mInstallationReferrerServiceLogic = new InstallationReferrerServiceLogic(this, this);
    }

    @Override
    public void onDestroy() {
        AppLog.i(T.UTILS, "installation referrer service destroyed");
        mInstallationReferrerServiceLogic.onDestroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppLog.i(T.UTILS, "installation referrer service > task: " + startId + " started");
        mInstallationReferrerServiceLogic.performTask(intent.getExtras(), startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCompleted(Object companion) {
        if (companion instanceof Integer) {
            AppLog.i(T.UTILS, "installation referrer service > task: " + companion + " completed");
            stopSelf((Integer) companion);
        } else {
            AppLog.i(T.UTILS, "installation referrer service > task: <not identified> completed");
            stopSelf();
        }
    }
}
