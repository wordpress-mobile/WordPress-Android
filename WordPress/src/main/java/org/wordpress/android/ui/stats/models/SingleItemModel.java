package org.wordpress.android.ui.stats.models;

import org.wordpress.android.ui.stats.StatsUtils;

import java.io.Serializable;

/*
* A model to represent a SINGLE stats item
*/
public class SingleItemModel implements Serializable {
    private String mBlogID;
    private String mItemID;
    private long mDate;
    private String mTitle;
    private int mTotals;
    private String mUrl;
    private String mIcon;

    public SingleItemModel(String blogId, String date, String itemID, String title, int totals, String url, String icon) {
       this(blogId, StatsUtils.toMs(date), itemID, title, totals, url, icon);
    }

    public SingleItemModel(String blogId, long date, String itemID, String title, int totals, String url, String icon) {
        this.mBlogID = blogId;
        this.mItemID = itemID;
        this.mTitle = title;
        this.mTotals = totals;
        this.mUrl = url;
        this.mDate = date;
        this.mIcon = icon;
    }

    public String getBlogID() {
        return mBlogID;
    }

    public String getItemID() {
        return mItemID;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getTotals() {
        return mTotals;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getIcon() {
        return mIcon;
    }

    public long getDate() {
        return mDate;
    }

}
