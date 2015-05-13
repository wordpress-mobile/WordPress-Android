package org.wordpress.android.ui.stats.models;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.stats.StatsUtils;
import org.wordpress.android.util.JSONUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A model to represent a click group stat
 */
public class ClickGroupModel implements Serializable {
    private String mBlogId;
    private long mDate;

    private String mGroupId;
    private String mName;
    private String mIcon;
    private int mViews;
    private String mUrl;
    private List<SingleItemModel> mClicks;

    public ClickGroupModel(String blogId, String date, JSONObject clickGroupJSON) throws JSONException {
        setBlogId(blogId);
        setDate(StatsUtils.toMs(date));

        setGroupId(clickGroupJSON.getString("name"));
        setName(clickGroupJSON.getString("name"));
        setViews(clickGroupJSON.getInt("views"));
        setIcon(JSONUtils.getString(clickGroupJSON, "icon"));

        // if URL is set in the response there is one result only. No need to unfold "results"
        if (!TextUtils.isEmpty(JSONUtils.getString(clickGroupJSON, "url"))) {
            setUrl(JSONUtils.getString(clickGroupJSON, "url"));
        } else {
            JSONArray childrenJSON = clickGroupJSON.getJSONArray("children");
            mClicks = new ArrayList<>(childrenJSON.length());
            for (int i = 0; i < childrenJSON.length(); i++) {
                JSONObject currentResultJSON = childrenJSON.getJSONObject(i);
                String name = currentResultJSON.getString("name");
                int totals = currentResultJSON.getInt("views");
                String icon = currentResultJSON.optString("icon");
                String url = currentResultJSON.optString("url");
                SingleItemModel rm = new SingleItemModel(blogId, date, null, name, totals, url, icon);
                mClicks.add(rm);
            }
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

    public int getViews() {
        return mViews;
    }

    private void setViews(int total) {
        this.mViews = total;
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

    public List<SingleItemModel> getClicks() { return mClicks; }
}
