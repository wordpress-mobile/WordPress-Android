package org.wordpress.android.ui.stats.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.wordpress.android.ui.stats.StatsBarChartUnit;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by nbradbury on 2/25/14.
 * Background service to retrieve latest stats - uses a ThreadPoolExecutor to
 * handle concurrent updating of the various stats tasks - see AbsStatsTask for
 * base implementation of an individual stats task
 */

public class StatsService extends Service {
    public static final String ARG_BLOG_ID = "blog_id";

    // broadcast action to notify clients of update start/end
    public static final String ACTION_STATS_UPDATING = "wp-stats-updating";
    public static final String EXTRA_IS_UPDATING = "is-updating";

    // broadcast action to notify clients when summary data has changed
    public static final String ACTION_STATS_SUMMARY_UPDATED = "STATS_SUMMARY_UPDATED";
    public static final String STATS_SUMMARY_UPDATED_EXTRA = "STATS_SUMMARY_UPDATED_EXTRA";

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(T.STATS, "service created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(T.STATS, "service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String blogId = StringUtils.notNullStr(intent.getStringExtra(ARG_BLOG_ID));
        startTasks(blogId, startId);
        return START_NOT_STICKY;
    }

    private static int getMaxConcurrentTasks() {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        return Math.min(numProcessors, 4);
    }

    /*
     * submit and process all stats tasks - tasks are submitted in the order they're displayed:
     *  Visitors and views (summary)
     *  Top posts & pages
     *  Views by country
     *  Referrers
     *  Clicks
     *  Search engine terms
     */
    private static final int EXECUTOR_TIMEOUT_SECONDS = 60;
    private void startTasks(final String blogId, final int startId) {
        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getMaxConcurrentTasks());

        new Thread() {
            @Override
            public void run() {
                final String today = StatUtils.getCurrentDate();
                final String yesterday = StatUtils.getYesterdaysDate();

                try {
                    // visitors and views
                    executor.submit(new SummaryTask(blogId)); // this includes bar chart data for days
                    executor.submit(new BarChartTask(blogId, StatsBarChartUnit.WEEK));
                    executor.submit(new BarChartTask(blogId, StatsBarChartUnit.MONTH));

                    // top posts and pages
                    executor.submit(new TopPostsAndPagesTask(blogId, today));
                    executor.submit(new TopPostsAndPagesTask(blogId, yesterday));

                    // views by country
                    executor.submit(new ViewsByCountryTask(blogId, today));
                    executor.submit(new ViewsByCountryTask(blogId, yesterday));

                    // referrers
                    executor.submit(new ReferrersTask(blogId, today));
                    executor.submit(new ReferrersTask(blogId, yesterday));

                    // clicks
                    executor.submit(new ClicksTask(blogId, today));
                    executor.submit(new ClicksTask(blogId, yesterday));

                    // search engine terms
                    executor.submit(new SearchEngineTermsTask(blogId, today));
                    executor.submit(new SearchEngineTermsTask(blogId, yesterday));

                    /*
                    // comments
                    executor.submit(new CommentsTopTask(blogId));
                    executor.submit(new CommentsMostTask(blogId));
                    // tags and categories
                    executor.submit(new TagsAndCategoriesTask(blogId));
                    // top authors
                    executor.submit(new TopAuthorsTask(blogId));
                    // video plays
                    executor.submit(new VideoPlaysTask(blogId));
                    */
                } catch (RejectedExecutionException e) {
                    AppLog.e(T.STATS, e);
                }

                AppLog.i(T.STATS, "update started");
                broadcastUpdate(true);
                try {
                    // prevent additional tasks from being submitted, then wait for all tasks to complete
                    executor.shutdown();
                    if (!executor.awaitTermination(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        AppLog.w(T.STATS, "executor failed to terminate");
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    AppLog.w(T.STATS, "executor interrupted");
                    // (re-)cancel if current thread also interrupted
                    executor.shutdownNow();
                    // preserve interrupt status
                    Thread.currentThread().interrupt();
                } finally {
                    AppLog.i(T.STATS, "update ended");
                    broadcastUpdate(false);
                    stopSelf(startId);
                }
            }
        }.start();
    }

    /*
     * broadcast that the update has started/ended - used by StatsActivity to animate refresh
     * icon while update is in progress
     */
    private void broadcastUpdate(boolean isUpdating) {
        Intent intent = new Intent()
                .setAction(ACTION_STATS_UPDATING)
                .putExtra(EXTRA_IS_UPDATING, isUpdating);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
