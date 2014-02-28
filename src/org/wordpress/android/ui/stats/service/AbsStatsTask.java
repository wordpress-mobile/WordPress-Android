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
    static final long TWO_DAYS = 2 * 24 * 60 * 60 * 1000;
    private static final long WAIT_TIMEOUT = 20 * 1000;
    private final Object mSyncObject = new Object();

    /*
     * descendants must implement this to send their specific request to the stats api
     */
    abstract void sendRequest();

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
        // send the stats api request
        sendRequest();

        // wait for the request to be completed - without this, the ThreadPoolExecutor
        // in StatsService will immediately move on to the next task
        waitForResponse();
    }

    private void waitForResponse() {
        synchronized (mSyncObject) {
            try {
                mSyncObject.wait(WAIT_TIMEOUT);
            } catch (InterruptedException e) {
                AppLog.w(AppLog.T.STATS, getTaskName() + " interrupted");
            }
        }
    }

    /*
     * called when either (a) the response has been received and parsed, or (b) the request failed
     */
    private void notifyResponseReceived() {
        synchronized (mSyncObject) {
            mSyncObject.notify();
        }
    }

    /*
     * response & error listeners used for all rest client calls made by AbsStatsTask descendants
     */
    final RestRequest.Listener responseListener = new RestRequest.Listener() {
        @Override
        public void onResponse(final JSONObject response) {
            new Thread() {
                @Override
                public void run() {
                    parseResponse(response);
                    notifyResponseReceived();
                }
            }.start();
        }
    };
    final RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            AppLog.e(AppLog.T.STATS, getTaskName() + " failed", error);
            notifyResponseReceived();
        }
    };

    ContentResolver getContentResolver() {
        return WordPress.getContext().getContentResolver();
    }
}
