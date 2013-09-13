package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.util.StatUtils;

/**
 * A model to represent a top post or page stat
 */
public class StatsTopPostsAndPages {

    private String mBlogId;
    private long mDate;
    private int mPostId;
    private String mTitle;
    private int mViews;
    private String mUrl;

    public StatsTopPostsAndPages(String blogId, long date, int postId, String title, int views, String url) {
        this.mBlogId = blogId;
        this.mDate = date;
        this.mPostId = postId;
        this.mTitle = title;
        this.mViews = views;
        this.mUrl = url;
    }

    public StatsTopPostsAndPages(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setDate(StatUtils.toMs(result.getString("date")));
        setPostId(result.getInt("postId"));
        setTitle(result.getString("title"));
        setViews(result.getInt("views"));
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

    public int getPostId() {
        return mPostId;
    }

    public void setPostId(int postId) {
        this.mPostId = postId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }
}
