
package org.wordpress.android.models;

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
