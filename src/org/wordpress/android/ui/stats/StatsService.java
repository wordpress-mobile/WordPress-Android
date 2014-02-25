package org.wordpress.android.ui.stats;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.wordpress.android.ui.stats.tasks.ClicksTask;
import org.wordpress.android.ui.stats.tasks.ReferrersTask;
import org.wordpress.android.ui.stats.tasks.SearchEngineTermsTask;
import org.wordpress.android.ui.stats.tasks.SummaryTask;
import org.wordpress.android.ui.stats.tasks.TopPostsAndPagesTask;
import org.wordpress.android.ui.stats.tasks.ViewsByCountryTask;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StatUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by nbradbury on 2/25/14.
 */
public class StatsService extends Service {
    public static final String ARG_BLOG_ID = "blog_id";
    private static final int MAX_TASKS = 3;
    private final ThreadPoolExecutor mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_TASKS);

    private static final long EXECUTOR_TIMEOUT = 60 * 1000;
    public static final long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;

    public static final String ACTION_STAT_UPDATE_STARTED = "wp-stats-update-started";
    public static final String ACTION_STAT_UPDATE_ENDED   = "wp-stats-update-ended";

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
        final String blogId = intent.getStringExtra(ARG_BLOG_ID);
        final String today = StatUtils.getCurrentDate();
        final String yesterday = StatUtils.getYesterdaysDate();

        broadcastAction(ACTION_STAT_UPDATE_STARTED);
        try {
            // summary (visitors and views)
            mExecutor.submit(new SummaryTask(blogId));

            // top posts and pages
            mExecutor.submit(new TopPostsAndPagesTask(blogId, today));
            mExecutor.submit(new TopPostsAndPagesTask(blogId, yesterday));

            // clicks
            mExecutor.submit(new ClicksTask(blogId, today));
            mExecutor.submit(new ClicksTask(blogId, yesterday));

            // referrers
            mExecutor.submit(new ReferrersTask(blogId, today));
            mExecutor.submit(new ReferrersTask(blogId, yesterday));

            // search engine terms
            mExecutor.submit(new SearchEngineTermsTask(blogId, today));
            mExecutor.submit(new SearchEngineTermsTask(blogId, yesterday));

            // views by country
            mExecutor.submit(new ViewsByCountryTask(blogId, today));
            mExecutor.submit(new ViewsByCountryTask(blogId, yesterday));

            /*
            // comments
            mExecutor.submit(new CommentsTask(blogId));
            // tags and categories
            mExecutor.submit(new TagsAndCategoriesTask(blogId));
            // top authors
            mExecutor.submit(new TopAuthorsTask(blogId));
            // video plays
            mExecutor.submit(new VideoPlaysTask(blogId));
            */

        } catch (RejectedExecutionException e) {
            AppLog.e(T.STATS, e);
        }

        // wait for tasks to complete
        mExecutor.shutdown();
        try {
            mExecutor.awaitTermination(EXECUTOR_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            AppLog.e(T.STATS, e);
        } finally {
            broadcastAction(ACTION_STAT_UPDATE_ENDED);
        }

        return START_NOT_STICKY;
    }

    private void broadcastAction(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
