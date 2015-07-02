package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Maps to notification settings returned from the /me/notifications/settings endpoint on wp.com
public class NotificationsSettings {

    private static final String KEY_OTHER = "other";
    private static final String KEY_SITES = "sites";
    private static final String KEY_WPCOM = "wpcom";

    private JSONObject mOtherSettings;
    private Map<Long, JSONObject> mSiteSettings;
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

    private enum Channel {
        OTHER,
        SITES,
        WPCOM
    }

    public NotificationsSettings(JSONObject json) {
        mSiteSettings = new HashMap<>();

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
                        case KEY_WPCOM:
                            mDotcomSettings = settingsObject;
                            break;
                        default:
                            AppLog.i(AppLog.T.NOTIFS, "Unknown notification channel found");
                    }
                } else if (json.get(key) instanceof JSONArray && key.equals(KEY_SITES)) {
                    JSONArray siteSettingsArray = (JSONArray)json.get(key);
                    for (int i=0; i < siteSettingsArray.length(); i++) {
                        JSONObject siteSetting = siteSettingsArray.getJSONObject(i);
                        mSiteSettings.put(siteSetting.optLong("site_id"), siteSetting);
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

    public Map<Long, JSONObject> getSiteSettings() {
        return mSiteSettings;
    }

    public JSONObject getDotcomSettings() {
        return mDotcomSettings;
    }
}
