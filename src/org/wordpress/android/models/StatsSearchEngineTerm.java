
package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.util.StatUtils;

public class StatsSearchEngineTerm {

    private String mBlogId;
    private long mDate;
    private String mSearch;
    private int mViews;

    public StatsSearchEngineTerm(String blogId, long date, String search, int views) {
        this.mBlogId = blogId;
        this.mDate = date;
        this.mSearch = search;
        this.mViews = views;
    }

    public StatsSearchEngineTerm(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setDate(StatUtils.toMs(result.getString("date")));
        setSearch(result.getString("search"));
        setViews(result.getInt("views"));
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public long getDate() {
        return mDate;
    }

    public void setDate(long date) {
        this.mDate = date;
    }

    public String getSearch() {
        return mSearch;
    }

    public void setSearch(String search) {
        this.mSearch = search;
    }

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }
}
