package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.stats.StatsUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A model to represent a Author
 */
public class AuthorModel implements Serializable {
    private String mBlogId;
    private long mDate;
    private String mGroupId;
    private String mName;
    private String mAvatar;
    private int mViews;
    private String mFollowData;
    private List<SingleItemModel> mPosts;

    public AuthorModel(String mBlogId, String date, String mGroupId, String mName, String mAvatar, int mViews, String mFollowData) {
        this.mBlogId = mBlogId;
        setDate(StatsUtils.toMs(date));
        this.mGroupId = mGroupId;
        this.mName = mName;
        this.mAvatar = mAvatar;
        this.mViews = mViews;
        this.mFollowData = mFollowData;
    }

    public AuthorModel(String blogId, String date, JSONObject authorJSON) throws JSONException {
        setBlogId(blogId);
        setDate(StatsUtils.toMs(date));

        setGroupId(authorJSON.getString("name"));
        setName(authorJSON.getString("name"));
        setViews(authorJSON.getInt("views"));
        if (authorJSON.has("avatar") && !authorJSON.getString("avatar").equals("null")) {
            setAvatar(authorJSON.getString("avatar"));
        }

        // Follow data could return a boolean false
        JSONObject followData = authorJSON.optJSONObject("follow_data");
        setFollowData((followData != null) ? followData.toString() : null);

        JSONArray postsJSON = authorJSON.getJSONArray("posts");
        mPosts = new ArrayList<SingleItemModel>(authorJSON.length());
        for (int i = 0; i < postsJSON.length(); i++) {
            JSONObject currentPostJSON = postsJSON.getJSONObject(i);
            String postId = String.valueOf(currentPostJSON.getInt("id"));
            String title = currentPostJSON.getString("title");
            int views = currentPostJSON.getInt("views");
            String url = currentPostJSON.getString("url");
            SingleItemModel currentPost = new SingleItemModel(mBlogId, mDate, postId, title, views, url, null);
            mPosts.add(currentPost);
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

    public String getFollowData() {
        return mFollowData;
    }

    public void setFollowData(String followData) {
        this.mFollowData = followData;
    }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String icon) {
        this.mAvatar = icon;
    }

    public List<SingleItemModel> getPosts() { return mPosts; }
}
