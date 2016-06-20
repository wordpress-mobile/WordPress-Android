package org.wordpress.android.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
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
        READER_ARTICLE_LIKED,
        READER_ARTICLE_OPENED,
        READER_ARTICLE_UNLIKED,
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
        EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY,
        EDITOR_ADDED_VIDEO_VIA_LOCAL_LIBRARY,
        EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY,
        EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY,
        EDITOR_UPDATED_POST,
        EDITOR_SCHEDULED_POST,
        EDITOR_CLOSED,
        EDITOR_PUBLISHED_POST,
        EDITOR_SAVED_DRAFT,
        EDITOR_DISCARDED_CHANGES,
        EDITOR_EDITED_IMAGE, // Visual editor only
        EDITOR_ENABLED_NEW_VERSION, // Visual editor only
        EDITOR_TOGGLED_OFF, // Visual editor only
        EDITOR_TOGGLED_ON, // Visual editor only
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
        ME_ACCESSED,
        ME_GRAVATAR_TAPPED,
        ME_GRAVATAR_TOOLTIP_TAPPED,
        ME_GRAVATAR_PERMISSIONS_INTERRUPTED,
        ME_GRAVATAR_PERMISSIONS_DENIED,
        ME_GRAVATAR_PERMISSIONS_ACCEPTED,
        ME_GRAVATAR_SHOT_NEW,
        ME_GRAVATAR_GALLERY_PICKED,
        ME_GRAVATAR_CROPPED,
        ME_GRAVATAR_UPLOADED,
        ME_GRAVATAR_UPLOAD_UNSUCCESSFUL,
        ME_GRAVATAR_UPLOAD_EXCEPTION,
        MY_SITE_ACCESSED,
        NOTIFICATIONS_ACCESSED,
        NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS,
        NOTIFICATION_REPLIED_TO,
        NOTIFICATION_APPROVED,
        NOTIFICATION_UNAPPROVED,
        NOTIFICATION_LIKED,
        NOTIFICATION_UNLIKED,
        NOTIFICATION_TRASHED,
        NOTIFICATION_FLAGGED_AS_SPAM,
        OPENED_POSTS,
        OPENED_PAGES,
        OPENED_COMMENTS,
        OPENED_VIEW_SITE,
        OPENED_VIEW_ADMIN,
        OPENED_MEDIA_LIBRARY,
        OPENED_BLOG_SETTINGS,
        OPENED_ACCOUNT_SETTINGS,
        OPENED_APP_SETTINGS,
        OPENED_MY_PROFILE,
        OPENED_PEOPLE_MANAGEMENT,
        OPENED_PERSON,
        CREATED_ACCOUNT,
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
        SUPPORT_SENT_REPLY_TO_SUPPORT_MESSAGE,
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
        ABTEST_START
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

    public static void refreshMetadata(boolean isUserConnected, boolean isWordPressComUser, boolean isJetpackUser,
                                       int sessionCount, int numBlogs, int versionCode, String username, String email) {
        for (Tracker tracker : TRACKERS) {
            tracker.refreshMetadata(isUserConnected, isWordPressComUser, isJetpackUser, sessionCount, numBlogs,
                    versionCode, username, email);
        }
    }
}
