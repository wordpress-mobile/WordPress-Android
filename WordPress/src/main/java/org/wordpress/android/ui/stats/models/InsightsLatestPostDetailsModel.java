package org.wordpress.android.ui.stats.models;

import org.json.JSONException;
import org.json.JSONObject;


public class InsightsLatestPostDetailsModel extends BaseStatsModel {
    private long mBlogID;
    private int mViews;

    public InsightsLatestPostDetailsModel(long blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mViews = response.getInt("views");
    }

    public long getBlogID() {
        return mBlogID;
    }

    public int getPostViewsCount() {
        return mViews;
    }
}
