package org.wordpress.android.ui.stats.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class TagModel implements Serializable {
    private String mName;
    private String mLink;
    private String mType;

    public TagModel(JSONObject tagJSON) throws JSONException {

        this.mName = tagJSON.getString("name");
        this.mType = tagJSON.getString("type");
        this.mLink = tagJSON.getString("link");
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