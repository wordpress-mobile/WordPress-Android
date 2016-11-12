package org.wordpress.android.ui.stats.models;

import org.json.JSONObject;

public class InsightsPopularModel extends BaseStatsModel {
    private final int mHighestHour;
    private final int mHighestDayOfWeek;
    private final Double mHighestDayPercent;
    private final Double mHighestHourPercent;
    private String mBlogID;

    public InsightsPopularModel(String blogID, JSONObject response) {
        this.setBlogID(blogID);
        this.mHighestDayOfWeek = response.optInt("highest_day_of_week");
        this.mHighestHour = response.optInt("highest_hour");
        this.mHighestDayPercent = response.optDouble("highest_day_percent");
        this.mHighestHourPercent = response.optDouble("highest_hour_percent");
    }

    public String getBlogID() {
        return mBlogID;
    }

    private void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public int getHighestHour() {
        return mHighestHour;
    }

    public int getHighestDayOfWeek() {
        return mHighestDayOfWeek;
    }

    public Double getHighestDayPercent() {
        return mHighestDayPercent;
    }

    public Double getHighestHourPercent() {
        return mHighestHourPercent;
    }
}
