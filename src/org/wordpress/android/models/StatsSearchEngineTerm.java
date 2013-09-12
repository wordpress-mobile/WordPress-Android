package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;

import org.wordpress.android.util.StatUtils;

/**
 * A model to represent a search engine term stat
 */
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

    public StatsSearchEngineTerm(String blogId, String date, JSONArray result) throws JSONException {
        setBlogId(blogId);
        setDate(StatUtils.toMs(date));
        setSearch(result.getString(0));
        setViews(result.getInt(1));
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
