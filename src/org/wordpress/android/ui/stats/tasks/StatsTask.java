package org.wordpress.android.ui.stats.tasks;

import android.content.ContentResolver;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

/**
 * Created by nbradbury on 2/25/14.
 */
public abstract class StatsTask implements Runnable {
    private boolean mIsCompleted;

    /*
     * descendants must implement this to parse a successful rest response
     */
    abstract void parseResponse(JSONObject response);

    /*
     * response & error listeners used for all rest client calls made by StatsTask descendants
     */
    protected RestRequest.Listener responseListener = new RestRequest.Listener() {
        @Override
        public void onResponse(JSONObject response) {
            parseResponse(response);
            doCompleted();
        }
    };
    protected RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            AppLog.e(AppLog.T.STATS, error);
            doCompleted();
        }
    };

    protected ContentResolver getContentResolver() {
        return WordPress.getContext().getContentResolver();
    }

    private void doCompleted() {
        mIsCompleted = true;
    }

    protected synchronized void waitForResponse() {
        while (!mIsCompleted) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
        }
    }
}
