package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class TopPostsAndPagesModel implements Serializable {
    private String period;
    private String days;
    private String date;
    private String blogID;


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

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public List<TopPostModel> getTopPostsAndPages() {
        JSONArray jArray;
        String decodedString = StringUtils.unescapeHTML(this.getDays() != null ? this.getDays() : "{}");
        try {
            JSONObject jDaysObject = new JSONObject(decodedString);
            Iterator<String> keys = jDaysObject.keys();
            if (keys.hasNext()) {
                String key = keys.next();
                JSONObject jDateObject = jDaysObject.getJSONObject(key);
                jArray = jDateObject.getJSONArray("postviews");
            } else {
                jArray = new JSONArray();
            }

            ArrayList<TopPostModel> list = new ArrayList<TopPostModel>(jArray.length());

            for (int i=0; i < jArray.length(); i++) {
                try {
                    JSONObject postObject = jArray.getJSONObject(i);
                    TopPostModel currentModel = new TopPostModel(blogID, postObject);
                    list.add(currentModel);
                } catch (JSONException e) {
                    AppLog.i(AppLog.T.NOTIFS, "Unexpected object in top posts and pages array.");
                }
            }
            return  list;
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, e);
            return new ArrayList<TopPostModel>(0);
        }
    }
}
