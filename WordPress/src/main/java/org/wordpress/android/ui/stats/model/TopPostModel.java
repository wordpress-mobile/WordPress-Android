package org.wordpress.android.ui.stats.model;

import org.json.JSONException;
import org.json.JSONObject;

 /*
 * A model to represent a SINGLE post or page stat
 */
public class TopPostModel {
    private String mBlogId;
    private String mPostId;
    private String mTitle;
    private int mViews;
    private String mUrl;

    public TopPostModel(String blogId, String postId, String title, int views, String url) {
        this.mBlogId = blogId;
        this.mPostId = postId;
        this.mTitle = title;
        this.mViews = views;
        this.mUrl = url;
    }

    public TopPostModel(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setPostId(result.getString("id"));
        setTitle(result.getString("title"));
        setViews(result.getInt("views"));
        setUrl(result.getString("href"));
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public String getPostId() {
        return mPostId;
    }

    public void setPostId(String postId) {
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
