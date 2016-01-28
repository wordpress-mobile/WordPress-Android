package org.wordpress.android.ui.stats.models;

import org.json.JSONException;
import org.json.JSONObject;


public class InsightsLatestPostDetailsModel extends BaseStatsModel {
    private String mBlogID;
    private int mViews;

    public InsightsLatestPostDetailsModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mViews = response.getInt("views");
    }

    public String getBlogID() {
        return mBlogID;
    }

    public int getPostViewsCount() {
        return mViews;
    }
}
