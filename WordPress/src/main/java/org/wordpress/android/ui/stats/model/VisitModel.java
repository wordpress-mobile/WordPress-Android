package org.wordpress.android.ui.stats.model;

import java.io.Serializable;

public class VisitModel implements Serializable {

    private int mViews;
    private int mReblogs;
    private int mLikes;
    private int mVisitors;
    private int mComments;
    private String mPeriod;
    private String mBlogID;

    public String getBlogID() {
        return mBlogID;
    }

    public void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }

    public int getReblogs() {
        return mReblogs;
    }

    public void setReblogs(int reblogs) {
        this.mReblogs = reblogs;
    }

    public int getLikes() {
        return mLikes;
    }

    public void setLikes(int likes) {
        this.mLikes = likes;
    }

    public int getVisitors() {
        return mVisitors;
    }

    public void setVisitors(int visitors) {
        this.mVisitors = visitors;
    }

    public int getComments() {
        return mComments;
    }

    public void setComments(int comments) {
        this.mComments = comments;
    }

    public String getPeriod() {
        return mPeriod;
    }

    public void setPeriod(String period) {
        this.mPeriod = period;
    }

}
