package org.wordpress.android.ui.stats.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Bundle;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import static org.wordpress.android.ui.stats.service.StatsServiceStarter.ARG_START_ID;


/**
 * Background service to retrieve Stats.
 * Parsing of response(s) and submission of new network calls are done by using a ThreadPoolExecutor
 * with a single thread.
 */

@TargetApi(21)
public class StatsJobService extends JobService implements StatsServiceLogic.ServiceCompletionListener {
    private StatsServiceLogic mStatsServiceLogic;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(T.STATS, "stats job service created");
        mStatsServiceLogic = new StatsServiceLogic(this);
        mStatsServiceLogic.onCreate((WordPress) getApplication());
    }

    @Override
    public void onDestroy() {
        AppLog.i(T.STATS, "stats job service destroyed");
        mStatsServiceLogic.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        if (params.getExtras() != null) {
            int startId = params.getExtras().getInt(ARG_START_ID);
            AppLog.i(T.STATS, "stats job service > task: " + startId + " started");
            mStatsServiceLogic.performTask(
                    new Bundle(params.getExtras()),
                    params);
            return true;
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        AppLog.i(AppLog.T.STATS, "stats job service > stopped");
        jobFinished(params, false);
        return false;
    }

    @Override
    public void onCompleted(Object companion) {
        int startId = ((JobParameters) companion).getExtras().getInt(ARG_START_ID, 0);
        AppLog.i(T.STATS, "stats job service > task: " + startId + " completed");
        jobFinished((JobParameters) companion, false);
    }
}
