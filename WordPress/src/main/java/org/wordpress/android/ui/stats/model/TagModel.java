package org.wordpress.android.ui.stats.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class TagModel implements Serializable {
    private String mBlogId;
    private String mName;
    private String mLink;
    private String mType;

    public TagModel(String blogId, String type, String name, String link) {
        this.mBlogId = blogId;
        this.mName = name;
        this.mType = type;
        this.mLink = link;
    }

    public TagModel(String blogId, JSONObject tagJSON) throws JSONException {
        setBlogId(blogId);
        this.mName = tagJSON.getString("name");
        this.mType = tagJSON.getString("type");
        this.mLink = tagJSON.getString("link");
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public String getName() {
        return mName;
    }

    public String getLink() {
        return mLink;
    }

    public String getType() {
        return mType;
    }
}