package org.wordpress.android.util.analytics.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Bundle;

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
public class InstallationReferrerJobService extends JobService implements
        InstallationReferrerServiceLogic.ServiceCompletionListener {
    @Override
    public boolean onStartJob(JobParameters params) {
        AppLog.i(T.UTILS, "installation referrer job service > started");
        InstallationReferrerServiceLogic logic = new InstallationReferrerServiceLogic(this, this);
        logic.performTask(new Bundle(params.getExtras()), params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        AppLog.i(T.UTILS, "installation referrer job service > stopped");
        jobFinished(params, false);
        return false;
    }

    @Override
    public void onCompleted(Object companion) {
        AppLog.i(T.UTILS, "installation referrer job service > completed");
        jobFinished((JobParameters) companion, false);
    }
}
