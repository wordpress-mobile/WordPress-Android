package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.util.StatUtils;

/**
 * A model to represent a geoview stat.
 */
public class StatsGeoview {

    private String mBlogId;
    private long mDate;
    private String mCountry;
    private int mViews;
    private String mImageUrl;

    public StatsGeoview(String blogId, long date, String country, int views, String imageUrl) {
        this.mBlogId = blogId;
        this.mDate = date;
        this.mCountry = country;
        this.mViews = views;
        this.mImageUrl = imageUrl;
    }

    public StatsGeoview(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setDate(StatUtils.toMs(result.getString("date")));
        setCountry(result.getString("country"));
        setViews(result.getInt("views"));
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

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(String country) {
        this.mCountry = country;
    }

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.mImageUrl = imageUrl;
    }
}
