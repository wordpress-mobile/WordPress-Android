package org.wordpress.android.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.models.Blog;

public class JetpackUtils {
    public static boolean isShortlinksModuleEnabled(Blog blog) {
        if (blog == null) {
            AppLog.e(AppLog.T.UTILS, "Blog object is null, really?");
            return false;
        }
        if (!blog.isJetpackPowered()) {
            AppLog.e(AppLog.T.UTILS, "This blog " + blog.getAlternativeHomeUrl() + " doesn't seem Jetpack superpowered!");
            return false;
        }

        JSONObject jetpackModulesInfo = blog.getJetpackModulesInfoJSONObject();
        if (jetpackModulesInfo == null || !jetpackModulesInfo.has("modules")) {
            return false;
        }

        try {
            JSONArray modules = jetpackModulesInfo.getJSONArray("modules");
            for (int i = 0; i < modules.length(); i++) {
                JSONObject module = modules.getJSONObject(i);
                if (module.has("id") && module.optString("id", "").equals("shortlinks") &&
                        module.has("active") && module.optBoolean("active", false) ) {
                    return true;
                }
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, "invalid module info json", e);
        }
        return false;
    }
}
