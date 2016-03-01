package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class TopPostsAndPagesModel extends BaseStatsModel {
    private String mPeriod;
    private String mDate;
    private String mBlogID;
    private List<PostModel> mTopPostsAndPages;

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

        ArrayList<PostModel> list = new ArrayList<>(postViewsArray.length());

        for (int i=0; i < postViewsArray.length(); i++) {
            try {
                JSONObject postObject = postViewsArray.getJSONObject(i);
                String itemID = postObject.getString("id");
                String itemTitle = postObject.getString("title");
                int itemTotal = postObject.getInt("views");
                String itemURL = postObject.getString("href");
                String itemType = postObject.getString("type");
                String itemDate = postObject.getString("date");
                PostModel currentModel = new PostModel(blogID, itemDate, itemID, itemTitle,
                        itemTotal, itemURL, itemType);
                list.add(currentModel);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, "Unexpected PostModel object in top posts and pages array " +
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

    public List<PostModel> getTopPostsAndPages() {
        return mTopPostsAndPages;
    }

    public boolean hasTopPostsAndPages() {
        return mTopPostsAndPages != null && mTopPostsAndPages.size() > 0;
    }
}
