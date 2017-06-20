package org.wordpress.android.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AnalyticsTracker {
    private static boolean mHasUserOptedOut;

    public static final String READER_DETAIL_TYPE_KEY = "post_detail_type";
    public static final String READER_DETAIL_TYPE_NORMAL = "normal";
    public static final String READER_DETAIL_TYPE_BLOG_PREVIEW = "preview-blog";
    public static final String READER_DETAIL_TYPE_TAG_PREVIEW = "preview-tag";

    public enum Stat {
        APPLICATION_OPENED,
        APPLICATION_CLOSED,
        APPLICATION_INSTALLED,
        APPLICATION_UPGRADED,
        READER_ACCESSED,
        READER_ARTICLE_COMMENTED_ON,
        READER_ARTICLE_COMMENTS_OPENED,
        READER_ARTICLE_COMMENT_LIKED,
        READER_ARTICLE_COMMENT_UNLIKED,
        READER_ARTICLE_LIKED,
        READER_ARTICLE_OPENED,
        READER_ARTICLE_UNLIKED,
        READER_ARTICLE_RENDERED,
        READER_BLOG_BLOCKED,
        READER_BLOG_FOLLOWED,
        READER_BLOG_PREVIEWED,
        READER_BLOG_UNFOLLOWED,
        READER_DISCOVER_VIEWED,
        READER_INFINITE_SCROLL,
        READER_LIST_FOLLOWED,
        READER_LIST_LOADED,
        READER_LIST_PREVIEWED,
        READER_LIST_UNFOLLOWED,
        READER_TAG_FOLLOWED,
        READER_TAG_LOADED,
        READER_TAG_PREVIEWED,
        READER_TAG_UNFOLLOWED,
        READER_SEARCH_LOADED,
        READER_SEARCH_PERFORMED,
        READER_SEARCH_RESULT_TAPPED,
        READER_GLOBAL_RELATED_POST_CLICKED,
        READER_LOCAL_RELATED_POST_CLICKED,
        READER_VIEWPOST_INTERCEPTED,
        READER_BLOG_POST_INTERCEPTED,
        READER_FEED_POST_INTERCEPTED,
        READER_WPCOM_BLOG_POST_INTERCEPTED,
        READER_SIGN_IN_INITIATED,
        READER_WPCOM_SIGN_IN_NEEDED,
        READER_USER_UNAUTHORIZED,
        STATS_ACCESSED,
        STATS_INSIGHTS_ACCESSED,
        STATS_PERIOD_DAYS_ACCESSED,
        STATS_PERIOD_WEEKS_ACCESSED,
        STATS_PERIOD_MONTHS_ACCESSED,
        STATS_PERIOD_YEARS_ACCESSED,
        STATS_VIEW_ALL_ACCESSED,
        STATS_SINGLE_POST_ACCESSED,
        STATS_TAPPED_BAR_CHART,
        STATS_SCROLLED_TO_BOTTOM,
        STATS_WIDGET_ADDED,
        STATS_WIDGET_REMOVED,
        STATS_WIDGET_TAPPED,
        EDITOR_CREATED_POST,
        EDITOR_ADDED_PHOTO_VIA_DEVICE_LIBRARY,
        EDITOR_ADDED_VIDEO_VIA_DEVICE_LIBRARY,
        EDITOR_ADDED_PHOTO_NEW,
        EDITOR_ADDED_VIDEO_NEW,
        EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY,
        EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY,
        MEDIA_PHOTO_OPTIMIZED,
        MEDIA_PHOTO_OPTIMIZE_ERROR,
        MEDIA_VIDEO_OPTIMIZED,
        MEDIA_VIDEO_CANT_OPTIMIZE,
        MEDIA_VIDEO_OPTIMIZE_ERROR,
        MEDIA_PICKER_OPEN_CAPTURE_MEDIA,
        MEDIA_PICKER_OPEN_DEVICE_LIBRARY,
        MEDIA_PICKER_OPEN_WP_MEDIA,
        MEDIA_PICKER_RECENT_MEDIA_SELECTED,
        MEDIA_PICKER_PREVIEW_OPENED,
        EDITOR_UPDATED_POST,
        EDITOR_SCHEDULED_POST,
        EDITOR_CLOSED,
        EDITOR_PUBLISHED_POST,
        EDITOR_SAVED_DRAFT,
        EDITOR_DISCARDED_CHANGES,
        EDITOR_EDITED_IMAGE, // Visual editor only
        EDITOR_HYBRID_ENABLED, // Visual editor only
        EDITOR_HYBRID_TOGGLED_OFF, // Visual editor only
        EDITOR_HYBRID_TOGGLED_ON, // Visual editor only
        EDITOR_UPLOAD_MEDIA_FAILED, // Visual editor only
        EDITOR_UPLOAD_MEDIA_RETRIED, // Visual editor only
        EDITOR_TAPPED_BLOCKQUOTE,
        EDITOR_TAPPED_BOLD,
        EDITOR_TAPPED_HTML, // Visual editor only
        EDITOR_TAPPED_IMAGE,
        EDITOR_TAPPED_ITALIC,
        EDITOR_TAPPED_LINK,
        EDITOR_TAPPED_MORE,
        EDITOR_TAPPED_STRIKETHROUGH,
        EDITOR_TAPPED_UNDERLINE,
        EDITOR_TAPPED_ORDERED_LIST, // Visual editor only
        EDITOR_TAPPED_UNLINK, // Visual editor only
        EDITOR_TAPPED_UNORDERED_LIST, // Visual editor only
        EDITOR_AZTEC_TOGGLED_OFF, // Aztec editor only
        EDITOR_AZTEC_TOGGLED_ON, // Aztec editor only
        EDITOR_AZTEC_ENABLED, // Aztec editor only
        ME_ACCESSED,
        ME_GRAVATAR_TAPPED,
        ME_GRAVATAR_TOOLTIP_TAPPED,
        ME_GRAVATAR_SHOT_NEW,
        ME_GRAVATAR_GALLERY_PICKED,
        ME_GRAVATAR_CROPPED,
        ME_GRAVATAR_UPLOADED,
        ME_GRAVATAR_UPLOAD_UNSUCCESSFUL,
        ME_GRAVATAR_UPLOAD_EXCEPTION,
        MY_SITE_ACCESSED,
        NOTIFICATIONS_ACCESSED,
        NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS,
        NOTIFICATIONS_MISSING_SYNC_WARNING,
        NOTIFICATION_REPLIED_TO,
        NOTIFICATION_QUICK_ACTIONS_REPLIED_TO,
        NOTIFICATION_APPROVED,
        NOTIFICATION_QUICK_ACTIONS_APPROVED,
        NOTIFICATION_UNAPPROVED,
        NOTIFICATION_LIKED,
        NOTIFICATION_QUICK_ACTIONS_LIKED,
        NOTIFICATION_UNLIKED,
        NOTIFICATION_TRASHED,
        NOTIFICATION_FLAGGED_AS_SPAM,
        NOTIFICATION_SWIPE_PAGE_CHANGED,
        NOTIFICATION_PENDING_DRAFTS_TAPPED,
        NOTIFICATION_PENDING_DRAFTS_IGNORED,
        NOTIFICATION_PENDING_DRAFTS_DISMISSED,
        NOTIFICATION_PENDING_DRAFTS_SETTINGS_ENABLED,
        NOTIFICATION_PENDING_DRAFTS_SETTINGS_DISABLED,
        OPENED_POSTS,
        OPENED_PAGES,
        OPENED_COMMENTS,
        OPENED_VIEW_SITE,
        OPENED_VIEW_SITE_FROM_HEADER,
        OPENED_VIEW_ADMIN,
        OPENED_MEDIA_LIBRARY,
        OPENED_BLOG_SETTINGS,
        OPENED_ACCOUNT_SETTINGS,
        OPENED_APP_SETTINGS,
        OPENED_MY_PROFILE,
        OPENED_PEOPLE_MANAGEMENT,
        OPENED_PERSON,
        CREATE_ACCOUNT_INITIATED,
        CREATE_ACCOUNT_EMAIL_EXISTS,
        CREATE_ACCOUNT_USERNAME_EXISTS,
        CREATE_ACCOUNT_FAILED,
        CREATED_ACCOUNT,
        CREATED_SITE,
        ACCOUNT_LOGOUT,
        SHARED_ITEM,
        ADDED_SELF_HOSTED_SITE,
        SIGNED_IN,
        SIGNED_INTO_JETPACK,
        PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN,
        STATS_SELECTED_INSTALL_JETPACK,
        STATS_SELECTED_CONNECT_JETPACK,
        PUSH_NOTIFICATION_RECEIVED,
        PUSH_NOTIFICATION_TAPPED, // Same of opened
        SUPPORT_OPENED_HELPSHIFT_SCREEN,
        SUPPORT_USER_ACCEPTED_THE_SOLUTION,
        SUPPORT_USER_REJECTED_THE_SOLUTION,
        SUPPORT_USER_SENT_SCREENSHOT,
        SUPPORT_USER_REVIEWED_THE_APP,
        SUPPORT_USER_REPLIED_TO_HELPSHIFT,
        LOGIN_ACCESSED,
        LOGIN_MAGIC_LINK_EXITED,
        LOGIN_MAGIC_LINK_FAILED,
        LOGIN_MAGIC_LINK_OPENED,
        LOGIN_MAGIC_LINK_REQUESTED,
        LOGIN_MAGIC_LINK_SUCCEEDED,
        LOGIN_FAILED,
        LOGIN_FAILED_TO_GUESS_XMLRPC,
        LOGIN_INSERTED_INVALID_URL,
        LOGIN_AUTOFILL_CREDENTIALS_FILLED,
        LOGIN_AUTOFILL_CREDENTIALS_UPDATED,
        MEDIA_LIBRARY_ADDED_PHOTO,
        MEDIA_LIBRARY_ADDED_VIDEO,
        PERSON_REMOVED,
        PERSON_UPDATED,
        PUSH_AUTHENTICATION_APPROVED,
        PUSH_AUTHENTICATION_EXPIRED,
        PUSH_AUTHENTICATION_FAILED,
        PUSH_AUTHENTICATION_IGNORED,
        NOTIFICATION_SETTINGS_LIST_OPENED,
        NOTIFICATION_SETTINGS_STREAMS_OPENED,
        NOTIFICATION_SETTINGS_DETAILS_OPENED,
        THEMES_ACCESSED_THEMES_BROWSER,
        THEMES_ACCESSED_SEARCH,
        THEMES_CHANGED_THEME,
        THEMES_PREVIEWED_SITE,
        THEMES_DEMO_ACCESSED,
        THEMES_CUSTOMIZE_ACCESSED,
        THEMES_SUPPORT_ACCESSED,
        THEMES_DETAILS_ACCESSED,
        ACCOUNT_SETTINGS_LANGUAGE_CHANGED,
        SITE_SETTINGS_ACCESSED,
        SITE_SETTINGS_ACCESSED_MORE_SETTINGS,
        SITE_SETTINGS_LEARN_MORE_CLICKED,
        SITE_SETTINGS_LEARN_MORE_LOADED,
        SITE_SETTINGS_ADDED_LIST_ITEM,
        SITE_SETTINGS_DELETED_LIST_ITEMS,
        SITE_SETTINGS_SAVED_REMOTELY,
        SITE_SETTINGS_HINT_TOAST_SHOWN,
        SITE_SETTINGS_START_OVER_ACCESSED,
        SITE_SETTINGS_START_OVER_CONTACT_SUPPORT_CLICKED,
        SITE_SETTINGS_EXPORT_SITE_ACCESSED,
        SITE_SETTINGS_EXPORT_SITE_REQUESTED,
        SITE_SETTINGS_EXPORT_SITE_RESPONSE_OK,
        SITE_SETTINGS_EXPORT_SITE_RESPONSE_ERROR,
        SITE_SETTINGS_DELETE_SITE_ACCESSED,
        SITE_SETTINGS_DELETE_SITE_PURCHASES_REQUESTED,
        SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOWN,
        SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOW_CLICKED,
        SITE_SETTINGS_DELETE_SITE_REQUESTED,
        SITE_SETTINGS_DELETE_SITE_RESPONSE_OK,
        SITE_SETTINGS_DELETE_SITE_RESPONSE_ERROR,
        SITE_SETTINGS_OPTIMIZE_IMAGES_CHANGED,
        ABTEST_START,
        TRAIN_TRACKS_RENDER,
        TRAIN_TRACKS_INTERACT,
        DEEP_LINKED,
        DEEP_LINKED_FALLBACK,
        DEEP_LINK_NOT_DEFAULT_HANDLER,
        MEDIA_UPLOAD_STARTED,
        MEDIA_UPLOAD_ERROR,
        MEDIA_UPLOAD_SUCCESS,
        MEDIA_UPLOAD_CANCELED,
        APP_PERMISSION_GRANTED,
        APP_PERMISSION_DENIED
    }

    private static final List<Tracker> TRACKERS = new ArrayList<>();

    private AnalyticsTracker() {
    }

    public static void init(Context context) {
        loadPrefHasUserOptedOut(context);
    }

    public static void loadPrefHasUserOptedOut(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hasUserOptedOut = !prefs.getBoolean("wp_pref_send_usage_stats", true);
        if (hasUserOptedOut != mHasUserOptedOut) {
            mHasUserOptedOut = hasUserOptedOut;
        }
    }

    public static void registerTracker(Tracker tracker) {
        if (tracker != null) {
            TRACKERS.add(tracker);
        }
    }

    public static void track(Stat stat) {
        if (mHasUserOptedOut) {
            return;
        }
        for (Tracker tracker : TRACKERS) {
            tracker.track(stat);
        }
    }

    public static void track(Stat stat, Map<String, ?> properties) {
        if (mHasUserOptedOut) {
            return;
        }
        for (Tracker tracker : TRACKERS) {
            tracker.track(stat, properties);
        }
    }

    /**
     * A convenience method for logging an error event with some additional meta data.
     * @param stat The stat to track.
     * @param errorContext A string providing additional context (if any) about the error.
     * @param errorType The type of error.
     * @param errorDescription The error text or other description.
     */
    public static void track(Stat stat, String errorContext, String errorType, String errorDescription) {
        Map<String, String> props = new HashMap<>();
        props.put("error_context", errorContext);
        props.put("error_type", errorType);
        props.put("error_description", errorDescription);
        track(stat, props);
    }

    public static void flush() {
        if (mHasUserOptedOut) {
            return;
        }
        for (Tracker tracker : TRACKERS) {
            tracker.flush();
        }
    }

    public static void endSession(boolean force) {
        if (mHasUserOptedOut && !force) {
            return;
        }
        for (Tracker tracker : TRACKERS) {
            tracker.endSession();
        }
    }

    public static void registerPushNotificationToken(String regId) {
        if (mHasUserOptedOut) {
            return;
        }
        for (Tracker tracker : TRACKERS) {
            tracker.registerPushNotificationToken(regId);
        }
    }

    public static void clearAllData() {
        for (Tracker tracker : TRACKERS) {
            tracker.clearAllData();
        }
    }

    public static void refreshMetadata(AnalyticsMetadata metadata) {
        for (Tracker tracker : TRACKERS) {
            tracker.refreshMetadata(metadata);
        }
    }
}
