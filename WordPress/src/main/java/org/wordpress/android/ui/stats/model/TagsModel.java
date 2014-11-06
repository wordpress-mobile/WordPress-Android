package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TagsModel implements Serializable {
    private String mBlogId;
    private int mViews;
    private List<TagModel> mTags;

    public TagsModel(String blogId, JSONObject responseJSON) throws JSONException {
        setBlogId(blogId);
        this.mViews = responseJSON.getInt("views");
        JSONArray innerTagsJSON = responseJSON.getJSONArray("tags");
        mTags = new ArrayList<TagModel>(innerTagsJSON.length());
        for (int i = 0; i < innerTagsJSON.length(); i++) {
            JSONObject currentTagJSON = innerTagsJSON.getJSONObject(i);
            TagModel currentTag = new TagModel(mBlogId, currentTagJSON);
            mTags.add(currentTag);
        }
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public List<TagModel> getTags() {
        return mTags;
    }

    public int getViews() {
        return mViews;
    }
}