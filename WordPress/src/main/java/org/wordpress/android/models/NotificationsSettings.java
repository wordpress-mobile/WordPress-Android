package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// Maps to notification settings returned from the /me/notifications/settings endpoint on wp.com
public class NotificationsSettings {

    public static final String KEY_BLOGS = "blogs";
    public static final String KEY_OTHER = "other";
    public static final String KEY_DOTCOM = "wpcom";

    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_BLOG_ID = "blog_id";

    private JSONObject mOtherSettings;
    private Map<Long, JSONObject> mBlogSettings;
    private JSONObject mDotcomSettings;

    public enum Type {
        TIMELINE,
        EMAIL,
        MOBILE;

        public String toString() {
            switch (this) {
                case TIMELINE:
                    return "timeline";
                case EMAIL:
                    return "email";
                case MOBILE:
                    return "device";
                default:
                    return "";
            }
        }
    }

    public enum Channel {
        OTHER,
        BLOGS,
        DOTCOM
    }

    public NotificationsSettings(JSONObject json) {
        updateJson(json);
    }

    public void updateJson(JSONObject json) {
        mBlogSettings = new HashMap<>();

        Iterator<?> keys = json.keys();
        while(keys.hasNext()) {
            String key = (String)keys.next();
            try {
                if (json.get(key) instanceof JSONObject) {
                    JSONObject settingsObject = (JSONObject)json.get(key);
                    switch(key) {
                        case KEY_OTHER:
                            mOtherSettings = settingsObject;
                            break;
                        case KEY_DOTCOM:
                            mDotcomSettings = settingsObject;
                            break;
                        default:
                            AppLog.i(AppLog.T.NOTIFS, "Unknown notification channel found");
                    }
                } else if (json.get(key) instanceof JSONArray && key.equals(KEY_BLOGS)) {
                    JSONArray siteSettingsArray = (JSONArray)json.get(key);
                    for (int i=0; i < siteSettingsArray.length(); i++) {
                        JSONObject siteSetting = siteSettingsArray.getJSONObject(i);
                        mBlogSettings.put(siteSetting.optLong(KEY_BLOG_ID), siteSetting);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public JSONObject getOtherSettings() {
        return mOtherSettings;
    }

    public Map<Long, JSONObject> getBlogSettings() {
        return mBlogSettings;
    }

    public JSONObject getDotcomSettings() {
        return mDotcomSettings;
    }
}
