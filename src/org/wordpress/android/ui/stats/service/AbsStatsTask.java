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

    /*
     * descendants must implement this to parse a successful rest response - note that this
     * is called from a non-UI thread
     */
    abstract void parseResponse(JSONObject response);

    abstract String getTaskName();

    /*
     * response & error listeners used for all rest client calls made by AbsStatsTask descendants
     */
    final RestRequest.Listener responseListener = new RestRequest.Listener() {
        @Override
        public void onResponse(final JSONObject response) {
            AppLog.d(AppLog.T.STATS, getTaskName() + " response");
            new Thread() {
                @Override
                public void run() {
                    parseResponse(response);
                }
            }.start();
        }
    };
    final RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            AppLog.e(AppLog.T.STATS, getTaskName() + " failed", error);
        }
    };

    ContentResolver getContentResolver() {
        return WordPress.getContext().getContentResolver();
    }
}
