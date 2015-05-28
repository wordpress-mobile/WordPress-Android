package org.wordpress.android.ui.stats.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class InsightsPopularModel implements Serializable {
    private int mHighestHour;
    private int mHighestDayOfWeek;
    private Double mHighestDayPercent;
    private Double mHighestHourPercent;
    private String mBlogID;

    public InsightsPopularModel(String blogID, JSONObject response) throws JSONException {
        this.setBlogID(blogID);
        this.mHighestDayOfWeek = response.getInt(("highest_day_of_week"));
        this.mHighestHour = response.getInt(("highest_hour"));
        this.mHighestDayPercent = response.getDouble("highest_day_percent");
        this.mHighestHourPercent = response.getDouble("highest_hour_percent");
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
