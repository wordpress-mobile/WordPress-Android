package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ClicksModel extends BaseStatsModel {
    private String mPeriod;
    private String mDate;
    private String mBlogID;
    private int mOtherClicks;
    private int mTotalClicks;
    private List<ClickGroupModel> mClickGroups;

    public ClicksModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mPeriod = response.getString("period");
        this.mDate = response.getString("date");

        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        JSONArray jClickGroupsArray;
        // Read the first day
        Iterator<String> keys = jDaysObject.keys();
        String key = keys.next();
        JSONObject firstDayObject = jDaysObject.getJSONObject(key);
        this.mOtherClicks = firstDayObject.getInt("other_clicks");
        this.mTotalClicks = firstDayObject.getInt("total_clicks");
        jClickGroupsArray = firstDayObject.optJSONArray("clicks");

        if (jClickGroupsArray != null) {
            mClickGroups = new ArrayList<>(jClickGroupsArray.length());
            for (int i = 0; i < jClickGroupsArray.length(); i++) {
                try {
                    JSONObject currentGroupJSON = jClickGroupsArray.getJSONObject(i);
                    ClickGroupModel currentGroupModel = new ClickGroupModel(blogID, mDate, currentGroupJSON);
                    mClickGroups.add(currentGroupModel);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Unexpected ClickGroupModel object " +
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

    public List<ClickGroupModel> getClickGroups() {
        return this.mClickGroups;
    }

    public int getOtherClicks() {
        return mOtherClicks;
    }

    public int getTotalClicks() {
        return mTotalClicks;
    }
}
