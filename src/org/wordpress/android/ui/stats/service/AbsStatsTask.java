package org.wordpress.android.ui.stats.service;

import android.content.ContentResolver;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

/**
 * Created by nbradbury on 2/25/14.
 * base class for all stats update tasks - code for each task was originally created
 * by Xtreme and lived in StatsRestHelper.java, then split into separate classes and
 * refactored by nbradbury
 */
abstract class AbsStatsTask implements Runnable {
    private boolean mIsCompleted;
    static final long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;

    /*
     * descendants must implement this to parse a successful rest response
     */
    abstract void parseResponse(JSONObject response);

    abstract String getTaskName();

    /*
     * response & error listeners used for all rest client calls made by AbsStatsTask descendants
     */
    final RestRequest.Listener responseListener = new RestRequest.Listener() {
        @Override
        public void onResponse(JSONObject response) {
            parseResponse(response);
            doCompleted();
        }
    };
    final RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            AppLog.e(AppLog.T.STATS, getTaskName() + " failed", error);
            doCompleted();
        }
    };

    ContentResolver getContentResolver() {
        return WordPress.getContext().getContentResolver();
    }

    private void doCompleted() {
        mIsCompleted = true;
    }

    /*
     * each task calls this after making a rest request to ensure the ThreadPoolExecutor
     * used by StatsService doesn't immediately move to the next task - necessary since
     * rest requests are processed in a separate thread - without this, the executor
     * would believe the task completed as soon as the rest request was made (before
     * the response was received)
     */
    synchronized void waitForResponse() {
        if (mIsCompleted)
            return;
        AppLog.d(AppLog.T.STATS, "waiting for " + getTaskName());
        while (!mIsCompleted) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                AppLog.e(AppLog.T.STATS, e);
                return;
            }
        }
        AppLog.d(AppLog.T.STATS, "completed " + getTaskName());
    }
}
