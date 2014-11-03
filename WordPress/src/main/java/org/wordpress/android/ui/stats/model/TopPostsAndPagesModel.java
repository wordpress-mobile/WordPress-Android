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
    private String date;
    private String blogID;
    private List<TopPostModel> topPostsAndPages;

    public TopPostsAndPagesModel(String blogID, JSONObject response) throws JSONException {
        this.blogID = blogID;
        this.period = response.getString("period");
        this.date = response.getString("date");
        JSONArray postViewsArray;
        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject == null) {
            return;
        }

        Iterator<String> keys = jDaysObject.keys();
        if (keys.hasNext()) {
            String key = keys.next();
            JSONObject jDateObject = jDaysObject.getJSONObject(key);
            postViewsArray = jDateObject.getJSONArray("postviews");
        } else {
            postViewsArray = new JSONArray();
        }

        ArrayList<TopPostModel> list = new ArrayList<TopPostModel>(postViewsArray.length());

        for (int i=0; i < postViewsArray.length(); i++) {
            try {
                JSONObject postObject = postViewsArray.getJSONObject(i);
                TopPostModel currentModel = new TopPostModel(blogID, postObject);
                list.add(currentModel);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, "Unexpected TopPostModel object in top posts and pages array" +
                        "at position " + i + " Response: " + response.toString(), e);
            }
        }
        this.topPostsAndPages = list;
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

    public List<TopPostModel> getTopPostsAndPages() {
        return topPostsAndPages;
    }
}
