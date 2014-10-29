package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Suggestion {
    private String userLogin;
    private String displayName;
    private String imageUrl;

    public Suggestion(String userLogin,
                      String displayName,
                      String imageUrl) {
        this.userLogin = userLogin;
        this.displayName = displayName;
        this.imageUrl = imageUrl;
    }

    public static Suggestion fromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        String userLogin = JSONUtil.getString(json, "user_login");
        String displayName = JSONUtil.getString(json, "display_name");
        String imageUrl = JSONUtil.getString(json, "image_URL");

        return new Suggestion(userLogin, displayName, imageUrl);
    }

    public static List<Suggestion> suggestionListFromJSON(JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }

        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            Suggestion suggestion = Suggestion.fromJSON(jsonArray.optJSONObject(i));
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
}
