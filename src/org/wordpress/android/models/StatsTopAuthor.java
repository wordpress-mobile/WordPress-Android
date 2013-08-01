
package org.wordpress.android.models;

public class StatsTopAuthor {

    private String mBlogId;
    private long mDate;
    private int mUserId;
    private String mName;
    private int mViews;
    private String mImageUrl;

    public StatsTopAuthor(String blogId, long date, int userId, String name, int views, String imageUrl) {
        this.mBlogId = blogId;
        this.mDate = date;
        this.mUserId = userId;
        this.mName = name;
        this.mViews = views;
        this.mImageUrl = imageUrl;
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public long getDate() {
        return mDate;
    }

    public void setDate(long date) {
        this.mDate = date;
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

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.mImageUrl = imageUrl;
    }
}
