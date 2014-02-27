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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by nbradbury on 2/25/14.
 * Background service to retrieve latest stats - uses a ThreadPoolExecutor to handle
 * concurrent updating of the various stats tasks - see AbsStatsTask for base
 * implementation of an individual stats task
 */

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

    private static final int EXECUTOR_TIMEOUT_SECONDS = 60;
    private void startTasks(final String blogId) {
        final StatsTaskExecutor executor = new StatsTaskExecutor();

        // submit tasks from a separate thread or else they'll run on the main thread - note that
        // these are submitted in the order they appear
        new Thread() {
            @Override
            public void run() {
                final String today = StatUtils.getCurrentDate();
                final String yesterday = StatUtils.getYesterdaysDate();

                // visitors and views
                executor.submitTask(new SummaryTask(blogId)); // this includes bar chart data for days
                executor.submitTask(new BarChartTask(blogId, StatsBarChartUnit.WEEK));
                executor.submitTask(new BarChartTask(blogId, StatsBarChartUnit.MONTH));

                // top posts and pages
                executor.submitTask(new TopPostsAndPagesTask(blogId, today));
                executor.submitTask(new TopPostsAndPagesTask(blogId, yesterday));

                // views by country
                executor.submitTask(new ViewsByCountryTask(blogId, today));
                executor.submitTask(new ViewsByCountryTask(blogId, yesterday));

                // clicks
                executor.submitTask(new ClicksTask(blogId, today));
                executor.submitTask(new ClicksTask(blogId, yesterday));

                // referrers
                executor.submitTask(new ReferrersTask(blogId, today));
                executor.submitTask(new ReferrersTask(blogId, yesterday));

                // search engine terms
                executor.submitTask(new SearchEngineTermsTask(blogId, today));
                executor.submitTask(new SearchEngineTermsTask(blogId, yesterday));

                /*
                // comments
                executor.submitTask(new CommentsTopTask(blogId));
                executor.submitTask(new CommentsMostTask(blogId));
                // tags and categories
                executor.submitTask(new TagsAndCategoriesTask(blogId));
                // top authors
                executor.submitTask(new TopAuthorsTask(blogId));
                // video plays
                executor.submitTask(new VideoPlaysTask(blogId));
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

    /*
     * executor to process stats tasks, limited to one concurrent task - this limit is necessary
     * due to how tasks use getContentResolver().notifyChange() to notify fragments of changes
     * to underlying data, resulting in work being done on the UI thread - without this limit
     * the stats views would stutter noticeably
     */
    private class StatsTaskExecutor extends ThreadPoolExecutor {
        private StatsTaskExecutor() {
            // same as Executors.newFixedThreadPool()
            super(1, 1,
                  0L, TimeUnit.MILLISECONDS,
                  new LinkedBlockingQueue<Runnable>());
        }

        private void submitTask(AbsStatsTask task) {
            submit(task);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
        }
    }
}
