
package org.xmlrpc.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;

/**
 * WordPress.com specific XML-RPC API calls
 * 
 * @author Dan Roundhill
 */

public class WPComXMLRPCApi {

    private XMLRPCClient client = new XMLRPCClient(Constants.wpcomXMLRPCURL, "", "");

    public void getNotificationSettings(final XMLRPCCallback callback, Context context) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String gcmToken = GCMRegistrar.getRegistrationId(context);
        if (gcmToken == null)
            return;

        if (!WordPress.hasValidWPComCredentials(context))
            return;

        /*
         * String wpcomToken = settings.getString("wpcom-access-token", null); if (wpcomToken ==
         * null) return false; client.setAuthorizationHeader(wpcomToken);
         */

        Object[] params = {
                settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, ""),
                WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, "")),
                gcmToken,
                "android"
        };

        client.callAsync(new XMLRPCCallback() {
            public void onSuccess(long id, Object result) {
                Log.v("WORDPRESS", "Push notification settings retrieved!");
                if (callback != null) {
                    callback.onSuccess(id, result);
                }
                Gson gson = new Gson();
                Editor editor = settings.edit();
                editor.putString("wp_pref_notification_settings", gson.toJson(result));
                editor.commit();
            }

            public void onFailure(long id, XMLRPCException error) {
                if (callback != null) {
                    callback.onFailure(id, error);
                }
                Log.v("WORDPRESS", error.getMessage());
            }
        }, "wpcom.get_mobile_push_notification_settings", params);
    }

    public void setNotificationSettings(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String settingsJson = settings.getString("wp_pref_notification_settings", null);

        if (settingsJson == null)
            return;

        Gson gson = new Gson();
        Map<String, StringMap<String>> notificationSettings = gson.fromJson(settingsJson, HashMap.class);
        Map<String, Object> updatedSettings = new HashMap<String, Object>();
        if (notificationSettings == null)
            return;
        String gcmToken = GCMRegistrar.getRegistrationId(context);
        if (gcmToken == null)
            return;

        if (!WordPress.hasValidWPComCredentials(context))
            return;

        // Build the settings object to send back to WP.com
        StringMap<?> mutedBlogsMap = (StringMap<?>) notificationSettings.get("muted_blogs");
        StringMap<?> muteUntilMap = (StringMap<?>) notificationSettings.get("mute_until");
        ArrayList<StringMap<Double>> blogsList = (ArrayList<StringMap<Double>>) mutedBlogsMap.get("value");
        notificationSettings.remove("muted_blogs");
        notificationSettings.remove("mute_until");

        for (Map.Entry<String, StringMap<String>> entry : notificationSettings.entrySet())
        {
            StringMap<String> setting = entry.getValue();
            updatedSettings.put(entry.getKey(), setting.get("value"));
        }

        if (muteUntilMap != null && muteUntilMap.get("value") != null) {
            updatedSettings.put("mute_until", muteUntilMap.get("value"));
        }

        ArrayList<StringMap<Double>> mutedBlogsList = new ArrayList<StringMap<Double>>();
        for (int i = 0; i < blogsList.size(); i++) {
            StringMap<Double> userBlog = blogsList.get(i);
            if (Double.valueOf(userBlog.get("value")) == 1.0) {
                mutedBlogsList.add(userBlog);
            }
        }

        if (mutedBlogsList.size() > 0) {
            updatedSettings.put("muted_blogs", mutedBlogsList);
        }

        /*
         * String wpcomToken = settings.getString("wpcom-access-token", null); if (wpcomToken ==
         * null) return false; client.setAuthorizationHeader(wpcomToken);
         */

        if (updatedSettings.size() == 0)
            return;

        Object[] params = {
                settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, ""),
                WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, "")),
                updatedSettings,
                gcmToken,
                "android"
        };

        client.callAsync(new XMLRPCCallback() {
            public void onSuccess(long id, Object result) {
                Log.v("WORDPRESS", "Notification settings updated successfully");
            }

            public void onFailure(long id, XMLRPCException error) {
                Log.v("WORDPRESS", error.getMessage());
            }
        }, "wpcom.set_mobile_push_notification_settings", params);
    }
}
