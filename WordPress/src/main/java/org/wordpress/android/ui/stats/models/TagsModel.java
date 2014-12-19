package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TagsModel implements Serializable {
    private int mViews;
    private List<TagModel> mTags;

    public TagsModel(JSONObject responseJSON) throws JSONException {
        this.mViews = responseJSON.getInt("views");
        JSONArray innerTagsJSON = responseJSON.getJSONArray("tags");
        mTags = new ArrayList<>(innerTagsJSON.length());
        for (int i = 0; i < innerTagsJSON.length(); i++) {
            JSONObject currentTagJSON = innerTagsJSON.getJSONObject(i);
            TagModel currentTag = new TagModel(currentTagJSON);
            mTags.add(currentTag);
        }
    }



    public List<TagModel> getTags() {
        return mTags;
    }

    public int getViews() {
        return mViews;
    }
}