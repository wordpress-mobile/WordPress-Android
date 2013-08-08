
package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

public class StatsBarChartWeek {

    private String mBlogId;
    private String mDate;
    private int mViews;
    private int mVisitors;

    public StatsBarChartWeek(String blogId, String date, int views, int visitors) {
        this.setBlogId(blogId);
        this.setDate(date);
        this.setViews(views);
        this.setVisitors(visitors);
    }

    public StatsBarChartWeek(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setDate(result.getString("date"));
        setViews(result.getInt("views"));
        setVisitors(result.getInt("visitors"));
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }

    public int getVisitors() {
        return mVisitors;
    }

    public void setVisitors(int visitors) {
        this.mVisitors = visitors;
    }

}
