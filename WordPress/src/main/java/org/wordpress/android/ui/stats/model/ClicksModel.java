package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ClicksModel implements Serializable {
    private String period;
    private String date;
    private String blogID;
    private int otherViews;
    private int totalViews;
    private List<ClickGroupModel> clickGroups;

    public ClicksModel(String blogID, JSONObject response) throws JSONException {
        this.blogID = blogID;
        this.period = response.getString("period");
        this.date = response.getString("date");

        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        JSONArray jClickGroupsArray;
        // Read the first day
        Iterator<String> keys = jDaysObject.keys();
        String key = keys.next();
        JSONObject firstDayObject = jDaysObject.getJSONObject(key);
        this.otherViews = firstDayObject.getInt("other_clicks");
        this.totalViews = firstDayObject.getInt("total_clicks");
        jClickGroupsArray = firstDayObject.optJSONArray("clicks");

        if (jClickGroupsArray != null) {
            clickGroups = new ArrayList<ClickGroupModel>(jClickGroupsArray.length());
            for (int i = 0; i < jClickGroupsArray.length(); i++) {
                try {
                    JSONObject currentGroupJSON = jClickGroupsArray.getJSONObject(i);
                    ClickGroupModel currentGroupModel = new ClickGroupModel(blogID, date, currentGroupJSON);
                    clickGroups.add(currentGroupModel);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Unexpected ClickGroupModel object " +
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

    public List<ClickGroupModel> getClickGroups() {
        return this.clickGroups;
    }
}
