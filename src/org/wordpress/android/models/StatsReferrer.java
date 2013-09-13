package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;

import org.wordpress.android.util.StatUtils;

/**
 * A model to represent a referrer child stat.
 */
public class StatsReferrer {

    private String mBlogId;
    private long mDate;
    private String mGroupId;
    private String mName;
    private int mTotal;

    public StatsReferrer(String blogId, long date, String groupId, String name, int total) {
        this.setBlogId(blogId);
        this.setDate(date);
        this.setGroupId(groupId);
        this.setName(name);
        this.setTotal(total);
    }

    public StatsReferrer(String blogId, String date, String groupId, JSONArray result) throws JSONException {
        setBlogId(blogId);
        setDate(StatUtils.toMs(date));
        setGroupId(groupId);
        
        setName(result.getString(0));
        setTotal(result.getInt(1));
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
}
