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
    void sendRequest() {
        WordPress.restClient.getStatsSummary(mBlogId, responseListener, errorListener);
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response == null)
            return;
        StatUtils.saveSummary(mBlogId, response);
        StatUtils.broadcastSummaryUpdated(StatUtils.getSummary(mBlogId));
    }
}
