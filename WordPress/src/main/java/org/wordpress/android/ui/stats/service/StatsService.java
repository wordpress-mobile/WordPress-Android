package org.wordpress.android.ui.stats.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.stats.StatsEvents;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import de.greenrobot.event.EventBus;


/**
 * Background service to retrieve Stats.
 * Parsing of response(s) and submission of new network calls are done by using a ThreadPoolExecutor
 * with a single thread.
 */

public class StatsService extends Service implements StatsServiceLogic.ServiceCompletionListener {
    public static final String ARG_BLOG_ID = "blog_id";
    public static final String ARG_PERIOD = "stats_period";
    public static final String ARG_DATE = "stats_date";
    public static final String ARG_SECTION = "stats_section";
    public static final String ARG_MAX_RESULTS = "stats_max_results";
    public static final String ARG_PAGE_REQUESTED = "stats_page_requested";

    // The number of results to return per page for Paged REST endpoints. Numbers larger than 20 will
    // default to 20 on the server.
    public static final int MAX_RESULTS_REQUESTED_PER_PAGE = 20;
    public static final int TASK_ID_GROUP_ALL = -2;

    private StatsServiceLogic mStatsServiceLogic;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(T.STATS, "stats service created");
        mStatsServiceLogic = new StatsServiceLogic(this);
        mStatsServiceLogic.onCreate((WordPress) getApplication());
    }

    @Override
    public void onDestroy() {
        AppLog.i(T.STATS, "stats service destroyed");
        mStatsServiceLogic.onDestroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppLog.i(AppLog.T.STATS, "stats service > task: " + startId + " started");
        EventBus.getDefault().post(new StatsEvents.UpdateStatusStarted(startId));
        mStatsServiceLogic.performTask(intent.getExtras(), Integer.valueOf(startId));
        return START_NOT_STICKY;
    }

    @Override
    public void onCompleted(Object companion) {
        EventBus.getDefault().post(new StatsEvents.UpdateStatusFinished(TASK_ID_GROUP_ALL));
        if (companion instanceof Integer) {
            AppLog.i(AppLog.T.STATS, "stats service > task: " + companion + " completed");
            stopSelf((Integer) companion);
        } else {
            AppLog.i(AppLog.T.STATS, "stats service > task: <not identified> completed");
            stopSelf();
        }
    }
}
