package org.wordpress.android.ui.stats.models;

import org.json.JSONException;
import org.json.JSONObject;

public class InsightsAllTimeModel extends BaseStatsModel {

    private String mBlogID;
    private String mDate;
    private int mVisitors;
    private int mViews;
    private int mPosts;
    private String mViewsBestDay;
    private int mViewsBestDayTotal;


    public InsightsAllTimeModel(String blogID, JSONObject response) throws JSONException {
        this.setBlogID(blogID);
        this.mDate = response.getString("date");
        JSONObject stats = response.getJSONObject("stats");
        this.mPosts = stats.optInt("posts");
        this.mVisitors = stats.optInt("visitors");
        this.mViews = stats.optInt("views");
        this.mViewsBestDay = stats.getString("views_best_day");
        this.mViewsBestDayTotal = stats.optInt("views_best_day_total");
    }

    public String getBlogID() {
        return mBlogID;
    }

    private void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public int getVisitors() {
        return mVisitors;
    }

    public int getViews() {
        return mViews;
    }

    public int getPosts() {
        return mPosts;
    }

    public String getViewsBestDay() {
        return mViewsBestDay;
    }

    public int getViewsBestDayTotal() {
        return mViewsBestDayTotal;
    }
}
