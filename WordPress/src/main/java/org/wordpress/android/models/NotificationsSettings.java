package org.wordpress.android.models;

import androidx.collection.LongSparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;

// Maps to notification settings returned from the /me/notifications/settings endpoint on wp.com
public class NotificationsSettings {
    public static final String KEY_BLOGS = "blogs";
    public static final String KEY_OTHER = "other";
    public static final String KEY_WPCOM = "wpcom";
    public static final String KEY_DEVICES = "devices";

    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_BLOG_ID = "blog_id";

    private JSONObject mOtherSettings;
    private JSONObject mWPComSettings;
    private LongSparseArray<JSONObject> mBlogSettings;

    // The main notification settings channels (displayed at root of NoticationsSettingsFragment)
    public enum Channel {
        OTHER,
        BLOGS,
        WPCOM
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
        mBlogSettings = new LongSparseArray<>();

        mOtherSettings = JSONUtils.queryJSON(json, KEY_OTHER, new JSONObject());
        mWPComSettings = JSONUtils.queryJSON(json, KEY_WPCOM, new JSONObject());

        JSONArray siteSettingsArray = JSONUtils.queryJSON(json, KEY_BLOGS, new JSONArray());
        for (int i = 0; i < siteSettingsArray.length(); i++) {
            try {
                JSONObject siteSetting = siteSettingsArray.getJSONObject(i);
                mBlogSettings.put(siteSetting.optLong(KEY_BLOG_ID), siteSetting);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.NOTIFS, "Could not parse blog JSON in notification settings");
            }
        }
    }

    // Updates a specific notification setting after a user makes a change
    public void updateSettingForChannelAndType(Channel channel, Type type, String settingName, boolean newValue,
                                               long blogId) {
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
                case WPCOM:
                    getWPComSettings().put(settingName, newValue);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.NOTIFS, "Could not update notifications settings JSON");
        }
    }

    public JSONObject getOtherSettings() {
        return mOtherSettings;
    }

    public LongSparseArray<JSONObject> getBlogSettings() {
        return mBlogSettings;
    }

    public JSONObject getWPComSettings() {
        return mWPComSettings;
    }

    // Returns settings json for the given {@link Channel}, {@link Type} and optional blog id
    public JSONObject getSettingsJsonForChannelAndType(Channel channel, Type type, long blogId) {
        JSONObject settingsJson = null;
        String typeString = type.toString();
        switch (channel) {
            case BLOGS:
                if (blogId != -1) {
                    settingsJson = JSONUtils.queryJSON(getBlogSettings().get(blogId),
                            typeString, new JSONObject());
                }
                break;
            case OTHER:
                settingsJson = JSONUtils.queryJSON(getOtherSettings(),
                        typeString, new JSONObject());
                break;
            case WPCOM:
                settingsJson = getWPComSettings();
                break;
        }
        return settingsJson;
    }

    /**
     * Determines if the main switch should be displayed on a notifications settings preference screen
     * for the given {@link Channel} and {@link Type}
     *
     * @param channel The {@link Channel}
     * @param type The {@link Type}
     * @return A flag indicating whether main switch should be displayed.
     */
    public boolean shouldDisplayMainSwitch(Channel channel, Type type) {
        boolean displayMainSwitch = false;
        switch (channel) {
            case BLOGS:
                if (type == Type.TIMELINE) {
                    displayMainSwitch = true;
                }
                break;
            case OTHER:
            case WPCOM:
            default:
                break;
        }

        return displayMainSwitch;
    }

    /**
     * Finds if at least one notifications settings value is enabled in the given json
     *
     * @param settingsJson The settings json
     * @param settingsArray The string array of settings display names
     * @param settingsValues The string array of settings json keys
     * @return A flag indicating if at least one settings option is enabled.
     */
    public boolean isAtLeastOneSettingsEnabled(
        JSONObject settingsJson,
        String[] settingsArray,
        String[] settingsValues
    ) {
        if (settingsJson != null && settingsArray.length == settingsValues.length) {
            for (int i = 0; i < settingsArray.length; i++) {
                String settingValue = settingsValues[i];
                boolean isChecked = JSONUtils.queryJSON(settingsJson, settingValue, true);
                if (isChecked) {
                    return true;
                }
            }
        }
        return false;
    }
}
