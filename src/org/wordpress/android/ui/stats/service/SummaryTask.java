package org.wordpress.android.ui.stats.service;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Created by nbradbury on 2/25/14.
 */
class SummaryTask extends AbsStatsTask {
    private final String mBlogId;

    public SummaryTask(final String blogId) {
        mBlogId = StringUtils.notNullStr(blogId);
    }

    @Override
    String getTaskName() {
        return "SummaryTask";
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
