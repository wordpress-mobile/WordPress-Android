package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.stats.StatsConstants;

import java.util.ArrayList;
import java.util.List;


public class CommentsModel extends BaseStatsModel {
    private String mDate;
    private String mBlogID;
    private int mMonthlyComments;
    private int mTotalComments;
    private String mMostActiveDay;
    private String mMostActiveTime;
    private SingleItemModel mMostCommentedPost;

    private List<PostModel> mPosts;
    private List<AuthorModel> mAuthors;

    public CommentsModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mDate = response.getString("date");

        this.mMonthlyComments = response.getInt("monthly_comments");
        this.mTotalComments = response.getInt("total_comments");
        this.mMostActiveDay = response.getString("most_active_day");
        this.mMostActiveTime = response.getString("most_active_time");


        JSONArray postsJSONArray = response.optJSONArray("posts");
        if (postsJSONArray != null) {
            mPosts = new ArrayList<>(postsJSONArray.length());
            for (int i = 0; i < postsJSONArray.length(); i++) {
                JSONObject currentPostJSON = postsJSONArray.getJSONObject(i);
                String itemID = String.valueOf(currentPostJSON.getInt("id"));
                String name = currentPostJSON.getString("name");
                int totals = currentPostJSON.getInt("comments");
                String link = currentPostJSON.getString("link");
                PostModel currentPost = new PostModel(blogID, mDate, itemID, name, totals, link, StatsConstants.ITEM_TYPE_POST);
                mPosts.add(currentPost);
            }
        }

        JSONArray authorsJSONArray = response.optJSONArray("authors");
        if (authorsJSONArray != null) {
            mAuthors = new ArrayList<>(authorsJSONArray.length());
            for (int i = 0; i < authorsJSONArray.length(); i++) {
                JSONObject currentAuthorJSON = authorsJSONArray.getJSONObject(i);
                String name = currentAuthorJSON.getString("name");
                int comments = currentAuthorJSON.getInt("comments");
                String url = currentAuthorJSON.getString("link");
                String gravatar = currentAuthorJSON.getString("gravatar");
                JSONObject followData = currentAuthorJSON.optJSONObject("follow_data");
                AuthorModel currentAuthor = new AuthorModel(blogID, mDate, url, name, gravatar, comments, followData);
                mAuthors.add(currentAuthor);
            }
        }
    }

    public String getBlogID() {
        return mBlogID;
    }

    public void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public List<PostModel> getPosts() {
        return this.mPosts;
    }

    public List<AuthorModel> getAuthors() {
        return this.mAuthors;
    }

    public int getTotalComments() {
        return mTotalComments;
    }

    public int getMonthlyComments() {
        return mMonthlyComments;
    }

    public String getMostActiveDay() {
        return mMostActiveDay;
    }

    public String getMostActiveTime() {
        return mMostActiveTime;
    }

    public SingleItemModel getMostCommentedPost() {
        return mMostCommentedPost;
    }
}
