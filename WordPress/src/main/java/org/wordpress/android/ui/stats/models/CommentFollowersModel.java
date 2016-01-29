package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class CommentFollowersModel extends BaseStatsModel {
    private String mBlogID;
    private int mPage;
    private int mPages;
    private int mTotal;
    private List<SingleItemModel> mPosts;

    public CommentFollowersModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mPage = response.getInt("page");
        this.mPages = response.getInt("pages");
        this.mTotal = response.getInt("total");

        JSONArray postsJSONArray = response.optJSONArray("posts");
        if (postsJSONArray != null) {
            mPosts = new ArrayList<>(postsJSONArray.length());
            for (int i = 0; i < postsJSONArray.length(); i++) {
                JSONObject currentPostJSON = postsJSONArray.getJSONObject(i);
                String postId = String.valueOf(currentPostJSON.getInt("id"));
                String title = currentPostJSON.getString("title");
                int followers = currentPostJSON.getInt("followers");
                String url = currentPostJSON.getString("url");
                SingleItemModel currentPost = new SingleItemModel(blogID, null, postId, title, followers, url, null);
                mPosts.add(currentPost);
            }
        }
    }

    public String getBlogID() {
        return mBlogID;
    }

    public void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public List<SingleItemModel> getPosts() {
        return this.mPosts;
    }

    public int getTotal() {
        return mTotal;
    }

    public int getPage() {
        return mPage;
    }

    public int getPages() {
        return mPages;
    }

}
