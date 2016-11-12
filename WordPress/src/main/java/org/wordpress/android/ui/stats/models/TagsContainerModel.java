package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TagsContainerModel extends BaseStatsModel {
    private String mDate;
    private String mBlogID;
    private List<TagsModel> mTags;

    public TagsContainerModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;
        this.mDate = response.getString("date");
        JSONArray outerTags = response.getJSONArray("tags");
        if (outerTags != null) {
             mTags = new ArrayList<>(outerTags.length());
            for (int i = 0; i < outerTags.length(); i++) {
                JSONObject current = outerTags.getJSONObject(i);
                mTags.add(new TagsModel(current));
            }
        }
    }

    public List<TagsModel> getTags() {
        return mTags;
    }

    public String getBlogId() {
        return mBlogID;
    }

    public void setBlogId(String blogId) {
        this.mBlogID = blogId;
    }

    public String getDate() {
        return mDate;
    }
}
