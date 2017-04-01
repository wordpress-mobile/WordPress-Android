package org.wordpress.android.ui.notifications.utils;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.models.Note;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.ui.notifications.blocks.NoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.PackageUtils;
import org.wordpress.android.util.helpers.WPImageGetter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsUtils {
    public static final String ARG_PUSH_AUTH_TOKEN = "arg_push_auth_token";
    public static final String ARG_PUSH_AUTH_TITLE = "arg_push_auth_title";
    public static final String ARG_PUSH_AUTH_MESSAGE = "arg_push_auth_message";
    public static final String ARG_PUSH_AUTH_EXPIRES = "arg_push_auth_expires";

    public static final String WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS = "wp_pref_notification_settings";
    public static final String WPCOM_PUSH_DEVICE_UUID = "wp_pref_notifications_uuid";
    public static final String WPCOM_PUSH_DEVICE_TOKEN = "wp_pref_notifications_token";

    public static final String WPCOM_PUSH_DEVICE_SERVER_ID = "wp_pref_notifications_server_id";
    public static final String PUSH_AUTH_ENDPOINT = "me/two-step/push-authentication";

    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

    private static final String WPCOM_SETTINGS_ENDPOINT = "/me/notifications/settings/";

    public interface TwoFactorAuthCallback {
        void onTokenValid(String token, String title, String message);
        void onTokenInvalid();
    }

    public static void getPushNotificationSettings(Context context, RestRequest.Listener listener,
                                                   RestRequest.ErrorListener errorListener) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceID = settings.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null);
        String settingsEndpoint = WPCOM_SETTINGS_ENDPOINT;
        if (!TextUtils.isEmpty(deviceID)) {
            settingsEndpoint += "?device_id=" + deviceID;
        }
        WordPress.getRestClientUtilsV1_1().get(settingsEndpoint, listener, errorListener);
    }

    public static void registerDeviceForPushNotifications(final Context ctx, String token) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        String uuid = settings.getString(WPCOM_PUSH_DEVICE_UUID, null);
        if (uuid == null)
            return;

        String deviceName = DeviceUtils.getInstance().getDeviceName(ctx);
        Map<String, String> contentStruct = new HashMap<>();
        contentStruct.put("device_token", token);
        contentStruct.put("device_family", "android");
        contentStruct.put("device_name", deviceName);
        contentStruct.put("device_model", Build.MANUFACTURER + " " + Build.MODEL);
        contentStruct.put("app_version", WordPress.versionName);
        contentStruct.put("version_code", String.valueOf(PackageUtils.getVersionCode(ctx)));
        contentStruct.put("os_version", Build.VERSION.RELEASE);
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
                    editor.apply();
                    AppLog.d(T.NOTIFS, "Server response OK. The device_id: " + deviceID);
                } catch (JSONException e1) {
                    AppLog.e(T.NOTIFS, "Server response is NOT ok, registration skipped.", e1);
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
                editor.remove(WPCOM_PUSH_DEVICE_UUID);
                editor.remove(WPCOM_PUSH_DEVICE_TOKEN);
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

    public static Spannable getSpannableContentForRanges(JSONObject subject) {
        return getSpannableContentForRanges(subject, null, null, false);
    }

    /**
     * Returns a spannable with formatted content based on WP.com note content 'range' data
     * @param blockObject the JSON data
     * @param textView the TextView that will display the spannnable
     * @param onNoteBlockTextClickListener - click listener for ClickableSpans in the spannable
     * @param footerEh - Set if spannable should apply special formatting
     * @return Spannable string with formatted content
     */
    public static Spannable getSpannableContentForRanges(JSONObject blockObject, TextView textView,
                                                         final NoteBlock.OnNoteBlockTextClickListener onNoteBlockTextClickListener,
                                                         boolean footerEh) {
        if (blockObject == null) {
            return new SpannableStringBuilder();
        }

        String text = blockObject.optString("text", "");
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);

        boolean shouldLink = onNoteBlockTextClickListener != null;

        // Add ImageSpans for note media
        addImageSpansForBlockMedia(textView, blockObject, spannableStringBuilder);

        // Process Ranges to add links and text formatting
        JSONArray rangesArray = blockObject.optJSONArray("ranges");
        if (rangesArray != null) {
            for (int i = 0; i < rangesArray.length(); i++) {
                JSONObject rangeObject = rangesArray.optJSONObject(i);
                if (rangeObject == null) {
                    continue;
                }

                NoteBlockClickableSpan clickableSpan = new NoteBlockClickableSpan(WordPress.getContext(), rangeObject,
                        shouldLink, footerEh) {
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

                    // Add additional styling if the range wants it
                    if (clickableSpan.getSpanStyle() != Typeface.NORMAL) {
                        StyleSpan styleSpan = new StyleSpan(clickableSpan.getSpanStyle());
                        spannableStringBuilder.setSpan(styleSpan, indices[0], indices[1], Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }
                }
            }
        }

        return spannableStringBuilder;
    }

    public static int[] getIndicesForRange(JSONObject rangeObject) {
        int[] indices = new int[]{0,0};
        if (rangeObject == null) {
            return indices;
        }

        JSONArray indicesArray = rangeObject.optJSONArray("indices");
        if (indicesArray != null && indicesArray.length() >= 2) {
            indices[0] = indicesArray.optInt(0);
            indices[1] = indicesArray.optInt(1);
        }

        return indices;
    }

    /**
     * Adds ImageSpans to the passed SpannableStringBuilder
     */
    private static void addImageSpansForBlockMedia(TextView textView, JSONObject subject, SpannableStringBuilder spannableStringBuilder) {
        if (textView == null || subject == null || spannableStringBuilder == null) return;

        Context context = textView.getContext();
        JSONArray mediaArray = subject.optJSONArray("media");
        if (context == null || mediaArray == null) {
            return;
        }

        Drawable loading = context.getResources().getDrawable(
            org.wordpress.android.editor.R.drawable.legacy_dashicon_format_image_big_grey);
        Drawable failed = context.getResources().getDrawable(R.drawable.ic_notice_grey_500_48dp);
        // Note: notifications_max_image_size seems to be the max size an ImageSpan can handle,
        // otherwise it would load blank white
        WPImageGetter imageGetter = new WPImageGetter(
                textView,
                context.getResources().getDimensionPixelSize(R.dimen.notifications_max_image_size),
                WordPress.sImageLoader,
                loading,
                failed
        );

        int indexAdjustment = 0;
        String imagePlaceholder;
        for (int i = 0; i < mediaArray.length(); i++) {
            JSONObject mediaObject = mediaArray.optJSONObject(i);
            if (mediaObject == null) {
                continue;
            }

            final Drawable remoteDrawable = imageGetter.getDrawable(mediaObject.optString("url", ""));
            ImageSpan noteImageSpan = new ImageSpan(remoteDrawable, mediaObject.optString("url", ""));
            int startIndex = JSONUtils.queryJSON(mediaObject, "indices[0]", -1);
            int endIndex = JSONUtils.queryJSON(mediaObject, "indices[1]", -1);
            if (startIndex >= 0) {
                startIndex += indexAdjustment;
                endIndex += indexAdjustment;

                if (startIndex > spannableStringBuilder.length()) {
                    continue;
                }

                // If we have a range, it means there is alt text that should be removed
                if (endIndex > startIndex && endIndex <= spannableStringBuilder.length()) {
                    spannableStringBuilder.replace(startIndex, endIndex, "");
                }

                // We need an empty space to insert the ImageSpan into
                imagePlaceholder = " ";

                // Move the image to a new line if needed
                int previousCharIndex = (startIndex > 0) ? startIndex - 1 : 0;
                if (!spannableHasCharacterAtIndex(spannableStringBuilder, '\n', previousCharIndex)
                        || spannableStringBuilder.getSpans(startIndex, startIndex, ImageSpan.class).length > 0) {
                    imagePlaceholder = "\n ";
                }

                int spanIndex = startIndex + imagePlaceholder.length() - 1;

                // Add a newline after the image if needed
                if (!spannableHasCharacterAtIndex(spannableStringBuilder, '\n', startIndex)
                        && !spannableHasCharacterAtIndex(spannableStringBuilder, '\r', startIndex)) {
                    imagePlaceholder += "\n";
                }

                spannableStringBuilder.insert(startIndex, imagePlaceholder);

                // Add the image span
                spannableStringBuilder.setSpan(
                        noteImageSpan,
                        spanIndex,
                        spanIndex + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // Add an AlignmentSpan to center the image
                spannableStringBuilder.setSpan(
                        new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                        spanIndex,
                        spanIndex + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                indexAdjustment += imagePlaceholder.length();
            }
        }
    }

    public static boolean spannableHasCharacterAtIndex(Spannable spannable, char character, int index) {
        return spannable != null && index < spannable.length() && spannable.charAt(index) == character;
    }

    public static boolean validate2FAuthorizationTokenFromIntentExtras(Intent intent, TwoFactorAuthCallback callback) {
        // Check for push authorization request
        if (intent != null && intent.hasExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN)) {
            Bundle extras = intent.getExtras();
            String token = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TOKEN, "");
            String title = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TITLE, "");
            String message = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_MESSAGE, "");
            long expires = extras.getLong(NotificationsUtils.ARG_PUSH_AUTH_EXPIRES, 0);

            long now = System.currentTimeMillis() / 1000;
            if (expires > 0 && now > expires) {
                callback.onTokenInvalid();
                return false;
            } else {
                callback.onTokenValid(token, title, message);
                return true;
            }
        }
        return false;
    }


    public static void showPushAuthAlert(Context context, final String token, String title, String message) {
        if (context == null ||
                TextUtils.isEmpty(token) ||
                TextUtils.isEmpty(title) ||
                TextUtils.isEmpty(message)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(message);

        builder.setPositiveButton(R.string.mnu_comment_approve, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendTwoFactorAuthToken(token);
            }
        });

        builder.setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_IGNORED);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void sendTwoFactorAuthToken(String token){
        // ping the push auth endpoint with the token, wp.com will take care of the rest!
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("action", "authorize_login");
        tokenMap.put("push_token", token);
        WordPress.getRestClientUtilsV1_1().post(PUSH_AUTH_ENDPOINT, tokenMap, null, null,
                new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_FAILED);
                    }
                });

        AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_APPROVED);
    }

    // Checks if global notifications toggle is enabled in the Android app settings
    // See: https://code.google.com/p/android/issues/detail?id=38482#c15
    @SuppressWarnings("unchecked")
    @TargetApi(19)
    public static boolean notificationsEnabledEh(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            ApplicationInfo appInfo = context.getApplicationInfo();
            String pkg = context.getApplicationContext().getPackageName();
            int uid = appInfo.uid;

            Class appOpsClass;
            try {
                appOpsClass = Class.forName(AppOpsManager.class.getName());

                Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE, String.class);

                Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
                int value = (int) opPostNotificationValue.get(Integer.class);

                return ((int) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException |
                    IllegalAccessException | InvocationTargetException e) {
                AppLog.e(T.NOTIFS, e.getMessage());
            }
        }

        // Default to assuming notifications are enabled
        return true;
    }

    public static boolean buildNoteObjectFromBundleAndSaveIt(Bundle data) {
        Note note = buildNoteObjectFromBundle(data);
        if (note != null) {
            return NotificationsTable.saveNote(note);
        }

        return false;
    }

    public static Note buildNoteObjectFromBundle(Bundle data) {

        if (data == null) {
            AppLog.e(T.NOTIFS, "Bundle is null! Cannot read '" + GCMMessageService.PUSH_ARG_NOTE_ID + "'.");
            return null;
        }

        Note note;
        String noteId = data.getString(GCMMessageService.PUSH_ARG_NOTE_ID, "");
        String base64FullData = data.getString(GCMMessageService.PUSH_ARG_NOTE_FULL_DATA);
        note = Note.buildFromBase64EncodedData(noteId, base64FullData);
        if (note == null) {
            // At this point we don't have the note :(
            AppLog.w(T.NOTIFS, "Cannot build the Note object by using info available in the PN payload. Please see " +
                    "previous log messages for detailed information about the error.");
        }

        return note;
    }

    public static int findNoteInNoteArray(List<Note> notes, String noteIdToSearchFor) {
        if (notes == null || TextUtils.isEmpty(noteIdToSearchFor)) return -1;

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (noteIdToSearchFor.equals(note.getId()))
                return i;
        }
        return -1;
    }

}
