package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class CommentsModel implements Serializable {
    private String mDate;
    private String mBlogID;
    private int monthlyComments;
    private int totalComments;
    private String mostActiveDay;
    private String mostActiveTime;
    private TopPostModel mostCommentedPost;

    private List<TopPostModel> mPosts;
    private List<AuthorModel> mAuthors;

    public CommentsModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mDate = response.getString("date");

        this.monthlyComments = response.getInt("monthly_comments");
        this.totalComments = response.getInt("total_comments");
        this.mostActiveDay = response.getString("most_active_day");
        this.mostActiveTime = response.getString("most_active_time");


        JSONArray postsJSONArray = response.optJSONArray("posts");
        if (postsJSONArray != null) {
            mPosts = new ArrayList<TopPostModel>(postsJSONArray.length());
            for (int i = 0; i < postsJSONArray.length(); i++) {
                JSONObject currentPostJSON = postsJSONArray.getJSONObject(i);
                String postId = String.valueOf(currentPostJSON.getInt("id"));
                String title = currentPostJSON.getString("name");
                int views = currentPostJSON.getInt("comments");
                String url = currentPostJSON.getString("link");
                TopPostModel currentPost = new TopPostModel(blogID, postId, title, views, url);
                mPosts.add(currentPost);
            }
        }

        JSONArray authorsJSONArray = response.optJSONArray("authors");
        if (authorsJSONArray != null) {
            mAuthors = new ArrayList<AuthorModel>(authorsJSONArray.length());
            for (int i = 0; i < authorsJSONArray.length(); i++) {
                JSONObject currentAuthorJSON = authorsJSONArray.getJSONObject(i);
                String name = currentAuthorJSON.getString("name");
                int comments = currentAuthorJSON.getInt("comments");
                String url = currentAuthorJSON.getString("link");
                String gravatar = currentAuthorJSON.getString("gravatar");
                AuthorModel currentAuthor = new AuthorModel(blogID, mDate, url, name, gravatar, comments, null);
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

    public List<TopPostModel> getPosts() {
        return this.mPosts;
    }
    public List<AuthorModel> getAuthors() {
        return this.mAuthors;
    }
}
