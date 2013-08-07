package org.wordpress.android.models;

public class StatsCommentsSummary {

    private int mCommentsPerMonth;
    private int mCommentsTotal;
    private String mRecentMostActiveDay;
    private String recentMostActiveTime;
    private String mRecentMostActivePost;
    private String mRecentMostActivePostUrl;
    private String mDate;
    
    public StatsCommentsSummary(int commentsPerMonth, int commentsTotal, String recentMostActiveDay, String recentMostActiveTime, String recentMostActivePost, String recentMostActivePostUrl, String date) {
        this.setCommentsPerMonth(commentsPerMonth);
        this.setCommentsTotal(commentsTotal);
        this.setRecentMostActiveDay(recentMostActiveDay);
        this.setRecentMostActiveTime(recentMostActiveTime);
        this.setRecentMostActivePost(recentMostActivePost);
        this.setRecentMostActivePostUrl(recentMostActivePostUrl);
        this.setDate(date);
    }

    public int getCommentsPerMonth() {
        return mCommentsPerMonth;
    }

    public void setCommentsPerMonth(int commentsPerMonth) {
        this.mCommentsPerMonth = commentsPerMonth;
    }

    public int getCommentsTotal() {
        return mCommentsTotal;
    }

    public void setCommentsTotal(int commentsTotal) {
        this.mCommentsTotal = commentsTotal;
    }

    public String getRecentMostActiveDay() {
        return mRecentMostActiveDay;
    }

    public void setRecentMostActiveDay(String recentMostActiveDay) {
        this.mRecentMostActiveDay = recentMostActiveDay;
    }

    public String getRecentMostActiveTime() {
        return recentMostActiveTime;
    }

    public void setRecentMostActiveTime(String recentMostActiveTime) {
        this.recentMostActiveTime = recentMostActiveTime;
    }

    public String getRecentMostActivePost() {
        return mRecentMostActivePost;
    }

    public void setRecentMostActivePost(String recentMostActivePost) {
        this.mRecentMostActivePost = recentMostActivePost;
    }

    public String getRecentMostActivePostUrl() {
        return mRecentMostActivePostUrl;
    }

    public void setRecentMostActivePostUrl(String recentMostActivePostUrl) {
        this.mRecentMostActivePostUrl = recentMostActivePostUrl;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }
    
    
}
