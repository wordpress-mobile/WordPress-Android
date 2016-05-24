package org.wordpress.android.ui.notifications.utils;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
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
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.comments.CommentActionResult;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.notifications.NotificationEvents.NoteModerationFailed;
import org.wordpress.android.ui.notifications.NotificationEvents.NoteModerationStatusChanged;
import org.wordpress.android.ui.notifications.NotificationEvents.NoteVisibilityChanged;
import org.wordpress.android.ui.notifications.NotificationsDetailActivity;
import org.wordpress.android.ui.notifications.blocks.NoteBlock;
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.helpers.WPImageGetter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class NotificationsUtils {
    public static final String ARG_PUSH_AUTH_TOKEN = "arg_push_auth_token";
    public static final String ARG_PUSH_AUTH_TITLE = "arg_push_auth_title";
    public static final String ARG_PUSH_AUTH_MESSAGE = "arg_push_auth_message";
    public static final String ARG_PUSH_AUTH_EXPIRES = "arg_push_auth_expires";

    public static final String WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS = "wp_pref_notification_settings";
    public static final String WPCOM_PUSH_DEVICE_UUID = "wp_pref_notifications_uuid";
    public static final String WPCOM_PUSH_DEVICE_TOKEN = "wp_pref_notifications_token";

    public static final String WPCOM_PUSH_DEVICE_SERVER_ID = "wp_pref_notifications_server_id";
    private static final String PUSH_AUTH_ENDPOINT = "me/two-step/push-authentication";

    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

    private static final String WPCOM_SETTINGS_ENDPOINT = "/me/notifications/settings/";

    private static boolean mSnackbarDidUndo;

    public static void getPushNotificationSettings(Context context, RestRequest.Listener listener,
                                                   RestRequest.ErrorListener errorListener) {
        if (!AccountHelper.isSignedInWordPressDotCom()) {
            return;
        }
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
     * @param isFooter - Set if spannable should apply special formatting
     * @return Spannable string with formatted content
     */
    public static Spannable getSpannableContentForRanges(JSONObject blockObject, TextView textView,
                                                         final NoteBlock.OnNoteBlockTextClickListener onNoteBlockTextClickListener,
                                                         boolean isFooter) {
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
                        shouldLink, isFooter) {
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
        Drawable failed = context.getResources().getDrawable(R.drawable.noticon_warning_big_grey);
        // Note: notifications_max_image_size seems to be the max size an ImageSpan can handle,
        // otherwise it would load blank white
        WPImageGetter imageGetter = new WPImageGetter(
                textView,
                context.getResources().getDimensionPixelSize(R.dimen.notifications_max_image_size),
                WordPress.imageLoader,
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

    public static void handleNoteBlockSpanClick(NotificationsDetailActivity activity, NoteBlockClickableSpan clickedSpan) {
        switch (clickedSpan.getRangeType()) {
            case SITE:
                // Show blog preview
                activity.showBlogPreviewActivity(clickedSpan.getId());
                break;
            case USER:
                // Show blog preview
                activity.showBlogPreviewActivity(clickedSpan.getSiteId());
                break;
            case POST:
                // Show post detail
                activity.showPostActivity(clickedSpan.getSiteId(), clickedSpan.getId());
                break;
            case COMMENT:
                // Load the comment in the reader list if it exists, otherwise show a webview
                if (ReaderUtils.postAndCommentExists(clickedSpan.getSiteId(), clickedSpan.getPostId(), clickedSpan.getId())) {
                    activity.showReaderCommentsList(clickedSpan.getSiteId(), clickedSpan.getPostId(), clickedSpan.getId());
                } else {
                    activity.showWebViewActivityForUrl(clickedSpan.getUrl());
                }
                break;
            case STAT:
            case FOLLOW:
                // We can open native stats if the site is a wpcom or Jetpack + stored in the app locally.
                // Note that for Jetpack sites we need the options already synced. That happens when the user
                // selects the site in the sites picker. So adding it to the app doesn't always populate options.

               // Do not load Jetpack shadow sites here. They've empty options and Stats can't be loaded for them.
                Blog blog = WordPress.wpDB.getBlogForDotComBlogId(
                        String.valueOf(clickedSpan.getSiteId())
                );
                // Make sure blog is not null, and it's either JP or dotcom. Better safe than sorry.
                if (blog == null ||  blog.getLocalTableBlogId() <= 0 || (!blog.isDotcomFlag() && !blog.isJetpackPowered())) {
                    activity.showWebViewActivityForUrl(clickedSpan.getUrl());
                    break;
                }
                activity.showStatsActivityForSite(blog.getLocalTableBlogId(), clickedSpan.getRangeType());
                break;
            case LIKE:
                if (ReaderPostTable.postExists(clickedSpan.getSiteId(), clickedSpan.getId())) {
                    activity.showReaderPostLikeUsers(clickedSpan.getSiteId(), clickedSpan.getId());
                } else {
                    activity.showPostActivity(clickedSpan.getSiteId(), clickedSpan.getId());
                }
                break;
            default:
                // We don't know what type of id this is, let's see if it has a URL and push a webview
                if (!TextUtils.isEmpty(clickedSpan.getUrl())) {
                    activity.showWebViewActivityForUrl(clickedSpan.getUrl());
                }
        }
    }

    public static boolean spannableHasCharacterAtIndex(Spannable spannable, char character, int index) {
        return spannable != null && index < spannable.length() && spannable.charAt(index) == character;
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

    private static void showUndoBarForNote(final Note note,
                                           final CommentStatus status,
                                           final View parentView) {
        Resources resources = parentView.getContext().getResources();
        String message = (status == CommentStatus.TRASH ? resources.getString(R.string.comment_trashed) : resources.getString(R.string.comment_spammed));
        View.OnClickListener undoListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSnackbarDidUndo = true;
                EventBus.getDefault().postSticky(new NoteVisibilityChanged(note.getId(), false));
            }
        };

        mSnackbarDidUndo = false;
        Snackbar snackbar = Snackbar.make(parentView, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, undoListener);

        // Deleted notifications in Simperium never come back, so we won't
        // make the request until the undo bar fades away
        snackbar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                if (mSnackbarDidUndo) {
                    return;
                }
                CommentActions.moderateCommentForNote(note, status,
                        new CommentActions.CommentActionListener() {
                            @Override
                            public void onActionResult(CommentActionResult result) {
                                if (!result.isSuccess()) {
                                    EventBus.getDefault().postSticky(new NoteVisibilityChanged(note.getId(), false));
                                    EventBus.getDefault().postSticky(new NoteModerationFailed());
                                }
                            }
                        });
            }
        });

        snackbar.show();
    }

    /**
     * Moderate a comment from a WPCOM notification.
     * Broadcast EventBus events on update/success/failure and show an undo bar if new status is Trash or Spam
     */
    public static void moderateCommentForNote(final Note note, final CommentStatus newStatus, final View parentView) {
        if (newStatus == CommentStatus.APPROVED || newStatus == CommentStatus.UNAPPROVED) {
            note.setLocalStatus(CommentStatus.toRESTString(newStatus));
            note.save();
            EventBus.getDefault().postSticky(new NoteModerationStatusChanged(note.getId(), true));
            CommentActions.moderateCommentForNote(note, newStatus,
                    new CommentActions.CommentActionListener() {
                        @Override
                        public void onActionResult(CommentActionResult result) {
                            EventBus.getDefault().postSticky(new NoteModerationStatusChanged(note.getId(), false));
                            if (!result.isSuccess()) {
                                note.setLocalStatus(null);
                                note.save();
                                EventBus.getDefault().postSticky(new NoteModerationFailed());
                            }
                        }
                    });
        } else if (newStatus == CommentStatus.TRASH || newStatus == CommentStatus.SPAM) {
            // Post as sticky, so that NotificationsListFragment can pick it up after it's created
            EventBus.getDefault().postSticky(new NoteVisibilityChanged(note.getId(), true));
            // Show undo bar for trash or spam actions
            showUndoBarForNote(note, newStatus, parentView);
        }
    }

    // Checks if global notifications toggle is enabled in the Android app settings
    // See: https://code.google.com/p/android/issues/detail?id=38482#c15
    @SuppressWarnings("unchecked")
    @TargetApi(19)
    public static boolean isNotificationsEnabled(Context context) {
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
                e.printStackTrace();
            }
        }

        // Default to assuming notifications are enabled
        return true;
    }
}
