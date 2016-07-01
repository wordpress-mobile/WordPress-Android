package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Tag {
    public long siteID;

    public long id;
    private String tag;

    public Tag(long siteID,
               long id,
               String tag) {
        this.siteID = siteID;
        this.id = id;
        this.tag = tag;
    }

    public static Tag fromJSON(JSONObject json, long siteID) {
        if (json == null) {
            return null;
        }

        String tag = JSONUtils.getString(json, "name");
        String idString =  JSONUtils.getString(json, "ID");
        long id = TextUtils.isEmpty(idString) ? 0 : Long.valueOf(idString);

        // the api currently doesn't return a taxonomy field but we want to be ready for when it does
        return new Tag(siteID, id, tag);
    }

    public static List<Tag> tagListFromJSON(JSONArray jsonArray, long siteID) {
        if (jsonArray == null) {
            return null;
        }

        ArrayList<Tag> suggestions = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            Tag suggestion = Tag.fromJSON(jsonArray.optJSONObject(i), siteID);
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    public String getTag() {
        return StringUtils.notNullStr(tag);
    }
}
