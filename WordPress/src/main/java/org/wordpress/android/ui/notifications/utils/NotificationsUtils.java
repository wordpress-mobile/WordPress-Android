package org.wordpress.android.ui.notifications.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;

import com.android.volley.VolleyError;
import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.NotificationsActivity;
import org.wordpress.android.ui.notifications.blocks.NoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan;
import org.wordpress.android.ui.notifications.blocks.NoteBlockRangeType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NotificationsUtils {

    public static final String WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS = "wp_pref_notification_settings";
    private static final String WPCOM_PUSH_DEVICE_SERVER_ID = "wp_pref_notifications_server_id";
    public static final String WPCOM_PUSH_DEVICE_UUID = "wp_pref_notifications_uuid";

    public static void getPushNotificationSettings(Context context, RestRequest.Listener listener,
                                                   RestRequest.ErrorListener errorListener) {
        if (!WordPress.hasValidWPComCredentials(context)) {
            return;
        }

        String gcmToken = GCMRegistrar.getRegistrationId(context);
        if (TextUtils.isEmpty(gcmToken)) {
            return;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceID = settings.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null);
        if (TextUtils.isEmpty(deviceID)) {
            AppLog.e(T.NOTIFS, "device_ID is null in preferences. Get device settings skipped.");
            return;
        }

        WordPress.getRestClientUtils().get("/device/" + deviceID, listener, errorListener);
    }

    public static void setPushNotificationSettings(Context context) {
        if (!WordPress.hasValidWPComCredentials(context)) {
            return;
        }

        String gcmToken = GCMRegistrar.getRegistrationId(context);
        if (TextUtils.isEmpty(gcmToken)) {
            return;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceID = settings.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null);
        if (TextUtils.isEmpty(deviceID)) {
            AppLog.e(T.NOTIFS, "device_ID is null in preferences. Set device settings skipped.");
            return;
        }

        String settingsJson = settings.getString(WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, null);
        if (settingsJson == null)
            return;

        Gson gson = new Gson();
        Map<String, StringMap<String>> notificationSettings = gson.fromJson(settingsJson, HashMap.class);
        Map<String, Object> updatedSettings = new HashMap<String, Object>();
        if (notificationSettings == null)
            return;


        // Build the settings object to send back to WP.com
        StringMap<?> mutedBlogsMap = notificationSettings.get("muted_blogs");
        StringMap<?> muteUntilMap = notificationSettings.get("mute_until");
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
        for (StringMap<Double> userBlog : blogsList) {
            if (MapUtils.getMapBool(userBlog, "value")) {
                mutedBlogsList.add(userBlog);
            }
        }

        if (updatedSettings.size() == 0 && mutedBlogsList.size() == 0)
            return;

        updatedSettings.put("muted_blogs", mutedBlogsList); //If muted blogs list is unchanged we can even skip this assignment.

        Map<String, String> contentStruct = new HashMap<String, String>();
        contentStruct.put("device_token", gcmToken);
        contentStruct.put("device_family", "android");
        contentStruct.put("app_secret_key", NotificationsUtils.getAppPushNotificationsName());
        contentStruct.put("settings", gson.toJson(updatedSettings));
        WordPress.getRestClientUtils().post("/device/"+deviceID, contentStruct, null, null, null);
    }

    public static void registerDeviceForPushNotifications(final Context ctx, String token) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        String uuid = settings.getString(WPCOM_PUSH_DEVICE_UUID, null);
        if (uuid == null)
            return;

        String deviceName = DeviceUtils.getInstance().getDeviceName(ctx);
        Map<String, String> contentStruct = new HashMap<String, String>();
        contentStruct.put("device_token", token);
        contentStruct.put("device_family", "android");
        contentStruct.put("app_secret_key", NotificationsUtils.getAppPushNotificationsName());
        contentStruct.put("device_name", deviceName);
        contentStruct.put("device_model",  Build.MANUFACTURER + " " + Build.MODEL);
        contentStruct.put("app_version", WordPress.versionName);
        contentStruct.put("os_version",  Build.VERSION.RELEASE);
        contentStruct.put("device_uuid", uuid);
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(T.NOTIFS, "Register token action succeeded");
                try {
                    String deviceID = jsonObject.getString("ID");
                    if (deviceID==null) {
                        AppLog.e(T.NOTIFS, "Server response is missing of the device_id. Registration skipped!!");
                        return;
                    }
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(WPCOM_PUSH_DEVICE_SERVER_ID, deviceID);
                    JSONObject settingsJSON = jsonObject.getJSONObject("settings");
                    editor.putString(WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, settingsJSON.toString());
                    editor.apply();
                    AppLog.d(T.NOTIFS, "Server response OK. The device_id : " + deviceID);
                } catch (JSONException e1) {
                    AppLog.e(T.NOTIFS, "Server response is NOT ok. Registration skipped!!", e1);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.NOTIFS, "Register token action failed", volleyError);
            }
        };

        WordPress.getRestClientUtils().post("/devices/new", contentStruct, null, listener, errorListener);
    }

    public static void unregisterDevicePushNotifications(final Context ctx) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(T.NOTIFS, "Unregister token action succeeded");
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
                editor.remove(WPCOM_PUSH_DEVICE_SERVER_ID);
                editor.remove(WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS);
                editor.remove(WPCOM_PUSH_DEVICE_UUID);
                editor.apply();
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.NOTIFS, "Unregister token action failed", volleyError);
            }
        };

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        String deviceID = settings.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null );
        if (TextUtils.isEmpty(deviceID)) {
            return;
        }
        WordPress.getRestClientUtils().post("/devices/" + deviceID + "/delete", listener, errorListener);
    }

    private static String getAppPushNotificationsName() {
        //white listing only few keys.
        if (BuildConfig.APP_PN_KEY.equals("org.wordpress.android.beta.build"))
                return "org.wordpress.android.beta.build";
        if (BuildConfig.APP_PN_KEY.equals("org.wordpress.android.debug.build"))
            return "org.wordpress.android.debug.build";

        return "org.wordpress.android.playstore";
    }

    public static Spannable getSpannableTextFromIndices(JSONObject subject,
                                                        final NoteBlock.OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        if (subject == null) {
            return new SpannableStringBuilder();
        }

        String text = subject.optString("text", "").trim();
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);

        boolean shouldLink = onNoteBlockTextClickListener != null;

        try {
            JSONArray rangesArray = subject.getJSONArray("ranges");

            for (int i=0; i < rangesArray.length(); i++) {
                JSONObject rangeObject = (JSONObject) rangesArray.get(i);
                NoteBlockClickableSpan clickableSpan = new NoteBlockClickableSpan(rangeObject, shouldLink) {
                    @Override
                    public void onClick(View widget) {
                        if (onNoteBlockTextClickListener != null) {
                            onNoteBlockTextClickListener.onNoteBlockTextClicked(this);
                        }
                    }
                };
                int[] indices = clickableSpan.getIndices();
                if (indices.length == 2 && indices[0] <= spannableStringBuilder.length() &&
                        indices[1] <= spannableStringBuilder.length()) {
                    spannableStringBuilder.setSpan(clickableSpan, indices[0], indices[1], Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    // Add additional styling if the id wants it
                    if (clickableSpan.getSpanStyle() != Typeface.NORMAL) {
                        StyleSpan styleSpan = new StyleSpan(clickableSpan.getSpanStyle());
                        spannableStringBuilder.setSpan(styleSpan, indices[0], indices[1], Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }
                }
            }
        } catch (JSONException e) {
            return spannableStringBuilder;
        }

        return spannableStringBuilder;
    }

    public static Spannable getClickableTextForIdUrl(JSONObject idBlock, String text,
                                                     final NoteBlock.OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        if (idBlock == null || TextUtils.isEmpty(text)) {
            return new SpannableStringBuilder("");
        }

        boolean shouldLink = onNoteBlockTextClickListener != null;

        NoteBlockClickableSpan clickableSpan = new NoteBlockClickableSpan(idBlock, shouldLink) {
            @Override
            public void onClick(View widget) {
                if (onNoteBlockTextClickListener != null) {
                    onNoteBlockTextClickListener.onNoteBlockTextClicked(this);
                }
            }
        };

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
        spannableStringBuilder.setSpan(clickableSpan, 0, spannableStringBuilder.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        return spannableStringBuilder;
    }

    public static void handleNoteBlockSpanClick(NotificationsActivity activity, NoteBlockClickableSpan clickedSpan) {
        if (clickedSpan.shouldShowBlogPreview()) {
            // Show blog preview
            activity.showBlogPreviewActivity(clickedSpan.getSiteId(), clickedSpan.getUrl());
        } else if (clickedSpan.getRangeType() == NoteBlockRangeType.POST) {
            // Show post detail
            activity.showPostActivity(clickedSpan.getSiteId(), clickedSpan.getId());
        } else if (clickedSpan.getRangeType() == NoteBlockRangeType.COMMENT) {
            // For now, show post detail for comments
            activity.showPostActivity(clickedSpan.getSiteId(), clickedSpan.getPostId());
        } else if (clickedSpan.getRangeType() == NoteBlockRangeType.STAT) {
            // We can open native stats, but only if the site is stored in the app locally.
            int localTableSiteId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(
                    (int)clickedSpan.getSiteId()
            );

            if (localTableSiteId > 0) {
                activity.showStatsActivityForSite(localTableSiteId);
            } else if (!TextUtils.isEmpty(clickedSpan.getUrl())) {
                activity.showWebViewActivityForUrl(clickedSpan.getUrl());
            }
        } else {
            // We don't know what type of id this is, let's see if it has a URL and push a webview
            if (!TextUtils.isEmpty(clickedSpan.getUrl())) {
                activity.showWebViewActivityForUrl(clickedSpan.getUrl());
            }
        }
    }
}