package org.wordpress.android.ui.notifications;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.VolleyError;
import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.ReaderLog;

public class NotificationUtils {
    public static void refreshNotifications(final RestRequest.Listener listener,
                                            final RestRequest.ErrorListener errorListener) {
        WordPress.restClient.getNotifications(
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (listener != null) {
                            listener.onResponse(response);
                        }
                    }
                },
                new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (errorListener != null) {
                            errorListener.onErrorResponse(error);
                        }
                    }
                }
        );
    }

    public static List<Note> parseNotes(JSONObject response) throws JSONException {
        List<Note> notes;
        JSONArray notesJSON = response.getJSONArray("notes");
        notes = new ArrayList<Note>(notesJSON.length());
        for (int i = 0; i < notesJSON.length(); i++) {
            Note n = new Note(notesJSON.getJSONObject(i));
            notes.add(n);
        }
        return notes;
    }

    public static String unzipString(byte[] zbytes) {
        String unzipped = null;
        try {
            // Add extra byte to array when Inflater is set to true
            byte[] input = new byte[zbytes.length + 1];
            System.arraycopy(zbytes, 0, input, 0, zbytes.length);
            input[zbytes.length] = 0;
            ByteArrayInputStream bin = new ByteArrayInputStream(input);
            InflaterInputStream in = new InflaterInputStream(bin);
            ByteArrayOutputStream bout = new ByteArrayOutputStream(512);
            int b;
            while ((b = in.read()) != -1) {
                bout.write(b);
            }
            bout.close();
            unzipped = bout.toString();
        } catch (IOException io) {
            Log.e(WordPress.TAG, "Unzipping failed: " + io);
            return null;
        }
        return unzipped;
    }
    
    
    public static void getPushNotificationSettings(Context context, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        String gcmToken = GCMRegistrar.getRegistrationId(context);
        if (gcmToken == null)
            return;

        if (!WordPress.hasValidWPComCredentials(context))
            return;
        
        Map<String, String> contentStruct = new HashMap<String, String>();
        contentStruct.put("device_token", gcmToken);
        contentStruct.put("device_family", "android");
        contentStruct.put("app_secret_key", NotificationUtils.getAppPushNotificationsName());
        contentStruct.put("testing", "io non dovrei essere qua");
        WordPress.restClient.post("/push/settings", contentStruct, null, listener, errorListener);
        
        return;
    }
    
    public static void setPushNotificationSettings(Context context) {
        
        String gcmToken = GCMRegistrar.getRegistrationId(context);
        if (gcmToken == null)
            return;

        if (!WordPress.hasValidWPComCredentials(context))
            return;
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String settingsJson = settings.getString("wp_pref_notification_settings", null);

        if (settingsJson == null)
            return;

       Gson gson = new Gson();
        Map<String, StringMap<String>> notificationSettings = gson.fromJson(settingsJson, HashMap.class);
        Map<String, Object> updatedSettings = new HashMap<String, Object>();
        if (notificationSettings == null)
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
            if (MapUtils.getMapBool(userBlog, "value")) {
                mutedBlogsList.add(userBlog);
            }
        }

        if (mutedBlogsList.size() > 0) {
            updatedSettings.put("muted_blogs", mutedBlogsList);
        }

        if (updatedSettings.size() == 0)
            return;
        
        Map<String, String> contentStruct = new HashMap<String, String>();
        contentStruct.put("device_token", gcmToken);
        contentStruct.put("device_family", "android");
        contentStruct.put("app_secret_key", NotificationUtils.getAppPushNotificationsName());
        contentStruct.put("testing", "io non dovrei essere qua");
        contentStruct.put("settings", gson.toJson(updatedSettings));
        WordPress.restClient.post("/push/settings/new", contentStruct, null, null, null);

    }
    
    public static void registerPushNotificationsToken(final Context ctx, String token, final boolean loadSettings) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        String uuid = settings.getString("wp_pref_notifications_uuid", null);
        if (uuid == null)
            return;

        String deviceName = DeviceUtils.getInstance().getDeviceName(ctx);
        Map<String, String> contentStruct = new HashMap<String, String>();
        contentStruct.put("device_token", token);
        contentStruct.put("device_family", "android");
        contentStruct.put("app_secret_key", NotificationUtils.getAppPushNotificationsName());
        contentStruct.put("device_name", deviceName);
        contentStruct.put("device_model",  Build.MANUFACTURER + " " + Build.MODEL);
        contentStruct.put("app_version", WordPress.versionName);
        contentStruct.put("os_version",  android.os.Build.VERSION.RELEASE);
        contentStruct.put("device_uuid", uuid);
        contentStruct.put("testing", "io non dovrei essere qua");
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderLog.d("Register token action succeeded");
                if (loadSettings) { //load notification settings if necessary
                    com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            ReaderLog.d("token action succeeded");
                            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                            Editor editor = settings.edit();
                            try {
                                JSONObject settingsJSON = jsonObject.getJSONObject("settings");
                                editor.putString("wp_pref_notification_settings", settingsJSON.toString());
                                editor.commit();
                            } catch (JSONException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    };


                    NotificationUtils.getPushNotificationSettings(ctx, listener, null);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.w("Register token action failed");
                ReaderLog.e(volleyError);
            }
        };
        WordPress.restClient.post("/push/register", contentStruct, null, listener, errorListener);
    }
    
    public static void unregisterPushNotificationsToken(Context ctx, String token) {
        Map<String, String> contentStruct = new HashMap<String, String>();
        contentStruct.put("device_token", token);
        contentStruct.put("device_family", "android");
        contentStruct.put("app_secret_key", NotificationUtils.getAppPushNotificationsName());
   
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderLog.d("Unregister token action succeeded");
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.w("Unregister token action failed");
                ReaderLog.e(volleyError);
            }
        };
        WordPress.restClient.post("/push/unregister", contentStruct, null, listener, errorListener);
    }
    
    public static String getAppPushNotificationsName(){
        //white listing only few keys.
        if (BuildConfig.APP_PN_KEY.equals("org.wordpress.android.beta.build"))
                return "org.wordpress.android.beta.build";
        if (BuildConfig.APP_PN_KEY.equals("org.wordpress.android.debug.build"))
            return "org.wordpress.android.debug.build";
                
        return "org.wordpress.android.playstore";        
    }
}