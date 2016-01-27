package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class VideoPlaysModel extends BaseStatsModel {
    private String mPeriod;
    private String mDate;
    private String mBlogID;
    private int mOtherPlays;
    private int mTotalPlays;
    private List<SingleItemModel> mPlays;

    public VideoPlaysModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mPeriod = response.getString("period");
        this.mDate = response.getString("date");

        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        // Read the first day
        Iterator<String> keys = jDaysObject.keys();
        String key = keys.next();
        JSONObject firstDayObject = jDaysObject.getJSONObject(key);
        this.mOtherPlays = firstDayObject.getInt("other_plays");
        this.mTotalPlays = firstDayObject.getInt("total_plays");
        JSONArray playsJSONArray = firstDayObject.optJSONArray("plays");

        if (playsJSONArray != null) {
            mPlays = new ArrayList<>(playsJSONArray.length());
            for (int i = 0; i < playsJSONArray.length(); i++) {
                JSONObject currentVideoplaysJSON = playsJSONArray.getJSONObject(i);
                String postId = String.valueOf(currentVideoplaysJSON.getInt("post_id"));
                String title = currentVideoplaysJSON.getString("title");
                int views = currentVideoplaysJSON.getInt("plays");
                String url = currentVideoplaysJSON.getString("url");
                SingleItemModel currentPost = new SingleItemModel(blogID, mDate, postId, title, views, url, null);
                mPlays.add(currentPost);
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

    public String getPeriod() {
        return mPeriod;
    }

    public void setPeriod(String period) {
        this.mPeriod = period;
    }

    public List<SingleItemModel> getPlays() {
        return this.mPlays;
    }

    public int getOtherPlays() {
        return mOtherPlays;
    }

    public int getTotalPlays() {
        return mTotalPlays;
    }
}
