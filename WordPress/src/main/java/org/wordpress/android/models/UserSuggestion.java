package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class UserSuggestion {
    private static final String MENTION_TAXONOMY = "mention";

    public long siteID;

    private String mUserLogin;
    private String mDisplayName;
    private String mImageUrl;
    private String mTaxonomy;

    public UserSuggestion(long siteID,
                          String userLogin,
                          String displayName,
                          String imageUrl,
                          String taxonomy) {
        this.siteID = siteID;
        mUserLogin = userLogin;
        mDisplayName = displayName;
        mImageUrl = imageUrl;
        mTaxonomy = taxonomy;
    }

    public static UserSuggestion fromJSON(JSONObject json, long siteID) {
        if (json == null) {
            return null;
        }

        String userLogin = JSONUtils.getString(json, "user_login");
        String displayName = JSONUtils.getString(json, "display_name");
        String imageUrl = JSONUtils.getString(json, "image_URL");

        // the api currently doesn't return a taxonomy field but we want to be ready for when it does
        return new UserSuggestion(siteID, userLogin, displayName, imageUrl, MENTION_TAXONOMY);
    }

    public static List<UserSuggestion> suggestionListFromJSON(JSONArray jsonArray, long siteID) {
        if (jsonArray == null) {
            return null;
        }

        ArrayList<UserSuggestion> suggestions = new ArrayList<UserSuggestion>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            UserSuggestion suggestion = UserSuggestion.fromJSON(jsonArray.optJSONObject(i), siteID);
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    public String getUserLogin() {
        return StringUtils.notNullStr(mUserLogin);
    }

    public String getDisplayName() {
        return StringUtils.notNullStr(mDisplayName);
    }

    public String getImageUrl() {
        return StringUtils.notNullStr(mImageUrl);
    }

    public String getTaxonomy() {
        return StringUtils.notNullStr(mTaxonomy);
    }
}
