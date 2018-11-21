package org.wordpress.android.ui.stats.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Bundle;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.stats.StatsEvents;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.ui.stats.service.StatsServiceStarter.ARG_START_ID;


/**
 * Background service to retrieve Stats.
 * Parsing of response(s) and submission of new network calls are done by using a ThreadPoolExecutor
 * with a single thread.
 */
public class StatsJobService extends JobService implements StatsServiceLogic.ServiceCompletionListener {
    @Override
    public boolean onStartJob(JobParameters params) {
        if (params.getExtras() != null) {
            int startId = params.getExtras().getInt(ARG_START_ID);
            EventBus.getDefault().post(new StatsEvents.UpdateStatusStarted(startId));
            AppLog.i(T.STATS, "stats job service > task: " + startId + " started");
            StatsServiceLogic logic = new StatsServiceLogic(this);
            logic.onCreate((WordPress) getApplication());
            logic.performTask(
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
        EventBus.getDefault().post(new StatsEvents.UpdateStatusFinished(startId));
        jobFinished((JobParameters) companion, false);
    }
}
