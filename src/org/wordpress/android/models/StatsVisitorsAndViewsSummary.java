
package org.wordpress.android.models;

public class StatsVisitorsAndViewsSummary {

    private int mVisitorsToday;
    private int mViewsToday;
    private int mVisitorsBestEver;
    private int mViewsAllTime;
    private int mCommentsAllTime;
    private String mDate;

    public StatsVisitorsAndViewsSummary(int visitorsToday, int viewsToday, int visitorsBestEver, int viewsAllTime, int commentsAllTime, String date) {
        this.setVisitorsToday(visitorsToday);
        this.setViewsToday(viewsToday);
        this.setVisitorsBestEver(visitorsBestEver);
        this.setViewsAllTime(viewsAllTime);
        this.setCommentsAllTime(commentsAllTime);
        this.setDate(date);
    }

    public int getVisitorsToday() {
        return mVisitorsToday;
    }

    public void setVisitorsToday(int visitorsToday) {
        this.mVisitorsToday = visitorsToday;
    }

    public int getViewsToday() {
        return mViewsToday;
    }

    public void setViewsToday(int viewsToday) {
        this.mViewsToday = viewsToday;
    }

    public int getVisitorsBestEver() {
        return mVisitorsBestEver;
    }

    public void setVisitorsBestEver(int visitorsBestEver) {
        this.mVisitorsBestEver = visitorsBestEver;
    }

    public int getViewsAllTime() {
        return mViewsAllTime;
    }

    public void setViewsAllTime(int viewsAllTime) {
        this.mViewsAllTime = viewsAllTime;
    }

    public int getCommentsAllTime() {
        return mCommentsAllTime;
    }

    public void setCommentsAllTime(int commentsAllTime) {
        this.mCommentsAllTime = commentsAllTime;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

}
