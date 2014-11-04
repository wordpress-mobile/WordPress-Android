package org.wordpress.android.ui.stats.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.stats.StatsUtils;

import java.io.Serializable;

/**
 * A model to represent a click child stat.
 */
public class ClickModel implements Serializable {
    private String mBlogId;
    private long mDate;
    private String mName;
    private int mTotal;
    private String mIcon;
    private String mUrl;

    public ClickModel(String blogId, String date, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setDate(StatsUtils.toMs(date));
        setName(result.getString("name"));
        setTotal(result.getInt("views"));
        setIcon(result.optString("icon"));
        setUrl(result.optString("url"));
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

    public int getTotal() {
        return mTotal;
    }

    public void setTotal(int total) {
        this.mTotal = total;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String icon) {
        this.mIcon = icon;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }
}
