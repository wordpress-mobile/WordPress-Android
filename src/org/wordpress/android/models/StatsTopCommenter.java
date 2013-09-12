package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A model to represent a top commenter stat
 */
public class StatsTopCommenter {

    private String mBlogId;
    private int mUserId;
    private String mName;
    private int mComments;
    private String mImageUrl;

    public StatsTopCommenter(String blogId, int userId, String name, int comments, String imageUrl) {
        this.mBlogId = blogId;
        this.mUserId = userId;
        this.mName = name;
        this.mComments = comments;
        this.mImageUrl = imageUrl;
    }

    public StatsTopCommenter(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setUserId(result.getInt("userId"));
        setName(result.getString("name"));
        setComments(result.getInt("comments"));
        if (result.has("imageUrl") && !result.getString("imageUrl").equals("null"))
            setImageUrl(result.getString("imageUrl"));
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public int getUserId() {
        return mUserId;
    }

    public void setUserId(int userId) {
        this.mUserId = userId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getComments() {
        return mComments;
    }

    public void setComments(int comments) {
        this.mComments = comments;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.mImageUrl = imageUrl;
    }

}
