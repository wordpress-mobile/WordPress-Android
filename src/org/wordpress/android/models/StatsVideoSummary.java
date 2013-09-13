package org.wordpress.android.models;

/**
 * A model representing the summary for video views
 */
public class StatsVideoSummary {

    private String mTimeframe;
    private int mPlays;
    private int mImpressions;
    private int mMinutes;
    private String mBandwidth;
    private String mDate;

    public StatsVideoSummary(String timeframe, int plays, int impressions, int minutes, String bandwidth, String date) {
        this.setTimeframe(timeframe);
        this.setPlays(plays);
        this.setImpressions(impressions);
        this.setMinutes(minutes);
        this.setBandwidth(bandwidth);
        this.setDate(date);
    }

    public String getTimeframe() {
        return mTimeframe;
    }

    public void setTimeframe(String timeframe) {
        this.mTimeframe = timeframe;
    }

    public int getPlays() {
        return mPlays;
    }

    public void setPlays(int plays) {
        this.mPlays = plays;
    }

    public int getImpressions() {
        return mImpressions;
    }

    public void setImpressions(int impressions) {
        this.mImpressions = impressions;
    }

    public int getMinutes() {
        return mMinutes;
    }

    public void setMinutes(int minutes) {
        this.mMinutes = minutes;
    }

    public String getBandwidth() {
        return mBandwidth;
    }

    public void setBandwidth(String bandwidth) {
        this.mBandwidth = bandwidth;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

}
