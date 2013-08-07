package org.wordpress.android.models;

public class StatsTotalsFollowersAndShares {

    private int mPosts;
    private int mCategories;
    private int mTags;
    private int mFollowers;
    private int mComments;
    private int mShares;
    private String mDate;
    
    public StatsTotalsFollowersAndShares(int posts, int categories, int tags, int followers, int comments, int shares, String date) {
        this.setPosts(posts);
        this.setCategories(categories);
        this.setTags(tags);
        this.setFollowers(followers);
        this.setComments(comments);
        this.setShares(shares);
        this.setDate(date);
    }

    public int getPosts() {
        return mPosts;
    }

    public void setPosts(int posts) {
        this.mPosts = posts;
    }

    public int getCategories() {
        return mCategories;
    }

    public void setCategories(int categories) {
        this.mCategories = categories;
    }

    public int getTags() {
        return mTags;
    }

    public void setTags(int tags) {
        this.mTags = tags;
    }

    public int getFollowers() {
        return mFollowers;
    }

    public void setFollowers(int followers) {
        this.mFollowers = followers;
    }

    public int getComments() {
        return mComments;
    }

    public void setComments(int comments) {
        this.mComments = comments;
    }

    public int getShares() {
        return mShares;
    }

    public void setShares(int shares) {
        this.mShares = shares;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }
    
    
    
}
