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
 * A model to represent a referrer result in stat
 */
public class ReferrerResultModel implements Serializable {
    private String mBlogId;
    private long mDate;

    private String mName;
    private String mIcon;
    private int mViews;
    private String mUrl;
    private List<SingleItemModel> mChildren;

    public ReferrerResultModel(String blogId, String date, JSONObject resultJSON) throws JSONException {
        setBlogId(blogId);
        setDate(StatsUtils.toMs(date));

        setName(resultJSON.getString("name"));
        setViews(resultJSON.getInt("views"));
        setIcon(JSONUtils.getString(resultJSON, "icon"));

        if (!TextUtils.isEmpty(JSONUtils.getString(resultJSON, "url"))) {
            setUrl(JSONUtils.getString(resultJSON, "url"));
        }

        if (resultJSON.has("children")) {
            JSONArray childrenJSON = resultJSON.getJSONArray("children");
            mChildren = new ArrayList<>();
            for (int i = 0; i < childrenJSON.length(); i++) {
                JSONObject currentChild = childrenJSON.getJSONObject(i);
                mChildren.add(getChildren(blogId, date, currentChild));
            }

            //Sort the children by views.
            Collections.sort(mChildren, new java.util.Comparator<SingleItemModel>() {
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
        String icon = JSONUtils.getString(child, "icon");
        String url = child.optString("url");
        return new SingleItemModel(blogId, date, null, name, totals, url, icon);
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

    public String getIcon() {
        return mIcon;
    }

    private void setIcon(String icon) {
        this.mIcon = icon;
    }

    public String getUrl() {
        return mUrl;
    }

    private void setUrl(String url) {
        this.mUrl = url;
    }

    public List<SingleItemModel> getChildren() { return mChildren; }
}
