package org.wordpress.android.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A model to represent a most commented post stat  
 */
public class StatsMostCommented {
    private String mBlogId;
    private int mPostId;
    private String mPost;
    private int mComments;
    private String mUrl;

    public StatsMostCommented(String blogId, int postId, String post, int comments, String url) {
        this.mBlogId = blogId;
        this.mPostId = postId;
        this.mPost = post;
        this.mComments = comments;
        this.mUrl = url;
    }

    public StatsMostCommented(String blogId, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setPostId(result.getInt("postId"));
        setPost(result.getString("post"));
        setComments(result.getInt("comments"));
        setUrl(result.getString("url"));
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public int getPostId() {
        return mPostId;
    }

    public void setPostId(int postId) {
        this.mPostId = postId;
    }

    public String getPost() {
        return mPost;
    }

    public void setPost(String post) {
        this.mPost = post;
    }

    public int getComments() {
        return mComments;
    }

    public void setComments(int comments) {
        this.mComments = comments;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }
}
