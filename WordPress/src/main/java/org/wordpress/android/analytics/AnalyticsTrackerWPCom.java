package org.wordpress.android.analytics;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

import java.util.Map;

public class AnalyticsTrackerWPCom implements AnalyticsTracker.Tracker {
    @Override
    public void track(AnalyticsTracker.Stat stat) {
        track(stat, null);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, Map<String, ?> properties) {
        switch (stat) {
            case READER_LOADED_FRESHLY_PRESSED:
                pingWPComStatsEndpoint("freshly");
                break;
            case READER_OPENED_ARTICLE:
                pingWPComStatsEndpoint("details_page");
                break;
            case READER_ACCESSED:
                pingWPComStatsEndpoint("home_page");
                break;
            default:
                // Do nothing
        }
    }

    @Override
    public void beginSession() {
        // No-op
    }

    @Override
    public void endSession() {
        // No-op
    }

    @Override
    public void refreshMetadata() {
       // No-op
    }

    @Override
    public void clearAllData() {
        // No-op
    }

    @Override
    public void registerPushNotificationToken(String regId) {
        // No-op
    }

    private void pingWPComStatsEndpoint(String statName) {
        Response.Listener<String> listener = new Response.Listener<String>() {
            public void onResponse(String response) {
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                String errMsg = String.format("Error pinging WPCom Stats: %s", volleyError.getMessage());
                AppLog.w(AppLog.T.STATS, errMsg);
            }
        };

        int rnd = (int) (Math.random() * 100000);
        String statsURL = String.format("%s%s%s%s%d", Constants.readerURL_v3,
                "&template=stats&stats_name=", statName, "&rnd=", rnd);
        StringRequest req = new StringRequest(Request.Method.GET, statsURL, listener, errorListener);
        WordPress.requestQueue.add(req);
    }
}
