package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.util.StatUtils;

/**
 * A model to represent a click group stat.
 */
public class StatsClickGroup {

    private String mBlogId;
    private long mDate;
    private String mGroupId;
    private String mName;
    private int mTotal;
    private String mUrl;
    private String mIcon;
    private int mChildren;

    public StatsClickGroup(String blogId, long date, String name, String groupId, int total, String url, String icon, int children) {
        this.setBlogId(blogId);
        this.setDate(date);
        this.setGroupId(groupId);
        this.setName(name);
        this.setTotal(total);
        this.setUrl(url);
        this.setIcon(icon);
        this.setChildren(children);
    }

    public StatsClickGroup(String blogId, String date, JSONObject result) throws JSONException {
        setBlogId(blogId);
        setDate(StatUtils.toMs(date));
        setGroupId(result.getString("group"));
        setName(result.getString("name"));
        setTotal(result.getInt("total"));
        if (result.has("icon") && !result.getString("icon").equals("null"))
            setIcon(result.getString("icon"));

        // Set a url only if there is one result, and this result starts with http
        // If there are more, the urls will be set in the children 
        JSONArray array = result.getJSONArray("results");
        if (array.length() == 1) {
            setChildren(0); // the child won't be stored if there's only one child
            
            JSONArray firstEntry = array.getJSONArray(0);
            String url = firstEntry.getString(0);
            if (url.startsWith("http"))
                setUrl(url);
        } else {
            setChildren(array.length());
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

    public int getChildren() {
        return mChildren;
    }

    public void setChildren(int children) {
        this.mChildren = children;
    }
}
