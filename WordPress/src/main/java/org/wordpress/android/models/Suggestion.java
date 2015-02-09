package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Suggestion {
    private static final String MENTION_TAXONOMY = "mention";

    public long siteID;

    private String userLogin;
    private String displayName;
    private String imageUrl;
    private String taxonomy;

    public Suggestion(long siteID,
                      String userLogin,
                      String displayName,
                      String imageUrl,
                      String taxonomy) {
        this.siteID = siteID;
        this.userLogin = userLogin;
        this.displayName = displayName;
        this.imageUrl = imageUrl;
        this.taxonomy = taxonomy;
    }

    public static Suggestion fromJSON(JSONObject json, long siteID) {
        if (json == null) {
            return null;
        }

        String userLogin = JSONUtils.getString(json, "user_login");
        String displayName = JSONUtils.getString(json, "display_name");
        String imageUrl = JSONUtils.getString(json, "image_URL");

        // the api currently doesn't return a taxonomy field but we want to be ready for when it does
        return new Suggestion(siteID, userLogin, displayName, imageUrl, MENTION_TAXONOMY);
    }

    public static List<Suggestion> suggestionListFromJSON(JSONArray jsonArray, long siteID) {
        if (jsonArray == null) {
            return null;
        }

        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            Suggestion suggestion = Suggestion.fromJSON(jsonArray.optJSONObject(i), siteID);
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    public String getUserLogin() {
        return StringUtils.notNullStr(userLogin);
    }
    public String getDisplayName() {
        return StringUtils.notNullStr(displayName);
    }
    public String getImageUrl() {
        return StringUtils.notNullStr(imageUrl);
    }
    public String getTaxonomy() { return StringUtils.notNullStr(taxonomy); }
}
