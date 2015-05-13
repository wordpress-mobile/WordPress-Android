package org.wordpress.android.ui.stats.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class InsightsAllTimeModel implements Serializable {

    private String mBlogID;
    private String mDate;
    private int visitors;
    private int views;
    private int posts;
    private String views_best_day;
    private int views_best_day_total;


    public InsightsAllTimeModel(String blogID, JSONObject response) throws JSONException {
        this.setBlogID(blogID);
        this.mDate = response.getString("date");
        JSONObject stats = response.getJSONObject("stats");
        this.posts = stats.getInt(("posts"));
        this.visitors = stats.getInt(("visitors"));
        this.views = stats.getInt(("views"));
        this.views_best_day = stats.getString(("views_best_day"));
        this.views_best_day_total = stats.getInt("views_best_day_total");
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
}
