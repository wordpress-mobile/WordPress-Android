package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class InsightsLatestPostModel extends BaseStatsModel {
    private String mBlogID;
    private String mPostTitle;
    private String mPostURL;
    private String mPostDate;
    private int mPostID;
    private int mPostViewsCount = Integer.MIN_VALUE;
    private int mPostCommentCount;
    private int mPostLikeCount;
    private int mPostsFound; // if 0 there are no posts on the blog.

    public InsightsLatestPostModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;

        mPostsFound = response.optInt("found", 0);
        if (mPostsFound == 0) {
            // No latest post found!
           return;
        }

        JSONArray postsObject = response.getJSONArray("posts");
        if (postsObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        // Read the first post
        JSONObject firstPostObject = postsObject.getJSONObject(0);

        this.mPostID = firstPostObject.getInt("ID");
        this.mPostTitle = firstPostObject.getString("title");
        this.mPostDate = firstPostObject.getString("date");
        this.mPostURL = firstPostObject.getString("URL");
        this.mPostLikeCount = firstPostObject.getInt("like_count");

        JSONObject discussionObject = firstPostObject.optJSONObject("discussion");
        if (discussionObject != null) {
            this.mPostCommentCount = discussionObject.optInt("comment_count", 0);
        }
    }

    public boolean isLatestPostAvailable() {
        return mPostsFound > 0;
    }

    public String getBlogID() {
        return mBlogID;
    }

    public String getPostDate() {
        return mPostDate;
    }

    public String getPostTitle() {
        return mPostTitle;
    }

    public String getPostURL() {
        return mPostURL;
    }

    public int getPostID() {
        return mPostID;
    }

    public int getPostViewsCount() {
        return mPostViewsCount;
    }

    public void setPostViewsCount(int postViewsCount) {
        this.mPostViewsCount = postViewsCount;
    }

    public int getPostCommentCount() {
        return mPostCommentCount;
    }

    public int getPostLikeCount() {
        return mPostLikeCount;
    }
}
