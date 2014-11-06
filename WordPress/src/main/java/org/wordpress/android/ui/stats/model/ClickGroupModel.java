package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.stats.StatsUtils;

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
    private List<ClickModel> mClicks;

    public ClickGroupModel(String blogId, String date, JSONObject clickGroupJSON) throws JSONException {
        setBlogId(blogId);
        setDate(StatsUtils.toMs(date));

        setGroupId(clickGroupJSON.getString("name"));
        setName(clickGroupJSON.getString("name"));
        setViews(clickGroupJSON.getInt("views"));
        if (clickGroupJSON.has("icon") && !clickGroupJSON.getString("icon").equals("null")) {
            setIcon(clickGroupJSON.getString("icon"));
        }

        // if URL is set in the response there is one result only. No need to unfold "results"
        if (clickGroupJSON.has("url") && !clickGroupJSON.getString("url").equals("null")) {
            setUrl(clickGroupJSON.getString("url"));
        } else {
            JSONArray childrenJSON = clickGroupJSON.getJSONArray("children");
            mClicks = new ArrayList<ClickModel>(childrenJSON.length());
            for (int i = 0; i < childrenJSON.length(); i++) {
                JSONObject currentResultJSON = childrenJSON.getJSONObject(i);
                ClickModel rm = new ClickModel(blogId, date, currentResultJSON);
                mClicks.add(rm);
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

    public int getViews() {
        return mViews;
    }

    public void setViews(int total) {
        this.mViews = total;
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

    public List<ClickModel> getClicks() { return mClicks; }
}
