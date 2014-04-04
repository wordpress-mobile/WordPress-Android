package org.wordpress.android.ui.stats.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import android.content.ContentResolver;
import android.content.Context;

import org.json.JSONObject;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

/**
 * base class for all stats update tasks - code for each task was originally created
 * by Xtreme and lived in StatsRestHelper.java, then split into separate classes and
 * refactored by nbradbury
 */
abstract class AbsStatsTask implements Runnable {
    static final long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;

    /*
     * descendants must implement this to return their specific path to the stats rest api
     */
    abstract String getPath();

    /*
     * descendants must implement this to parse a successful response - note that this
     * is called from a non-UI thread
     */
    abstract void parseResponse(JSONObject response);

    /*
     * task name as it should appear in log messages
     */
    abstract String getTaskName();

    @Override
    public void run() {
        try {
            AppLog.d(T.STATS, getTaskName() + " started");
            // send the stats api request
            JSONObject response = WordPress.getRestClientUtils().getSynchronous(getPath());
            parseResponse(response);
            AppLog.d(T.STATS, getTaskName() + " responded");
        } catch (InterruptedException e) {
            AppLog.e(T.STATS, getTaskName() + " failed", e);
        } catch (ExecutionException e) {
            AppLog.e(T.STATS, getTaskName() + " failed", e);
        } catch (TimeoutException e) {
            AppLog.e(T.STATS, getTaskName() + " failed", e);
        }
    }

    ContentResolver getContentResolver() {
        return getContext().getContentResolver();
    }

    Context getContext() {
        return WordPress.getContext();
    }
}
