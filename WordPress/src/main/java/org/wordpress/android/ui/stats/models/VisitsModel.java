package org.wordpress.android.ui.stats.models;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VisitsModel extends BaseStatsModel {
    private String mFields; // Holds a JSON Object
    private String mUnit;
    private String mDate;
    private String mBlogID;
    private List<VisitModel> mVisits;

    public VisitsModel(String blogID, JSONObject response) throws JSONException {
        this.setBlogID(blogID);
        this.setDate(response.getString("date"));
        this.setUnit(response.getString("unit"));
        this.setFields(response.getJSONArray("fields").toString());

        JSONArray dataJSON;
        try {
            dataJSON = response.getJSONArray("data");
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, this.getClass().getName() + " cannot convert the data field to a JSON array", e);
            dataJSON = new JSONArray();
        }

        if (dataJSON == null || dataJSON.length() == 0) {
            mVisits =  new ArrayList<>(0);
        } else {
            // Read the position/index of each field in the response
            HashMap<String, Integer> columnsMapping = new HashMap<>(6);
            final JSONArray fieldsJSON = getFieldsJSON();
            if (fieldsJSON == null || fieldsJSON.length() == 0) {
                mVisits =  new ArrayList<>(0);
            } else {
                try {
                    for (int i = 0; i < fieldsJSON.length(); i++) {
                        final String field = fieldsJSON.getString(i);
                        columnsMapping.put(field, i);
                    }
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Cannot read the parameter fields from the JSON response." +
                            "Response: " + response.toString(), e);
                    mVisits = new ArrayList<>(0);
                }
            }

            int viewsColumnIndex = columnsMapping.get("views");
            int visitorsColumnIndex = columnsMapping.get("visitors");
            int likesColumnIndex = columnsMapping.get("likes");
            int commentsColumnIndex = columnsMapping.get("comments");
            int periodColumnIndex = columnsMapping.get("period");

            int numPoints = dataJSON.length();
            mVisits = new ArrayList<>(numPoints);

            for (int i = 0; i < numPoints; i++) {
                try {
                    JSONArray currentDayData = dataJSON.getJSONArray(i);
                    VisitModel currentVisitModel = new VisitModel();
                    currentVisitModel.setBlogID(getBlogID());
                    currentVisitModel.setPeriod(currentDayData.getString(periodColumnIndex));
                    currentVisitModel.setViews(currentDayData.getInt(viewsColumnIndex));
                    currentVisitModel.setVisitors(currentDayData.getInt(visitorsColumnIndex));
                    currentVisitModel.setComments(currentDayData.getInt(commentsColumnIndex));
                    currentVisitModel.setLikes(currentDayData.getInt(likesColumnIndex));
                    mVisits.add(currentVisitModel);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Cannot read the Visit item at index " + i
                            + " Response: " + response.toString(), e);
                }
            }
        }
    }

    public List<VisitModel> getVisits() {
        return mVisits;
    }

    public String getBlogID() {
        return mBlogID;
    }

    private void setBlogID(String blogID) {
        this.mBlogID = blogID;
    }

    public String getDate() {
        return mDate;
    }

    private void setDate(String date) {
        this.mDate = date;
    }

    public String getUnit() {
        return mUnit;
    }

    private void setUnit(String unit) {
        this.mUnit = unit;
    }

    private JSONArray getFieldsJSON() {
        JSONArray jArray;
        try {
            String categories = StringUtils.unescapeHTML(this.getFields() != null ? this.getFields() : "[]");
            if (TextUtils.isEmpty(categories)) {
                jArray = new JSONArray();
            } else {
                jArray = new JSONArray(categories);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, this.getClass().getName() + " cannot convert the string to JSON", e);
            return null;
        }
        return jArray;
    }

    private void setFields(String fields) {
        this.mFields = fields;
    }

    private String getFields() {
        return mFields;
    }
}
