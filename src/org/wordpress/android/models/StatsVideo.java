package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.util.StatUtils;

/**
 * A model to represent a video stat
 */
public class StatsVideo {

    private String mBlogId;
    private long mDate;
    private int mVideoId;
    private String mName;
    private int mPlays;
    private String mUrl;

    public StatsVideo(String blogId, long date, int videoId, String name, int plays, String url) {
        this.mBlogId = blogId;
        this.mDate = date;
        this.mVideoId = videoId;
        this.mName = name;
        this.mPlays = plays;
        this.mUrl = url;
    }

    public StatsVideo(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setDate(StatUtils.toMs(result.getString("date")));
        setVideoId(result.getInt("videoId"));
        setName(result.getString("name"));
        setPlays(result.getInt("plays"));
        setUrl(result.getString("url"));
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

    public int getVideoId() {
        return mVideoId;
    }

    public void setVideoId(int videoId) {
        this.mVideoId = videoId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getPlays() {
        return mPlays;
    }

    public void setPlays(int plays) {
        this.mPlays = plays;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }
}
