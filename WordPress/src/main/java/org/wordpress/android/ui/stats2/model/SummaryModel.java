package org.wordpress.android.ui.stats2.model;

import io.realm.RealmObject;
import io.realm.annotations.Index;

public class SummaryModel extends RealmObject {

    private int followers;
    private int views;
    private int reblog;
    private int like;
    private int visitors;
    private int comments;
    private String period;

    @Index
    private String date;

    @Index
    private String blogID;

    public String getBlogID() {
        return blogID;
    }

    public void setBlogID(String blogID) {
        this.blogID = blogID;
    }


    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getReblog() {
        return reblog;
    }

    public void setReblog(int reblog) {
        this.reblog = reblog;
    }

    public int getLike() {
        return like;
    }

    public void setLike(int like) {
        this.like = like;
    }

    public int getVisitors() {
        return visitors;
    }

    public void setVisitors(int visitors) {
        this.visitors = visitors;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
