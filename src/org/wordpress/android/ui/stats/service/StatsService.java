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
 * concurrent updating of the various stats tasks - see AbsStatsTask for base
 * implementation of an individual stats task
 */

// TODO: broadcast when summary changes
// TODO: modify each task to only notify content resolver of changes if changes actually exist
public class StatsService extends Service {
    public static final String ARG_BLOG_ID = "blog_id";

    // broadcast action used to notify clients of update start/end
    public static final String ACTION_STATS_UPDATING = "wp-stats-updating";
    public static final String EXTRA_IS_UPDATING = "is-updating";

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

    /*
     * create executor to process stats tasks, limited to one concurrent task for single-core
     * devices and two concurrent tasks for all other devices - this limit is necessary due
     * to how various tasks rely on getContentResolver().notifyChange() to notify fragments
     * of changes to underlying data, resulting in work being done on the UI thread - without
     * this limit the stats views would stutter noticeably
     */
    private ThreadPoolExecutor createExecutor() {
        int numCPUs = Runtime.getRuntime().availableProcessors();
        int numConcurrentTasks = Math.min(numCPUs, 2);
        AppLog.i(T.STATS, "creating executor with pool size = " + numConcurrentTasks);
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(numConcurrentTasks);
    }

    private static final int EXECUTOR_TIMEOUT_SECONDS = 60;
    private void startTasks(final String blogId) {
        final ThreadPoolExecutor executor = createExecutor();

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

                AppLog.i(T.STATS, "stats update started");
                broadcastUpdate(true);
                try {
                    // prevent additional tasks from being submitted, then wait for all tasks to complete
                    executor.shutdown();
                    if (!executor.awaitTermination(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        AppLog.w(T.STATS, "executor failed to terminate");
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    AppLog.e(T.STATS, e);
                    // (re-)cancel if current thread also interrupted
                    executor.shutdownNow();
                    // preserve interrupt status
                    Thread.currentThread().interrupt();
                } finally {
                    AppLog.i(T.STATS, "stats update ended");
                    broadcastUpdate(false);
                }
            }
        }.start();
    }

    /*
     * broadcast that the update has started - used by StatsActivity to animate refresh
     * icon while update is in progress
     */
    private void broadcastUpdate(boolean isUpdating) {
        Intent intent = new Intent()
                .setAction(ACTION_STATS_UPDATING)
                .putExtra(EXTRA_IS_UPDATING, isUpdating);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
