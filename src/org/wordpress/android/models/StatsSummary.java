
package org.wordpress.android.models;

public class StatsSummary {

    private int mViews;
    private int mComments;
    private int mFavorites;
    private int mReblogs;
    private String mDate;

    public StatsSummary(int views, int comments, int favorites, int reblogs, String date) {
        this.mViews = views;
        this.mComments = comments;
        this.mFavorites = favorites;
        this.mReblogs = reblogs;
        this.mDate = date;
    }

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }

    public int getComments() {
        return mComments;
    }

    public void setComments(int comments) {
        this.mComments = comments;
    }

    public int getFavorites() {
        return mFavorites;
    }

    public void setFavorites(int favorites) {
        this.mFavorites = favorites;
    }

    public int getReblogs() {
        return mReblogs;
    }

    public void setReblogs(int reblogs) {
        this.mReblogs = reblogs;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

}
