
package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.util.StatUtils;

public class StatsReferrer {

    private String mBlogId;
    private long mDate;
    private String mName;
    private int mTotal;
    private String mUrl;
    private String mIcon;

    public StatsReferrer(String blogId, long date, String name, int total, String url, String icon) {
        this.mBlogId = blogId;
        this.mDate = date;
        this.mName = name;
        this.mTotal = total;
        this.mUrl = url;
        this.mIcon = icon;
    }

    public StatsReferrer(String blogId, String date, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setDate(StatUtils.toMs(date));
        setName(result.getString("name"));
        setTotal(result.getInt("total"));
        if (result.has("icon"))
            setIcon(result.getString("icon"));

        // for now, set the url to be the first result
        JSONArray array = result.getJSONArray("results");
        JSONArray firstEntry = array.getJSONArray(0);
        String url = firstEntry.getString(0);
        setUrl(url);
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
}
