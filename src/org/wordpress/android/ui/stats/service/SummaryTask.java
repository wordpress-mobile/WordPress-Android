package org.wordpress.android.ui.stats.service;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StringUtils;

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
        WordPress.getRestClientUtils().getStatsSummary(mBlogId, responseListener, errorListener);
    }

    @Override
    void parseResponse(JSONObject response) {
        if (response == null)
            return;

        // save summary, then send broadcast that they've changed
        StatUtils.saveSummary(mBlogId, response);
        StatsSummary stats = StatUtils.getSummary(mBlogId);
        if (stats != null) {
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
            Intent intent = new Intent(StatsService.ACTION_STATS_SUMMARY_UPDATED);
            intent.putExtra(StatsService.STATS_SUMMARY_UPDATED_EXTRA, stats);
            lbm.sendBroadcast(intent);
        }
    }
}
