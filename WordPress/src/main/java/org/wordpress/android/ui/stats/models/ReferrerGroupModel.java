package org.wordpress.android.ui.stats.models;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.stats.StatsUtils;
import org.wordpress.android.util.JSONUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A model to represent a referrer group stat
 */
public class ReferrerGroupModel implements Serializable {
    private String mBlogId;
    private long mDate;

    private String mGroupId;
    private String mName;
    private String mIcon;
    private int mTotal;
    private String mUrl;
    private List<SingleItemModel> mResults;

    public ReferrerGroupModel(String blogId, String date, JSONObject groupJSON) throws JSONException {
        setBlogId(blogId);
        setDate(StatsUtils.toMs(date));

        setGroupId(groupJSON.getString("group"));
        setName(groupJSON.getString("name"));
        setTotal(groupJSON.getInt("total"));
        setIcon(JSONUtils.getString(groupJSON, "icon"));

        // if URL is set in the response there is one result only. No need to unfold "results"
        if (!TextUtils.isEmpty(JSONUtils.getString(groupJSON, "url"))) {
            setUrl(JSONUtils.getString(groupJSON, "url"));
        } else {
            // Referrers is a 3-levels depth structure. We don't have 3 levels UI for now. Unfold childs here.
            JSONArray resultsJSON = groupJSON.getJSONArray("results");
            mResults = new ArrayList<>();
            for (int i = 0; i < resultsJSON.length(); i++) {
                JSONObject currentResultJSON = resultsJSON.getJSONObject(i);
                if (currentResultJSON.has("children")) {
                    JSONArray currentResultChildensJSON = currentResultJSON.getJSONArray("children");
                    for (int j = 0; j < currentResultChildensJSON.length(); j++) {
                        JSONObject currentChild = currentResultChildensJSON.getJSONObject(j);
                        mResults.add(getChildren(blogId, date, currentChild));
                    }
                } else {
                    mResults.add(getChildren(blogId, date, currentResultJSON));
                }
            }

            // Sort the childs by views.
            Collections.sort(mResults, new java.util.Comparator<SingleItemModel>() {
                public int compare(SingleItemModel o1, SingleItemModel o2) {
                    // descending order
                    return o2.getTotals() - o1.getTotals();
                }
            });

        }
    }

    private SingleItemModel getChildren(String blogId, String date, JSONObject child) throws JSONException {
        String name = child.getString("name");
        int totals = child.getInt("views");
        String icon = child.optString("icon");
        String url = child.optString("url");
        return new SingleItemModel(blogId, date, null, name, totals, url, icon);
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public long getDate() {
        return mDate;
    }

    public void setDate(long date) {
        this.mDate = date;
    }

    public String getGroupId() {
        return mGroupId;
    }

    public void setGroupId(String groupId) {
        this.mGroupId = groupId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getTotal() {
        return mTotal;
    }

    public void setTotal(int total) {
        this.mTotal = total;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String icon) {
        this.mIcon = icon;
    }

    public List<SingleItemModel> getResults() { return mResults; }
}
