package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;


public class FollowersModel extends BaseStatsModel {
    private String mBlogID;
    private int mPage;
    private int mPages;
    private int mTotal;
    private int mTotalEmail;
    private int mTotalWPCom;
    private List<FollowerModel> mSubscribers;

    public FollowersModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mPage = response.getInt("page");
        this.mPages = response.getInt("pages");
        this.mTotal = response.getInt("total");
        this.mTotalEmail = response.getInt("total_email");
        this.mTotalWPCom = response.getInt("total_wpcom");

        JSONArray subscribersJSONArray = response.optJSONArray("subscribers");
        if (subscribersJSONArray != null) {
            mSubscribers = new ArrayList<>(subscribersJSONArray.length());
            for (int i = 0; i < subscribersJSONArray.length(); i++) {
                JSONObject currentAuthorJSON = subscribersJSONArray.getJSONObject(i);
                try {
                    FollowerModel currentFollower = new FollowerModel(mBlogID, currentAuthorJSON);
                    mSubscribers.add(currentFollower);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Unexpected Follower object " +
                            "at position " + i + " Response: " + response.toString(), e);
                }
            }
        }
    }

    public String getBlogID() {
        return mBlogID;
    }

    public void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public List<FollowerModel> getFollowers() {
        return this.mSubscribers;
    }

    public int getTotalEmail() {
        return mTotalEmail;
    }

    public int getTotalWPCom() {
        return mTotalWPCom;
    }

    public int getPage() {
        return mPage;
    }

    public int getPages() {
        return mPages;
    }

    public int getTotal() {
        return mTotal;
    }
}
