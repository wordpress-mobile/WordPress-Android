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
    private List<ReferrerResultModel> mResults;

    public transient boolean isRestCallInProgress = false;
    public transient boolean isMarkedAsSpam = false;

    public ReferrerGroupModel(String blogId, String date, JSONObject groupJSON) throws JSONException {
        setBlogId(blogId);
        setDate(StatsUtils.toMs(date));

        setGroupId(groupJSON.getString("group"));
        setName(groupJSON.getString("name"));
        setTotal(groupJSON.getInt("total"));
        setIcon(JSONUtils.getString(groupJSON, "icon"));

        // if URL is set in the response there is one result only.
        if (!TextUtils.isEmpty(JSONUtils.getString(groupJSON, "url"))) {
            setUrl(JSONUtils.getString(groupJSON, "url"));
        }

        // results is an array when there are results, otherwise it's an object.
        JSONArray resultsArray = groupJSON.optJSONArray("results");
        if (resultsArray != null) {
            mResults = new ArrayList<>();
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject currentResultJSON = resultsArray.getJSONObject(i);
                ReferrerResultModel currentResultModel = new ReferrerResultModel(blogId,
                        date, currentResultJSON);
                mResults.add(currentResultModel);
            }
            // Sort the results by views.
            Collections.sort(mResults, new java.util.Comparator<ReferrerResultModel>() {
                public int compare(ReferrerResultModel o1, ReferrerResultModel o2) {
                    // descending order
                    return o2.getViews() - o1.getViews();
                }
            });
        }
    }

    public String getBlogId() {
        return mBlogId;
    }

    private void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public long getDate() {
        return mDate;
    }

    private void setDate(long date) {
        this.mDate = date;
    }

    public String getGroupId() {
        return mGroupId;
    }

    private void setGroupId(String groupId) {
        this.mGroupId = groupId;
    }

    public String getName() {
        return mName;
    }

    private void setName(String name) {
        this.mName = name;
    }

    public int getTotal() {
        return mTotal;
    }

    private void setTotal(int total) {
        this.mTotal = total;
    }

    public String getUrl() {
        return mUrl;
    }

    private void setUrl(String url) {
        this.mUrl = url;
    }

    public String getIcon() {
        return mIcon;
    }

    private void setIcon(String icon) {
        this.mIcon = icon;
    }

    public List<ReferrerResultModel> getResults() { return mResults; }
}
