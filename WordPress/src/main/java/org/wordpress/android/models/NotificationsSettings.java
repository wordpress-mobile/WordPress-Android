package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;

import java.util.HashMap;
import java.util.Map;

// Maps to notification settings returned from the /me/notifications/settings endpoint on wp.com
public class NotificationsSettings {

    public static final String KEY_BLOGS = "blogs";
    public static final String KEY_OTHER = "other";
    public static final String KEY_DOTCOM = "wpcom";
    public static final String KEY_DEVICES = "devices";

    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_BLOG_ID = "blog_id";

    private JSONObject mOtherSettings;
    private JSONObject mDotcomSettings;
    private Map<Long, JSONObject> mBlogSettings;

    // The main notification settings channels (displayed at root of NoticationsSettingsFragment)
    public enum Channel {
        OTHER,
        BLOGS,
        DOTCOM
    }

    // The notification setting type, used in BLOGS and OTHER channels
    public enum Type {
        TIMELINE,
        EMAIL,
        DEVICE;

        public String toString() {
            switch (this) {
                case TIMELINE:
                    return "timeline";
                case EMAIL:
                    return "email";
                case DEVICE:
                    return "device";
                default:
                    return "";
            }
        }
    }

    public NotificationsSettings(JSONObject json) {
        updateJson(json);
    }

    // Parses the json response from /me/notifications/settings endpoint and updates the instance variables
    public void updateJson(JSONObject json) {
        mBlogSettings = new HashMap<>();

        mOtherSettings = JSONUtils.queryJSON(json, KEY_OTHER, new JSONObject());
        mDotcomSettings = JSONUtils.queryJSON(json, KEY_DOTCOM, new JSONObject());

        JSONArray siteSettingsArray = JSONUtils.queryJSON(json, KEY_BLOGS, new JSONArray());
        for (int i=0; i < siteSettingsArray.length(); i++) {
            try {
                JSONObject siteSetting = siteSettingsArray.getJSONObject(i);
                mBlogSettings.put(siteSetting.optLong(KEY_BLOG_ID), siteSetting);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.NOTIFS, "Could not parse blog JSON in notification settings");
            }
        }
    }

    // Updates a specific notification setting after a user makes a change
    public void updateSettingForChannelAndType(Channel channel, Type type, String settingName, boolean newValue, long blogId) {
        String typeName = type.toString();
        try {
            switch (channel) {
                case BLOGS:
                    JSONObject blogJson = getBlogSettings().get(blogId);
                    if (blogJson != null) {
                        JSONObject blogSetting = JSONUtils.queryJSON(blogJson, typeName, new JSONObject());
                        blogSetting.put(settingName, newValue);
                        blogJson.put(typeName, blogSetting);

                        getBlogSettings().put(blogId, blogJson);
                    }
                    break;
                case OTHER:
                    JSONObject otherSetting = JSONUtils.queryJSON(getOtherSettings(), typeName, new JSONObject());
                    otherSetting.put(settingName, newValue);
                    getOtherSettings().put(typeName, otherSetting);
                    break;
                case DOTCOM:
                    getDotcomSettings().put(settingName, newValue);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.NOTIFS, "Could not update notifications settings JSON");
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
