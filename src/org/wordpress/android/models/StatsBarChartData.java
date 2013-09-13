package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;

import org.wordpress.android.ui.stats.StatsBarChartUnit;

/**
 * A model to represent the bar chart data.
 * <p>
 * The bar chart unit is either:
 * <ul>
 *  <li> "DAY" where the date looks like "2013-09-12"
 *  <li> "WEEK" where the date looks like "2013W26"
 *  <li> "MONTH" where the date looks like "2013-09-01"
 * </ul>
 * </p>
 */
public class StatsBarChartData {

    private String mBlogId;
    private String mDate;
    private int mViews;
    private int mVisitors;
    private StatsBarChartUnit mBarChartUnit;

    public StatsBarChartData(String blogId, StatsBarChartUnit unit, JSONArray result) throws JSONException {
        setBlogId(blogId);
        setBarChartUnit(unit);
        setDate(result.getString(0));
        setViews(result.getInt(1));
        setVisitors(result.getInt(2));
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

    public StatsBarChartUnit getBarChartUnit() {
        return mBarChartUnit;
    }

    public void setBarChartUnit(StatsBarChartUnit unit) {
        this.mBarChartUnit = unit;
    }

}
