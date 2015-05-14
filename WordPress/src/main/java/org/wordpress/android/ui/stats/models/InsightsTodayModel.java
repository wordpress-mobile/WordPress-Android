package org.wordpress.android.ui.stats.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class InsightsTodayModel implements Serializable {

    private String mBlogID;
    private String mDate;
    private String period;
    private int visitors;
    private int views;
    private int likes;
    private int reblogs;
    private int comments;
    private int followers;

    public InsightsTodayModel(String blogID, JSONObject response) throws JSONException {
        this.setBlogID(blogID);
        this.mDate = response.getString("date");
        this.period = response.getString("period");
        this.views = response.getInt(("views"));
        this.visitors = response.getInt(("visitors"));
        this.likes = response.getInt(("likes"));
        this.reblogs = response.getInt(("reblogs"));
        this.comments = response.getInt(("comments"));
        this.followers = response.getInt(("followers"));
    }

    public String getBlogID() {
        return mBlogID;
    }

    private void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public int getReblogs() {
        return reblogs;
    }

    public int getComments() {
        return comments;
    }

    public int getFollowers() {
        return followers;
    }

    public int getLikes() {
        return likes;
    }

    public int getViews() {
        return views;
    }

    public int getVisitors() {
        return visitors;
    }
}
