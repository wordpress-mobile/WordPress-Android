package org.wordpress.android.ui.notifications.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.VolleyError;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.tools.FormattableContent;
import org.wordpress.android.fluxc.tools.FormattableContentMapper;
import org.wordpress.android.fluxc.tools.FormattableMedia;
import org.wordpress.android.fluxc.tools.FormattableRange;
import org.wordpress.android.models.Note;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.ui.notifications.blocks.NoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan;
import org.wordpress.android.ui.notifications.blocks.NoteBlockLinkMovementMethod;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.PackageUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.image.getters.WPCustomImageGetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

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
        if (uuid == null) {
            return;
        }

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
                    if (deviceID == null) {
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
        String deviceID = settings.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null);
        if (TextUtils.isEmpty(deviceID)) {
            return;
        }
        WordPress.getRestClientUtils().post("/devices/" + deviceID + "/delete", listener, errorListener);
    }

    static FormattableContent mapJsonToFormattableContent(FormattableContentMapper mapper, JSONObject blockObject) {
        return mapper.mapToFormattableContent(blockObject.toString());
    }

    static SpannableStringBuilder getSpannableContentForRanges(
            FormattableContentMapper formattableContentMapper,
            JSONObject blockObject, TextView textView,
            final NoteBlock.OnNoteBlockTextClickListener onNoteBlockTextClickListener,
            boolean isFooter) {
        return getSpannableContentForRanges(mapJsonToFormattableContent(formattableContentMapper, blockObject),
                textView, onNoteBlockTextClickListener, isFooter);
    }

    /**
     * Returns a spannable with formatted content based on WP.com note content 'range' data
     *
     * @param formattableContent the data
     * @param textView the TextView that will display the spannnable
     * @param onNoteBlockTextClickListener - click listener for ClickableSpans in the spannable
     * @param isFooter - Set if spannable should apply special formatting
     * @return Spannable string with formatted content
     */
    static SpannableStringBuilder getSpannableContentForRanges(FormattableContent formattableContent, TextView textView,
                                                  final NoteBlock.OnNoteBlockTextClickListener
                                                          onNoteBlockTextClickListener,
                                                  boolean isFooter) {
        Function1<NoteBlockClickableSpan, Unit> clickListener =
                onNoteBlockTextClickListener != null ? new Function1<NoteBlockClickableSpan, Unit>() {
                    @Override public Unit invoke(NoteBlockClickableSpan noteBlockClickableSpan) {
                        onNoteBlockTextClickListener.onNoteBlockTextClicked(noteBlockClickableSpan);
                        return null;
                    }
                } : null;
        return getSpannableContentForRanges(formattableContent,
                textView,
                isFooter,
                clickListener);
    }

    /**
     * Returns a spannable with formatted content based on WP.com note content 'range' data
     *
     * @param formattableContent the data
     * @param textView the TextView that will display the spannnable
     * @param clickHandler - click listener for ClickableSpans in the spannable
     * @param isFooter - Set if spannable should apply special formatting
     * @return Spannable string with formatted content
     */
    static SpannableStringBuilder getSpannableContentForRanges(FormattableContent formattableContent,
                                                  TextView textView,
                                                  final Function1<FormattableRange, Unit> clickHandler,
                                                  boolean isFooter) {
        Function1<NoteBlockClickableSpan, Unit> clickListener =
                clickHandler != null ? new Function1<NoteBlockClickableSpan, Unit>() {
                    @Override public Unit invoke(NoteBlockClickableSpan noteBlockClickableSpan) {
                        clickHandler.invoke(noteBlockClickableSpan.getFormattableRange());
                        return null;
                    }
                } : null;
        return getSpannableContentForRanges(formattableContent,
                textView,
                isFooter,
                clickListener);
    }

    /**
     * Returns a spannable with formatted content based on WP.com note content 'range' data
     *
     * @param formattableContent the data
     * @param textView the TextView that will display the spannnable
     * @param onNoteBlockTextClickListener - click listener for ClickableSpans in the spannable
     * @param isFooter - Set if spannable should apply special formatting
     * @return Spannable string with formatted content
     */
    private static SpannableStringBuilder getSpannableContentForRanges(FormattableContent formattableContent,
                                                          TextView textView,
                                                          boolean isFooter,
                                                          final Function1<NoteBlockClickableSpan, Unit>
                                                                  onNoteBlockTextClickListener) {
        if (formattableContent == null) {
            return new SpannableStringBuilder();
        }

        String text = formattableContent.getText();
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);

        boolean shouldLink = onNoteBlockTextClickListener != null;

        // Add ImageSpans for note media
        addImageSpansForBlockMedia(textView, formattableContent, spannableStringBuilder);

        // Process Ranges to add links and text formatting
        List<FormattableRange> rangesArray = formattableContent.getRanges();
        if (rangesArray != null) {
            for (FormattableRange range : rangesArray) {
                NoteBlockClickableSpan clickableSpan =
                        new NoteBlockClickableSpan(range, shouldLink, isFooter) {
                    @Override
                    public void onClick(View widget) {
                        if (onNoteBlockTextClickListener != null) {
                            onNoteBlockTextClickListener.invoke(this);
                        }
                    }
                };

                List<Integer> indices = clickableSpan.getIndices();
                if (indices != null && indices.size() == 2 && indices.get(0) <= spannableStringBuilder.length()
                    && indices.get(1) <= spannableStringBuilder.length()) {
                    spannableStringBuilder
                            .setSpan(clickableSpan, indices.get(0), indices.get(1), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    // Add additional styling if the range wants it
                    if (clickableSpan.getSpanStyle() != Typeface.NORMAL) {
                        StyleSpan styleSpan = new StyleSpan(clickableSpan.getSpanStyle());
                        spannableStringBuilder
                                .setSpan(styleSpan, indices.get(0), indices.get(1), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }

                    if (onNoteBlockTextClickListener != null && textView != null) {
                        textView.setLinksClickable(true);
                        textView.setMovementMethod(new NoteBlockLinkMovementMethod());
                    }
                }
            }
        }

        return spannableStringBuilder;
    }

    public static int[] getIndicesForRange(JSONObject rangeObject) {
        int[] indices = new int[]{0, 0};
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
    private static void addImageSpansForBlockMedia(TextView textView, FormattableContent subject,
                                                   SpannableStringBuilder spannableStringBuilder) {
        if (textView == null || subject == null || spannableStringBuilder == null) {
            return;
        }

        Context context = textView.getContext();
        List<FormattableMedia> mediaArray = subject.getMedia();
        if (context == null || mediaArray == null) {
            return;
        }

        // Note: notifications_max_image_size seems to be the max size an ImageSpan can handle,
        // otherwise it would load blank white
        WPCustomImageGetter imageGetter = new WPCustomImageGetter(textView,
                context.getResources().getDimensionPixelSize(R.dimen.notifications_max_image_size));

        int indexAdjustment = 0;
        String imagePlaceholder;
        for (FormattableMedia mediaObject : mediaArray) {
            if (mediaObject == null) {
                continue;
            }

            final Drawable remoteDrawable = imageGetter.getDrawable(StringUtils.notNullStr(mediaObject.getUrl()));
            ImageSpan noteImageSpan = new ImageSpan(remoteDrawable, StringUtils.notNullStr(mediaObject.getUrl()));
            int startIndex = -1;
            int endIndex = -1;
            List<Integer> indices =
                    (mediaObject.getIndices() != null && mediaObject.getIndices().size() == 2) ? mediaObject
                            .getIndices() : null;
            if (indices != null) {
                startIndex = indices.get(0);
                endIndex = indices.get(1);
            }
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
        if (context == null
            || TextUtils.isEmpty(token)
            || TextUtils.isEmpty(title)
            || TextUtils.isEmpty(message)) {
            return;
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
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

    public static void sendTwoFactorAuthToken(String token) {
        // ping the push auth endpoint with the token, wp.com will take care of the rest!
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("action", "authorize_login");
        tokenMap.put("push_token", token);
        WordPress.getRestClientUtilsV1_1().post(
                PUSH_AUTH_ENDPOINT, tokenMap, null, null,
                new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_FAILED);
                    }
                });

        AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_APPROVED);
    }

    /**
     * Checks if global notifications toggle is enabled in the Android app settings
     */
    public static boolean isNotificationsEnabled(Context context) {
        return NotificationManagerCompat.from(context.getApplicationContext()).areNotificationsEnabled();
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
            AppLog.w(T.NOTIFS, "Cannot build the Note object by using info available in the PN payload. Please see "
                               + "previous log messages for detailed information about the error.");
        }

        return note;
    }

    public static int findNoteInNoteArray(List<Note> notes, String noteIdToSearchFor) {
        if (notes == null || TextUtils.isEmpty(noteIdToSearchFor)) {
            return -1;
        }

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (noteIdToSearchFor.equals(note.getId())) {
                return i;
            }
        }
        return -1;
    }
}
