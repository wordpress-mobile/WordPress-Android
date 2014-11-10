package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.stats.StatsUtils;

import java.io.Serializable;
import java.util.ArrayList;
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
        if (groupJSON.has("icon") && !groupJSON.getString("icon").equals("null")) {
            setIcon(groupJSON.getString("icon"));
        }

        // if URL is set in the response there is one result only. No need to unfold "results"
        if (groupJSON.has("url") && !groupJSON.getString("url").equals("null")) {
            setUrl(groupJSON.getString("url"));
        } else {
            JSONArray resultsJSON = groupJSON.getJSONArray("results");
            mResults = new ArrayList<SingleItemModel>(resultsJSON.length());
            for (int i = 0; i < resultsJSON.length(); i++) {
                JSONObject currentResultJSON = resultsJSON.getJSONObject(i);
                String name = currentResultJSON.getString("name");
                int totals = currentResultJSON.getInt("views");
                String icon = currentResultJSON.optString("icon");
                SingleItemModel rm = new SingleItemModel(blogId, date, null, name, totals, null, icon );
                mResults.add(rm);
            }
        }
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
