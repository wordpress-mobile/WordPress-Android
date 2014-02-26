package org.wordpress.android.ui.stats;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.wordpress.android.ui.stats.tasks.BarChartTask;
import org.wordpress.android.ui.stats.tasks.ClicksTask;
import org.wordpress.android.ui.stats.tasks.ReferrersTask;
import org.wordpress.android.ui.stats.tasks.SearchEngineTermsTask;
import org.wordpress.android.ui.stats.tasks.SummaryTask;
import org.wordpress.android.ui.stats.tasks.TopPostsAndPagesTask;
import org.wordpress.android.ui.stats.tasks.ViewsByCountryTask;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by nbradbury on 2/25/14.
 * Background service to retrieve latest stats - see various stats tasks in the
 * ui/stats/tasks package
 */
public class StatsService extends Service {
    public static final String ARG_BLOG_ID = "blog_id";

    public static final String ACTION_STAT_UPDATE_STARTED = "wp-stats-update-started";
    public static final String ACTION_STAT_UPDATE_ENDED   = "wp-stats-update-ended";

    private static final long EXECUTOR_TIMEOUT = 30 * 1000;
    public static final long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;

    private static final int MAX_CONCURRENT_TASKS = 3;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

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
        final StatsExecutor executor = new StatsExecutor();
        final String today = StatUtils.getCurrentDate();
        final String yesterday = StatUtils.getYesterdaysDate();

        // submit tasks from a separate thread or else they'll run on the main thread
        new Thread() {
            @Override
            public void run() {
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
                broadcastAction(ACTION_STAT_UPDATE_STARTED);
                try {
                    executor.awaitTermination(EXECUTOR_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    AppLog.e(T.STATS, e);
                } finally {
                    broadcastAction(ACTION_STAT_UPDATE_ENDED);
                }
            }
        }.start();
    }

    private void broadcastAction(String action) {
        Intent intent = new Intent().setAction(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private class StatsExecutor extends ThreadPoolExecutor {
        public StatsExecutor() {
            // same as Executors.newFixedThreadPool()
            super(MAX_CONCURRENT_TASKS,
                  MAX_CONCURRENT_TASKS,
                  0L,
                  TimeUnit.MILLISECONDS,
                  new LinkedBlockingQueue<Runnable>());
        }
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            AppLog.i(T.STATS, "beforeExecute " + r.toString());
        }
        protected void afterExecute(Runnable r, Throwable t) {
            AppLog.i(T.STATS, "afterExecute " + r.toString());
            super.afterExecute(r, t);
        }

    }
}
