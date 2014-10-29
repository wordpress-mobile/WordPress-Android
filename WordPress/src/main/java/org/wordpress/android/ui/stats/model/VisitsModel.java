package org.wordpress.android.ui.stats.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;

public class VisitsModel implements Serializable {
    private String fields; // Holds a JSON Object
    private String unit;
    private String date;
    private String blogID;
    private VisitModel[] visits;

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
            visits =  new VisitModel[0];
        } else {
            // Read the position/index of each field in the response
            HashMap<String, Integer> columnsMapping = new HashMap<String, Integer>(6);
            final JSONArray fieldsJSON = getFieldsJSON();
            if (fieldsJSON == null || fieldsJSON.length() == 0) {
                visits =  new VisitModel[0];
            }
            try {
                for (int i = 0; i < fieldsJSON.length(); i++) {
                    final String field = fieldsJSON.getString(i);
                    columnsMapping.put(field, i);
                }
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, "Cannot read the parameter fields from the JSON response", e);
                visits =  new VisitModel[0];
            }

            int viewsColumnIndex = columnsMapping.get("views");
            int visitorsColumnIndex = columnsMapping.get("visitors");
            int likesColumnIndex = columnsMapping.get("likes");
            int reblogsColumnIndex = columnsMapping.get("reblogs");
            int commentsColumnIndex = columnsMapping.get("comments");
            int periodColumnIndex = columnsMapping.get("period");

            int numPoints = dataJSON.length();
            VisitModel[] visitModels = new VisitModel[numPoints];

            for (int i = 0; i < numPoints; i++) {
                try {
                    JSONArray currentDayData = dataJSON.getJSONArray(i);
                    VisitModel currentVisitModel = new VisitModel();
                    currentVisitModel.setPeriod(currentDayData.getString(periodColumnIndex));
                    currentVisitModel.setViews(currentDayData.getInt(viewsColumnIndex));
                    currentVisitModel.setVisitors(currentDayData.getInt(visitorsColumnIndex));
                    currentVisitModel.setComments(currentDayData.getInt(commentsColumnIndex));
                    currentVisitModel.setLikes(currentDayData.getInt(likesColumnIndex));
                    currentVisitModel.setReblogs(currentDayData.getInt(reblogsColumnIndex));
                    visitModels[i] = currentVisitModel;
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Cannot read the Visit at index " + i, e);
                }
            }
            this.visits = visitModels;
        }
    }

    public VisitModel[] getVisits() {
        return visits;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    private JSONArray getFieldsJSON() {
        JSONArray jArray = null;
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
        this.fields = fields;
    }

    private String getFields() {
        return fields;
    }
}
