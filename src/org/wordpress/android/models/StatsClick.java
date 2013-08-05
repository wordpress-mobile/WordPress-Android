
package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.util.StatUtils;

public class StatsClick {

    private String mBlogId;
    private long mDate;
    private int mClicks;
    private String mUrl;
    private String mImageUrl;

    public StatsClick(String blogId, long date, int clicks, String url, String imageUrl) {
        this.mBlogId = blogId;
        this.mDate = date;
        this.mClicks = clicks;
        this.mUrl = url;
        this.mImageUrl = imageUrl;
    }

    public StatsClick(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setDate(StatUtils.toMs(result.getString("date")));
        setClicks(result.getInt("clicks"));
        setUrl(result.getString("url"));
        if (result.has("imageUrl"))
            setImageUrl(result.getString("imageUrl"));
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

    public int getClicks() {
        return mClicks;
    }

    public void setClicks(int clicks) {
        this.mClicks = clicks;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.mImageUrl = imageUrl;
    }

}
