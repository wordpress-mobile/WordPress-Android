package org.wordpress.android.ui.stats.tasks;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
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
        WordPress.restClient.getStatsSummary(mBlogId, responseListener, errorListener);
        waitForResponse();
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response != null)
            StatUtils.saveSummary(mBlogId, response);
    }
}
