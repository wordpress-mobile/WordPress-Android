package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ReferrersModel implements Serializable {
    private String period;
    private String date;
    private String blogID;
    private int otherViews;
    private int totalViews;
    private List<ReferrerGroupModel> groups;

    public ReferrersModel(String blogID, JSONObject response) throws JSONException {
        this.blogID = blogID;
        this.period = response.getString("period");
        this.date = response.getString("date");

        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject == null || jDaysObject.length() == 0) {
            //FIXME: ???
            return;
        }

        JSONArray jGroupsArray;
        // Read the first day
        Iterator<String> keys = jDaysObject.keys();
        String key = keys.next();
        JSONObject firstDayObject = jDaysObject.getJSONObject(key);
        this.otherViews = firstDayObject.optInt("other_views");
        this.totalViews = firstDayObject.optInt("total_views");
        jGroupsArray = firstDayObject.getJSONArray("groups");

        if (jGroupsArray != null) {
            groups = new ArrayList<ReferrerGroupModel>(jGroupsArray.length());
            for (int i = 0; i < jGroupsArray.length(); i++) {
                try {
                    JSONObject currentGroupJSON = jGroupsArray.getJSONObject(i);
                    ReferrerGroupModel currentGroupModel = new ReferrerGroupModel(blogID, date, currentGroupJSON);
                    groups.add(currentGroupModel);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Unexpected ReferrerGroupModel object referrers" +
                            "at position " + i + " Response: " + response.toString(), e);
                }
            }
        }
    }

    public String getBlogID() {
        return blogID;
    }

    public void setBlogID(String blogID) {
        this.blogID = blogID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public List<ReferrerGroupModel> getGroups() {
        return this.groups;
    }
}
