package org.wordpress.android.ui.stats.tasks;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Created by nbradbury on 2/25/14.
 */
public class SummaryTask extends StatsTask {
    private final String mBlogId;

    public SummaryTask(final String blogId) {
        mBlogId = StringUtils.notNullStr(blogId);
    }

    @Override
    public void run() {
        WordPress.restClient.getStatsSummary(mBlogId,
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        parseResponse(response);
                    }
                },
                new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.STATS, error);
                    }
                });
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response != null)
            StatUtils.saveSummary(mBlogId, response);
    }
}
