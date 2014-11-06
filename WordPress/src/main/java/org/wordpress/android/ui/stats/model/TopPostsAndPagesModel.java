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
    private String mPeriod;
    private String mDate;
    private String mBlogID;
    private List<TopPostModel> mTopPostsAndPages;

    public TopPostsAndPagesModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mPeriod = response.getString("period");
        this.mDate = response.getString("date");
        JSONArray postViewsArray = null;
        JSONObject jDaysObject = response.getJSONObject("days");
        if (jDaysObject.length() == 0) {
            throw new JSONException("Invalid document returned from the REST API");
        }

        Iterator<String> keys = jDaysObject.keys();
        if (keys.hasNext()) {
            String key = keys.next();
            JSONObject jDateObject = jDaysObject.optJSONObject(key); // This could be an empty array on site with low traffic
            postViewsArray = (jDateObject != null) ? jDateObject.getJSONArray("postviews") : null;
        }

        if (postViewsArray == null) {
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
        this.mTopPostsAndPages = list;
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

    public List<TopPostModel> getTopPostsAndPages() {
        return mTopPostsAndPages;
    }
}
