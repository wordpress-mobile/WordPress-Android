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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by nbradbury on 2/25/14.
 * Background service to retrieve latest stats - uses a ThreadPoolExecutor to handle
 * concurrent updating of the various stats tasks
 */
public class StatsService extends Service {
    public static final String ARG_BLOG_ID = "blog_id";

    // broadcast actions used to notify clients of update start/end
    public static final String ACTION_STAT_UPDATE_STARTED = "wp-stats-update-started";
    public static final String ACTION_STAT_UPDATE_ENDED   = "wp-stats-update-ended";

    private static final long EXECUTOR_TIMEOUT = 30 * 1000;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String blogId = StringUtils.notNullStr(intent.getStringExtra(ARG_BLOG_ID));
        startTasks(blogId);
        return START_NOT_STICKY;
    }

    private void startTasks(final String blogId) {
        broadcastAction(ACTION_STAT_UPDATE_STARTED);
        AppLog.i(T.STATS, "stats update started");
        try {
            final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getMaxConcurrentTasks());
            // submit tasks from a separate thread or else they'll run on the main thread
            new Thread() {
                @Override
                public void run() {
                    final String today = StatUtils.getCurrentDate();
                    final String yesterday = StatUtils.getYesterdaysDate();

                    // visitors and views
                    executor.submit(new SummaryTask(blogId)); // this includes bar chart data for days
                    executor.submit(new BarChartTask(blogId, StatsBarChartUnit.WEEK));
                    executor.submit(new BarChartTask(blogId, StatsBarChartUnit.MONTH));

                    // top posts and pages
                    executor.submit(new TopPostsAndPagesTask(blogId, today));
                    executor.submit(new TopPostsAndPagesTask(blogId, yesterday));

                    // clicks
                    executor.submit(new ClicksTask(blogId, today));
                    executor.submit(new ClicksTask(blogId, yesterday));

                    // referrers
                    executor.submit(new ReferrersTask(blogId, today));
                    executor.submit(new ReferrersTask(blogId, yesterday));

                    // search engine terms
                    executor.submit(new SearchEngineTermsTask(blogId, today));
                    executor.submit(new SearchEngineTermsTask(blogId, yesterday));

                    // views by country
                    executor.submit(new ViewsByCountryTask(blogId, today));
                    executor.submit(new ViewsByCountryTask(blogId, yesterday));

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

                    // wait for tasks to complete
                    try {
                        executor.awaitTermination(EXECUTOR_TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        AppLog.e(T.STATS, e);
                    }
                }
            }.start();
        } finally {
            AppLog.i(T.STATS, "stats update ended");
            broadcastAction(ACTION_STAT_UPDATE_ENDED);
        }
    }

    private void broadcastAction(String action) {
        Intent intent = new Intent().setAction(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private static int getMaxConcurrentTasks() {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        return (numProcessors > 1 ? 2 : 1);
    }
}
