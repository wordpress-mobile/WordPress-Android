package org.wordpress.android.ui.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.models.PeopleListFilter;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria;
import org.wordpress.android.ui.posts.AuthorFilterSelection;
import org.wordpress.android.ui.posts.PostListViewLayoutType;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPMediaUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppPrefs {
    private static final int THEME_IMAGE_SIZE_WIDTH_DEFAULT = 400;
    private static final int MAX_PENDING_DRAFTS_AMOUNT = 100;

    // store twice as many recent sites as we show
    private static final int MAX_RECENTLY_PICKED_SITES_TO_SHOW = 5;
    private static final int MAX_RECENTLY_PICKED_SITES_TO_SAVE = MAX_RECENTLY_PICKED_SITES_TO_SHOW * 2;

    public interface PrefKey {
        String name();

        String toString();
    }

    /**
     * Application related preferences. When the user disconnects, these preferences are erased.
     */
    public enum DeletablePrefKey implements PrefKey {
        // name of last shown activity
        LAST_ACTIVITY_STR,

        // last selected tag in the reader
        READER_TAG_NAME,
        READER_TAG_TYPE,
        READER_TAG_WAS_FOLLOWING,

        // last selected subfilter in the reader
        READER_SUBFILTER,

        // title of the last active page in ReaderSubsActivity
        READER_SUBS_PAGE_TITLE,

        // index of the last active page in main activity
        MAIN_PAGE_INDEX,

        // index of the last active item in Stats activity
        STATS_ITEM_INDEX,

        // Keep the associations between each widget_id/blog_id added to the app
        STATS_WIDGET_KEYS_BLOGS,

        // last data stored for the Stats Widgets
        STATS_WIDGET_DATA,

        // Store the number of times Stats are loaded without errors. It's used to show the Widget promo dialog.
        STATS_WIDGET_PROMO_ANALYTICS,

        // index of the last active status type in Comments activity
        COMMENTS_STATUS_TYPE_INDEX,

        // index of the last active people list filter in People Management activity
        PEOPLE_LIST_FILTER_INDEX,

        // selected site in the main activity
        SELECTED_SITE_LOCAL_ID,

        // wpcom ID of the last push notification received
        PUSH_NOTIFICATIONS_LAST_NOTE_ID,

        // local time of the last push notification received
        PUSH_NOTIFICATIONS_LAST_NOTE_TIME,

        // local IDs of sites recently chosen in the site picker
        RECENTLY_PICKED_SITE_IDS,

        // list of last time a notification has been created for a draft
        PENDING_DRAFTS_NOTIFICATION_LAST_NOTIFICATION_DATES,

        // Optimize Image and Video settings
        IMAGE_OPTIMIZE_ENABLED,
        IMAGE_OPTIMIZE_WIDTH,
        IMAGE_OPTIMIZE_QUALITY,
        VIDEO_OPTIMIZE_ENABLED,
        VIDEO_OPTIMIZE_WIDTH,
        VIDEO_OPTIMIZE_QUALITY, // Encoder max bitrate

        // Used to flag whether the app should strip geolocation from images
        STRIP_IMAGE_LOCATION,

        // Used to flag the account created stat needs to be bumped after account information is synced.
        SHOULD_TRACK_MAGIC_LINK_SIGNUP,

        // Support email address and name that's independent of any account or site
        SUPPORT_EMAIL,
        SUPPORT_NAME,

        // Store a version of the last dismissed News Card
        NEWS_CARD_DISMISSED_VERSION,
        // Store a version of the last shown News Card
        NEWS_CARD_SHOWN_VERSION,
        AVATAR_VERSION,
        GUTENBERG_DEFAULT_FOR_NEW_POSTS,
        USER_IN_GUTENBERG_ROLLOUT_GROUP,
        SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS,
        SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS_PHASE_2,
        GUTENBERG_OPT_IN_DIALOG_SHOWN,

        IS_QUICK_START_NOTICE_REQUIRED,

        POST_LIST_AUTHOR_FILTER,
        POST_LIST_VIEW_LAYOUT_TYPE,

        // Widget settings
        STATS_WIDGET_SELECTED_SITE_ID,
        STATS_WIDGET_COLOR_MODE,
        STATS_WIDGET_DATA_TYPE,
        STATS_WIDGET_HAS_DATA,

        // Keep the local_blog_id + local_post_id values that have HW Acc. turned off
        AZTEC_EDITOR_DISABLE_HW_ACC_KEYS,
    }

    /**
     * These preferences won't be deleted when the user disconnects. They should be used for device specifics or user
     * independent prefs.
     */
    public enum UndeletablePrefKey implements PrefKey {
        // Theme image size retrieval
        THEME_IMAGE_SIZE_WIDTH,

        // index of the last app-version
        LAST_APP_VERSION_INDEX,

        // aztec editor enabled
        AZTEC_EDITOR_ENABLED,

        // aztec editor toolbar expanded state
        AZTEC_EDITOR_TOOLBAR_EXPANDED,

        BOOKMARKS_SAVED_LOCALLY_DIALOG_SHOWN,

        // When we need to show the new image optimize promo dialog
        IMAGE_OPTIMIZE_PROMO_REQUIRED,

        // Global plans features
        GLOBAL_PLANS_PLANS_FEATURES,

        // When we need to sync IAP data with the wpcom backend
        IAP_SYNC_REQUIRED,

        // When we need to show the snackbar indicating how notifications can be navigated through
        SWIPE_TO_NAVIGATE_NOTIFICATIONS,

        // Same as above but for the reader
        SWIPE_TO_NAVIGATE_READER,

        // smart toast counters
        SMART_TOAST_COMMENTS_LONG_PRESS_USAGE_COUNTER,
        SMART_TOAST_COMMENTS_LONG_PRESS_TOAST_COUNTER,

        // permission keys - set once a specific permission has been asked, regardless of response
        ASKED_PERMISSION_STORAGE_WRITE,
        ASKED_PERMISSION_STORAGE_READ,
        ASKED_PERMISSION_CAMERA,

        // Updated after WP.com themes have been fetched
        LAST_WP_COM_THEMES_SYNC,

        // user id last used to login with
        LAST_USED_USER_ID,

        // last user access status in reader
        LAST_READER_KNOWN_ACCESS_TOKEN_STATUS,
        LAST_READER_KNOWN_USER_ID,

        // used to indicate that user opted out of quick start
        IS_QUICK_START_DISABLED,

        // quick start migration dialog is shown only once for all sites
        HAS_QUICK_START_MIGRATION_SHOWN,

        // used to indicate that we already obtained and tracked the installation referrer
        IS_INSTALLATION_REFERRER_OBTAINED,

        // used to indicate that user dont want to see the Gutenberg warning dialog anymore
        IS_GUTENBERG_WARNING_DIALOG_DISABLED,

        // used to indicate that user dont want to see the Gutenberg informative dialog anymore
        IS_GUTENBERG_INFORMATIVE_DIALOG_DISABLED,

        // indicates whether the system notifications are enabled for the app
        SYSTEM_NOTIFICATIONS_ENABLED,

        // Used to indicate whether or not the the post-signup interstitial must be shown
        SHOULD_SHOW_POST_SIGNUP_INTERSTITIAL,

        // used to indicate that we do not need to show the main FAB tooltip
        IS_MAIN_FAB_TOOLTIP_DISABLED,

        // version of the last shown feature announcement
        FEATURE_ANNOUNCEMENT_SHOWN_VERSION,

        // last app version code feature announcement was shown for
        LAST_FEATURE_ANNOUNCEMENT_APP_VERSION_CODE,
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
    }

    private static String getString(PrefKey key) {
        return getString(key, "");
    }

    private static String getString(PrefKey key, String defaultValue) {
        return prefs().getString(key.name(), defaultValue);
    }

    private static void setString(PrefKey key, String value) {
        SharedPreferences.Editor editor = prefs().edit();
        if (TextUtils.isEmpty(value)) {
            editor.remove(key.name());
        } else {
            editor.putString(key.name(), value);
        }
        editor.apply();
    }

    private static long getLong(PrefKey key) {
        return getLong(key, 0);
    }

    private static long getLong(PrefKey key, long defaultValue) {
        try {
            String value = getString(key);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static void setLong(PrefKey key, long value) {
        setString(key, Long.toString(value));
    }

    private static int getInt(PrefKey key, int def) {
        try {
            String value = getString(key);
            if (value.isEmpty()) {
                return def;
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int getInt(PrefKey key) {
        return getInt(key, 0);
    }

    public static void setInt(PrefKey key, int value) {
        setString(key, Integer.toString(value));
    }

    public static boolean getBoolean(PrefKey key, boolean def) {
        String value = getString(key, Boolean.toString(def));
        return Boolean.parseBoolean(value);
    }

    public static void setBoolean(PrefKey key, boolean value) {
        setString(key, Boolean.toString(value));
    }

    private static void remove(PrefKey key) {
        prefs().edit().remove(key.name()).apply();
    }

    public static boolean keyExists(@NonNull PrefKey key) {
        return prefs().contains(key.name());
    }

    // Exposed methods

    /**
     * remove all user-related preferences
     */
    public static void reset() {
        SharedPreferences.Editor editor = prefs().edit();
        for (DeletablePrefKey key : DeletablePrefKey.values()) {
            editor.remove(key.name());
        }
        editor.apply();
    }

    public static ReaderTag getReaderTag() {
        String tagName = getString(DeletablePrefKey.READER_TAG_NAME);
        if (TextUtils.isEmpty(tagName)) {
            return null;
        }
        int tagType = getInt(DeletablePrefKey.READER_TAG_TYPE);

        boolean wasFollowing = false;

        // The intention here is to check if the `DeletablePrefKey.READER_TAG_WAS_FOLLOWING` key
        // was present at all in the Shared Prefs.
        // We could have it not set for example in cases where user is upgrading from
        // a previous version of the app. In those cases we do not have enough information as of the saved
        // tag was a Following tag or not, so (as with empty `DeletablePrefKey.READER_TAG_NAME`)
        // let's do not use this piece of information.
        String wasFallowingString = getString(DeletablePrefKey.READER_TAG_WAS_FOLLOWING);
        if (TextUtils.isEmpty(wasFallowingString)) return null;

        wasFollowing = getBoolean(DeletablePrefKey.READER_TAG_WAS_FOLLOWING, false);

        return ReaderUtils.getTagFromTagName(tagName, ReaderTagType.fromInt(tagType), wasFollowing);
    }

    public static void setReaderTag(ReaderTag tag) {
        if (tag != null && !TextUtils.isEmpty(tag.getTagSlug())) {
            setString(DeletablePrefKey.READER_TAG_NAME, tag.getTagSlug());
            setInt(DeletablePrefKey.READER_TAG_TYPE, tag.tagType.toInt());
            setBoolean(
                    DeletablePrefKey.READER_TAG_WAS_FOLLOWING,
                    tag.isFollowedSites() || tag.isDefaultInMemoryTag()
            );
        } else {
            prefs().edit()
                   .remove(DeletablePrefKey.READER_TAG_NAME.name())
                   .remove(DeletablePrefKey.READER_TAG_TYPE.name())
                   .remove(DeletablePrefKey.READER_TAG_WAS_FOLLOWING.name())
                   .apply();
        }
    }

    public static String getReaderSubfilter() {
        return getString(DeletablePrefKey.READER_SUBFILTER);
    }

    public static void setReaderSubfilter(@NonNull String json) {
        setString(DeletablePrefKey.READER_SUBFILTER, json);
    }

    /**
     * title of the last active page in ReaderSubsActivity - this is stored rather than
     * the index of the page so we can re-order pages without affecting this value
     */
    public static String getReaderSubsPageTitle() {
        return getString(DeletablePrefKey.READER_SUBS_PAGE_TITLE);
    }

    public static void setReaderSubsPageTitle(String pageTitle) {
        setString(DeletablePrefKey.READER_SUBS_PAGE_TITLE, pageTitle);
    }

    public static CommentStatusCriteria getCommentsStatusFilter() {
        int idx = getInt(DeletablePrefKey.COMMENTS_STATUS_TYPE_INDEX);
        CommentStatusCriteria[] commentStatusValues = CommentStatusCriteria.values();
        if (commentStatusValues.length < idx) {
            return commentStatusValues[0];
        } else {
            return commentStatusValues[idx];
        }
    }

    public static void setCommentsStatusFilter(CommentStatusCriteria commentStatus) {
        if (commentStatus != null) {
            setInt(DeletablePrefKey.COMMENTS_STATUS_TYPE_INDEX, commentStatus.ordinal());
        } else {
            prefs().edit()
                   .remove(DeletablePrefKey.COMMENTS_STATUS_TYPE_INDEX.name())
                   .apply();
        }
    }

    public static PeopleListFilter getPeopleListFilter() {
        int idx = getInt(DeletablePrefKey.PEOPLE_LIST_FILTER_INDEX);
        PeopleListFilter[] values = PeopleListFilter.values();
        if (values.length < idx) {
            return values[0];
        } else {
            return values[idx];
        }
    }

    public static void setPeopleListFilter(PeopleListFilter peopleListFilter) {
        if (peopleListFilter != null) {
            setInt(DeletablePrefKey.PEOPLE_LIST_FILTER_INDEX, peopleListFilter.ordinal());
        } else {
            prefs().edit()
                   .remove(DeletablePrefKey.PEOPLE_LIST_FILTER_INDEX.name())
                   .apply();
        }
    }

    // Store the version code of the app. Used to check it the app was upgraded.
    public static int getLastAppVersionCode() {
        return getInt(UndeletablePrefKey.LAST_APP_VERSION_INDEX);
    }

    public static void setLastAppVersionCode(int versionCode) {
        setInt(UndeletablePrefKey.LAST_APP_VERSION_INDEX, versionCode);
    }

    public static long getLastUsedUserId() {
        return getLong(UndeletablePrefKey.LAST_USED_USER_ID);
    }

    public static void setLastUsedUserId(long userId) {
        setLong(UndeletablePrefKey.LAST_USED_USER_ID, userId);
    }

    public static boolean getLastReaderKnownAccessTokenStatus() {
        return getBoolean(UndeletablePrefKey.LAST_READER_KNOWN_ACCESS_TOKEN_STATUS, false);
    }

    public static void setLastReaderKnownAccessTokenStatus(boolean accessTokenStatus) {
        setBoolean(UndeletablePrefKey.LAST_READER_KNOWN_ACCESS_TOKEN_STATUS, accessTokenStatus);
    }

    public static long getLastReaderKnownUserId() {
        return getLong(UndeletablePrefKey.LAST_READER_KNOWN_USER_ID);
    }

    public static void setLastReaderKnownUserId(long userId) {
        setLong(UndeletablePrefKey.LAST_READER_KNOWN_USER_ID, userId);
    }

    /**
     * name of the last shown activity - used at startup to restore the previously selected
     * activity, also used by analytics tracker
     */
    public static String getLastActivityStr() {
        return getString(DeletablePrefKey.LAST_ACTIVITY_STR, ActivityId.UNKNOWN.name());
    }

    public static void setLastActivityStr(String value) {
        setString(DeletablePrefKey.LAST_ACTIVITY_STR, value);
    }

    public static void resetLastActivityStr() {
        remove(DeletablePrefKey.LAST_ACTIVITY_STR);
    }

    public static int getMainPageIndex(int maxIndexValue) {
        int value = getInt(DeletablePrefKey.MAIN_PAGE_INDEX);
        return value > maxIndexValue ? 0 : value;
    }

    public static void setMainPageIndex(int index) {
        setInt(DeletablePrefKey.MAIN_PAGE_INDEX, index);
    }

    // Stats Widgets
    public static void resetStatsWidgetsKeys() {
        remove(DeletablePrefKey.STATS_WIDGET_KEYS_BLOGS);
    }

    public static String getStatsWidgetsKeys() {
        return getString(DeletablePrefKey.STATS_WIDGET_KEYS_BLOGS);
    }

    public static void setStatsWidgetsKeys(String widgetData) {
        setString(DeletablePrefKey.STATS_WIDGET_KEYS_BLOGS, widgetData);
    }

    public static String getStatsWidgetsData() {
        return getString(DeletablePrefKey.STATS_WIDGET_DATA);
    }

    public static void setStatsWidgetsData(String widgetData) {
        setString(DeletablePrefKey.STATS_WIDGET_DATA, widgetData);
    }

    public static void resetStatsWidgetsData() {
        remove(DeletablePrefKey.STATS_WIDGET_DATA);
    }

    // Themes
    public static void setThemeImageSizeWidth(int width) {
        setInt(UndeletablePrefKey.THEME_IMAGE_SIZE_WIDTH, width);
    }

    public static int getThemeImageSizeWidth() {
        int value = getInt(UndeletablePrefKey.THEME_IMAGE_SIZE_WIDTH);
        if (value == 0) {
            return THEME_IMAGE_SIZE_WIDTH_DEFAULT;
        } else {
            return getInt(UndeletablePrefKey.THEME_IMAGE_SIZE_WIDTH);
        }
    }

    // Aztec Editor
    public static void setAztecEditorEnabled(boolean isEnabled) {
        setBoolean(UndeletablePrefKey.AZTEC_EDITOR_ENABLED, isEnabled);
        AnalyticsTracker.track(isEnabled ? Stat.EDITOR_AZTEC_TOGGLED_ON : Stat.EDITOR_AZTEC_TOGGLED_OFF);
    }

    public static boolean isAztecEditorEnabled() {
        // hardcode Aztec enabled to "true". It's Aztec and Gutenberg that we're going to expose to the user now.
        return true;
    }

    public static boolean isAztecEditorToolbarExpanded() {
        return getBoolean(UndeletablePrefKey.AZTEC_EDITOR_TOOLBAR_EXPANDED, false);
    }

    public static void setAztecEditorToolbarExpanded(boolean isExpanded) {
        setBoolean(UndeletablePrefKey.AZTEC_EDITOR_TOOLBAR_EXPANDED, isExpanded);
    }

    public static boolean shouldShowBookmarksSavedLocallyDialog() {
        return getBoolean(UndeletablePrefKey.BOOKMARKS_SAVED_LOCALLY_DIALOG_SHOWN, true);
    }

    public static void setBookmarksSavedLocallyDialogShown() {
        setBoolean(UndeletablePrefKey.BOOKMARKS_SAVED_LOCALLY_DIALOG_SHOWN, false);
    }

    public static boolean isImageOptimizePromoRequired() {
        return getBoolean(UndeletablePrefKey.IMAGE_OPTIMIZE_PROMO_REQUIRED, true);
    }

    public static void setImageOptimizePromoRequired(boolean required) {
        setBoolean(UndeletablePrefKey.IMAGE_OPTIMIZE_PROMO_REQUIRED, required);
    }

    // Store the number of times Stats are loaded successfully before showing the Promo Dialog
    public static void bumpAnalyticsForStatsWidgetPromo() {
        int current = getAnalyticsForStatsWidgetPromo();
        setInt(DeletablePrefKey.STATS_WIDGET_PROMO_ANALYTICS, current + 1);
    }

    public static int getAnalyticsForStatsWidgetPromo() {
        return getInt(DeletablePrefKey.STATS_WIDGET_PROMO_ANALYTICS);
    }

    public static boolean isInAppPurchaseRefreshRequired() {
        return getBoolean(UndeletablePrefKey.IAP_SYNC_REQUIRED, false);
    }

    public static void setInAppPurchaseRefreshRequired(boolean required) {
        setBoolean(UndeletablePrefKey.IAP_SYNC_REQUIRED, required);
    }

    public static int getSelectedSite() {
        return getInt(DeletablePrefKey.SELECTED_SITE_LOCAL_ID, -1);
    }

    public static void setSelectedSite(int selectedSite) {
        setInt(DeletablePrefKey.SELECTED_SITE_LOCAL_ID, selectedSite);
    }

    public static String getLastPushNotificationWpcomNoteId() {
        return getString(DeletablePrefKey.PUSH_NOTIFICATIONS_LAST_NOTE_ID);
    }

    public static void setLastPushNotificationWpcomNoteId(String noteID) {
        setString(DeletablePrefKey.PUSH_NOTIFICATIONS_LAST_NOTE_ID, noteID);
    }

    public static long getLastPushNotificationTime() {
        return getLong(DeletablePrefKey.PUSH_NOTIFICATIONS_LAST_NOTE_TIME);
    }

    public static void setLastPushNotificationTime(long time) {
        setLong(DeletablePrefKey.PUSH_NOTIFICATIONS_LAST_NOTE_ID, time);
    }

    public static boolean isNotificationsSwipeToNavigateShown() {
        return getBoolean(UndeletablePrefKey.SWIPE_TO_NAVIGATE_NOTIFICATIONS, false);
    }

    public static void setNotificationsSwipeToNavigateShown(boolean alreadyShown) {
        setBoolean(UndeletablePrefKey.SWIPE_TO_NAVIGATE_NOTIFICATIONS, alreadyShown);
    }

    public static boolean isReaderSwipeToNavigateShown() {
        return getBoolean(UndeletablePrefKey.SWIPE_TO_NAVIGATE_READER, false);
    }

    public static void setReaderSwipeToNavigateShown(boolean alreadyShown) {
        setBoolean(UndeletablePrefKey.SWIPE_TO_NAVIGATE_READER, alreadyShown);
    }

    public static long getPendingDraftsLastNotificationDate(PostModel post) {
        String key = DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_LAST_NOTIFICATION_DATES.name() + "-" + post.getId();
        return prefs().getLong(key, 0);
    }

    public static void setPendingDraftsLastNotificationDate(PostModel post, long timestamp) {
        String key = DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_LAST_NOTIFICATION_DATES.name() + "-" + post.getId();
        SharedPreferences.Editor editor = prefs().edit();
        editor.putLong(key, timestamp);
        editor.apply();
    }

    public static boolean isImageOptimize() {
        return getBoolean(DeletablePrefKey.IMAGE_OPTIMIZE_ENABLED, false);
    }

    public static void setImageOptimize(boolean optimize) {
        setBoolean(DeletablePrefKey.IMAGE_OPTIMIZE_ENABLED, optimize);
    }

    public static boolean isStripImageLocation() {
        return getBoolean(DeletablePrefKey.STRIP_IMAGE_LOCATION, false);
    }

    public static void setStripImageLocation(boolean stripImageLocation) {
        setBoolean(DeletablePrefKey.STRIP_IMAGE_LOCATION, stripImageLocation);
    }

    public static void setImageOptimizeMaxSize(int width) {
        setInt(DeletablePrefKey.IMAGE_OPTIMIZE_WIDTH, width);
    }

    public static int getImageOptimizeMaxSize() {
        int resizeWidth = getInt(DeletablePrefKey.IMAGE_OPTIMIZE_WIDTH, 0);
        return resizeWidth == 0 ? WPMediaUtils.OPTIMIZE_IMAGE_MAX_SIZE : resizeWidth;
    }

    public static void setImageOptimizeQuality(int quality) {
        setInt(DeletablePrefKey.IMAGE_OPTIMIZE_QUALITY, quality);
    }

    public static int getImageOptimizeQuality() {
        int quality = getInt(DeletablePrefKey.IMAGE_OPTIMIZE_QUALITY, 0);
        return quality > 1 ? quality : WPMediaUtils.OPTIMIZE_IMAGE_ENCODER_QUALITY;
    }

    public static boolean isVideoOptimize() {
        return getBoolean(DeletablePrefKey.VIDEO_OPTIMIZE_ENABLED, false);
    }

    public static void setVideoOptimize(boolean optimize) {
        setBoolean(DeletablePrefKey.VIDEO_OPTIMIZE_ENABLED, optimize);
    }

    public static boolean isGutenbergEditorEnabled() {
        // hardcode Gutenberg enabled to "true". It's Aztec and Gutenberg that we're going to expose to the user now.
        return true;
    }

    /**
     * @deprecated  As of release 13.0, replaced by SiteSettings mobile editor value
     */
    @Deprecated
    public static boolean isGutenbergDefaultForNewPosts() {
        return getBoolean(DeletablePrefKey.GUTENBERG_DEFAULT_FOR_NEW_POSTS, false);
    }

    public static boolean isDefaultAppWideEditorPreferenceSet() {
        // Check if the default editor pref was previously set
        return !"".equals(getString(DeletablePrefKey.GUTENBERG_DEFAULT_FOR_NEW_POSTS));
    }

    public static boolean isUserInGutenbergRolloutGroup() {
        return getBoolean(DeletablePrefKey.USER_IN_GUTENBERG_ROLLOUT_GROUP, false);
    }

    public static void setUserInGutenbergRolloutGroup() {
        setBoolean(DeletablePrefKey.USER_IN_GUTENBERG_ROLLOUT_GROUP, true);
    }

    public static void removeAppWideEditorPreference() {
        remove(DeletablePrefKey.GUTENBERG_DEFAULT_FOR_NEW_POSTS);
    }

    private static boolean getShowGutenbergInfoPopupForTheNewPosts(PrefKey key, String siteURL) {
        if (TextUtils.isEmpty(siteURL)) {
            return false;
        }

        Set<String> urls;
        try {
            urls = prefs().getStringSet(key.name(), null);
        } catch (ClassCastException exp) {
            // no operation - This should not happen.
            return false;
        }
        // Check if the current site address is available in the set.
        boolean flag = false;
        if (urls != null) {
            if (urls.contains(siteURL)) {
                flag = true;
                // remove the flag from Prefs
                setShowGutenbergInfoPopupForTheNewPosts(key, siteURL, false);
            }
        }

        return flag;
    }

    private static void setShowGutenbergInfoPopupForTheNewPosts(PrefKey key, String siteURL, boolean show) {
        if (TextUtils.isEmpty(siteURL)) {
            return;
        }
        Set<String> urls;
        try {
            urls = prefs().getStringSet(key.name(), null);
        } catch (ClassCastException exp) {
            // nope - this should never happens
            return;
        }

        Set<String> newUrls = new HashSet<>();
        // re-add the old urls here
        if (urls != null) {
            newUrls.addAll(urls);
        }

        // 1. First remove & 2. add if necessary
        newUrls.remove(siteURL);
        if (show) {
            newUrls.add(siteURL);
        }

        SharedPreferences.Editor editor = prefs().edit();
        editor.putStringSet(key.name(), newUrls);
        editor.apply();
    }

    public static boolean shouldShowGutenbergInfoPopupPhase2ForNewPosts(String siteURL) {
        return getShowGutenbergInfoPopupForTheNewPosts(
                DeletablePrefKey.SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS_PHASE_2, siteURL);
    }

    public static void setShowGutenbergInfoPopupPhase2ForNewPosts(String siteURL, boolean show) {
        setShowGutenbergInfoPopupForTheNewPosts(DeletablePrefKey.SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS_PHASE_2,
                siteURL, show);
    }

    public static boolean shouldShowGutenbergInfoPopupForTheNewPosts(String siteURL) {
        return getShowGutenbergInfoPopupForTheNewPosts(
                DeletablePrefKey.SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS, siteURL);
    }

    public static void setShowGutenbergInfoPopupForTheNewPosts(String siteURL, boolean show) {
        setShowGutenbergInfoPopupForTheNewPosts(DeletablePrefKey.SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS,
                siteURL, show);
    }

    public static boolean isGutenbergInfoPopupDisplayed(String siteURL) {
        if (TextUtils.isEmpty(siteURL)) {
            return false;
        }

        Set<String> urls;
        try {
            urls = prefs().getStringSet(DeletablePrefKey.GUTENBERG_OPT_IN_DIALOG_SHOWN.name(), null);
        } catch (ClassCastException exp) {
            // no operation - This should not happen.
            return false;
        }

        return urls != null && urls.contains(siteURL);
    }

    public static void setGutenbergInfoPopupDisplayed(String siteURL, boolean isDisplayed) {
        if (isGutenbergInfoPopupDisplayed(siteURL)) {
            return;
        }
        if (TextUtils.isEmpty(siteURL)) {
            return;
        }
        Set<String> urls;
        try {
            urls = prefs().getStringSet(DeletablePrefKey.GUTENBERG_OPT_IN_DIALOG_SHOWN.name(), null);
        } catch (ClassCastException exp) {
            // nope - this should never happens
            return;
        }

        Set<String> newUrls = new HashSet<>();
        // re-add the old urls here
        if (urls != null) {
            newUrls.addAll(urls);
        }
        if (isDisplayed) {
            newUrls.add(siteURL);
        } else {
            newUrls.remove(siteURL);
        }
        SharedPreferences.Editor editor = prefs().edit();
        editor.putStringSet(DeletablePrefKey.GUTENBERG_OPT_IN_DIALOG_SHOWN.name(), newUrls);
        editor.apply();
    }

    public static void setVideoOptimizeWidth(int width) {
        setInt(DeletablePrefKey.VIDEO_OPTIMIZE_WIDTH, width);
    }

    public static int getVideoOptimizeWidth() {
        int resizeWidth = getInt(DeletablePrefKey.VIDEO_OPTIMIZE_WIDTH, 0);
        return resizeWidth == 0 ? WPMediaUtils.OPTIMIZE_VIDEO_MAX_WIDTH : resizeWidth;
    }

    public static void setVideoOptimizeQuality(int quality) {
        setInt(DeletablePrefKey.VIDEO_OPTIMIZE_QUALITY, quality);
    }

    public static int getVideoOptimizeQuality() {
        int quality = getInt(DeletablePrefKey.VIDEO_OPTIMIZE_QUALITY, 0);
        return quality > 1 ? quality : WPMediaUtils.OPTIMIZE_VIDEO_ENCODER_BITRATE_KB;
    }

    public static void setSupportEmail(String email) {
        setString(DeletablePrefKey.SUPPORT_EMAIL, email);
    }

    public static String getSupportEmail() {
        return getString(DeletablePrefKey.SUPPORT_EMAIL);
    }

    public static void removeSupportEmail() {
        remove(DeletablePrefKey.SUPPORT_EMAIL);
    }

    public static void setSupportName(String name) {
        setString(DeletablePrefKey.SUPPORT_NAME, name);
    }

    public static String getSupportName() {
        return getString(DeletablePrefKey.SUPPORT_NAME);
    }

    public static void removeSupportName() {
        remove(DeletablePrefKey.SUPPORT_NAME);
    }

    /*
     * returns a list of local IDs of sites recently chosen in the site picker
     */
    public static ArrayList<Integer> getRecentlyPickedSiteIds() {
        return getRecentlyPickedSiteIds(MAX_RECENTLY_PICKED_SITES_TO_SHOW);
    }

    private static ArrayList<Integer> getRecentlyPickedSiteIds(int limit) {
        String idsAsString = getString(DeletablePrefKey.RECENTLY_PICKED_SITE_IDS, "");
        List<String> items = Arrays.asList(idsAsString.split(","));

        ArrayList<Integer> siteIds = new ArrayList<>();
        for (String item : items) {
            siteIds.add(StringUtils.stringToInt(item));
            if (siteIds.size() == limit) {
                break;
            }
        }

        return siteIds;
    }

    /*
     * adds a local site ID to the top of list of recently chosen sites
     */
    public static void addRecentlyPickedSiteId(Integer localId) {
        if (localId == 0) {
            return;
        }

        ArrayList<Integer> currentIds = getRecentlyPickedSiteIds(MAX_RECENTLY_PICKED_SITES_TO_SAVE);

        // remove this ID if it already exists in the list
        int index = currentIds.indexOf(localId);
        if (index > -1) {
            currentIds.remove(index);
        }

        // add this ID to the front of the list
        currentIds.add(0, localId);

        // remove at max
        if (currentIds.size() > MAX_RECENTLY_PICKED_SITES_TO_SAVE) {
            currentIds.remove(MAX_RECENTLY_PICKED_SITES_TO_SAVE);
        }

        // store in prefs
        String idsAsString = TextUtils.join(",", currentIds);
        setString(DeletablePrefKey.RECENTLY_PICKED_SITE_IDS, idsAsString);
    }

    public static void removeRecentlyPickedSiteId(Integer localId) {
        ArrayList<Integer> currentIds = getRecentlyPickedSiteIds(MAX_RECENTLY_PICKED_SITES_TO_SAVE);

        int index = currentIds.indexOf(localId);
        if (index > -1) {
            currentIds.remove(index);
            String idsAsString = TextUtils.join(",", currentIds);
            setString(DeletablePrefKey.RECENTLY_PICKED_SITE_IDS, idsAsString);
        }
    }

    public static long getLastWpComThemeSync() {
        return getLong(UndeletablePrefKey.LAST_WP_COM_THEMES_SYNC);
    }

    public static void setLastWpComThemeSync(long time) {
        setLong(UndeletablePrefKey.LAST_WP_COM_THEMES_SYNC, time);
    }

    public static void setShouldTrackMagicLinkSignup(Boolean shouldTrack) {
        setBoolean(DeletablePrefKey.SHOULD_TRACK_MAGIC_LINK_SIGNUP, shouldTrack);
    }

    public static boolean getShouldTrackMagicLinkSignup() {
        return getBoolean(DeletablePrefKey.SHOULD_TRACK_MAGIC_LINK_SIGNUP, false);
    }

    public static void removeShouldTrackMagicLinkSignup() {
        remove(DeletablePrefKey.SHOULD_TRACK_MAGIC_LINK_SIGNUP);
    }

    public static void setNewsCardDismissedVersion(int version) {
        setInt(DeletablePrefKey.NEWS_CARD_DISMISSED_VERSION, version);
    }

    public static int getNewsCardDismissedVersion() {
        return getInt(DeletablePrefKey.NEWS_CARD_DISMISSED_VERSION, -1);
    }

    public static void setNewsCardShownVersion(int version) {
        setInt(DeletablePrefKey.NEWS_CARD_SHOWN_VERSION, version);
    }

    public static int getNewsCardShownVersion() {
        return getInt(DeletablePrefKey.NEWS_CARD_SHOWN_VERSION, -1);
    }

    public static void setQuickStartDisabled(Boolean isDisabled) {
        setBoolean(UndeletablePrefKey.IS_QUICK_START_DISABLED, isDisabled);
    }

    public static boolean isQuickStartDisabled() {
        return getBoolean(UndeletablePrefKey.IS_QUICK_START_DISABLED, false);
    }

    public static void setMainFabTooltipDisabled(Boolean disable) {
        setBoolean(UndeletablePrefKey.IS_MAIN_FAB_TOOLTIP_DISABLED, disable);
    }

    public static boolean isMainFabTooltipDisabled() {
        return getBoolean(UndeletablePrefKey.IS_MAIN_FAB_TOOLTIP_DISABLED, false);
    }

    public static void setQuickStartMigrationDialogShown(Boolean shown) {
        setBoolean(UndeletablePrefKey.HAS_QUICK_START_MIGRATION_SHOWN, shown);
    }

    public static boolean hasQuickStartMigrationDialogShown() {
        return getBoolean(UndeletablePrefKey.HAS_QUICK_START_MIGRATION_SHOWN, false);
    }

    public static void setQuickStartNoticeRequired(Boolean shown) {
        setBoolean(DeletablePrefKey.IS_QUICK_START_NOTICE_REQUIRED, shown);
    }

    public static boolean isQuickStartNoticeRequired() {
        return getBoolean(DeletablePrefKey.IS_QUICK_START_NOTICE_REQUIRED, false);
    }

    public static void setInstallationReferrerObtained(Boolean isObtained) {
        setBoolean(UndeletablePrefKey.IS_INSTALLATION_REFERRER_OBTAINED, isObtained);
    }

    public static boolean isInstallationReferrerObtained() {
        return getBoolean(UndeletablePrefKey.IS_INSTALLATION_REFERRER_OBTAINED, false);
    }

    public static int getAvatarVersion() {
        return getInt(DeletablePrefKey.AVATAR_VERSION, 0);
    }

    public static void setAvatarVersion(int version) {
        setInt(DeletablePrefKey.AVATAR_VERSION, version);
    }

    @NonNull public static AuthorFilterSelection getAuthorFilterSelection() {
        long id = getLong(DeletablePrefKey.POST_LIST_AUTHOR_FILTER, AuthorFilterSelection.getDefaultValue().getId());
        return AuthorFilterSelection.fromId(id);
    }

    public static void setAuthorFilterSelection(@NonNull AuthorFilterSelection selection) {
        setLong(DeletablePrefKey.POST_LIST_AUTHOR_FILTER, selection.getId());
    }

    @NonNull public static PostListViewLayoutType getPostsListViewLayoutType() {
        long id = getLong(DeletablePrefKey.POST_LIST_VIEW_LAYOUT_TYPE,
                PostListViewLayoutType.getDefaultValue().getId());
        return PostListViewLayoutType.fromId(id);
    }

    public static void setPostsListViewLayoutType(@NonNull PostListViewLayoutType type) {
        setLong(DeletablePrefKey.POST_LIST_VIEW_LAYOUT_TYPE, type.getId());
    }

    public static void setStatsWidgetSelectedSiteId(long siteId, int appWidgetId) {
        prefs().edit().putLong(getSiteIdWidgetKey(appWidgetId), siteId).apply();
    }

    public static long getStatsWidgetSelectedSiteId(int appWidgetId) {
        return prefs().getLong(getSiteIdWidgetKey(appWidgetId), -1);
    }

    public static void removeStatsWidgetSelectedSiteId(int appWidgetId) {
        prefs().edit().remove(getSiteIdWidgetKey(appWidgetId)).apply();
    }

    @NonNull private static String getSiteIdWidgetKey(int appWidgetId) {
        return DeletablePrefKey.STATS_WIDGET_SELECTED_SITE_ID.name() + appWidgetId;
    }

    public static void setStatsWidgetColorModeId(int colorModeId, int appWidgetId) {
        prefs().edit().putInt(getColorModeIdWidgetKey(appWidgetId), colorModeId).apply();
    }

    public static int getStatsWidgetColorModeId(int appWidgetId) {
        return prefs().getInt(getColorModeIdWidgetKey(appWidgetId), -1);
    }

    public static void removeStatsWidgetColorModeId(int appWidgetId) {
        prefs().edit().remove(getColorModeIdWidgetKey(appWidgetId)).apply();
    }

    @NonNull private static String getColorModeIdWidgetKey(int appWidgetId) {
        return DeletablePrefKey.STATS_WIDGET_COLOR_MODE.name() + appWidgetId;
    }

    public static void setStatsWidgetDataTypeId(int dataTypeId, int appWidgetId) {
        prefs().edit().putInt(getDataTypeIdWidgetKey(appWidgetId), dataTypeId).apply();
    }

    public static int getStatsWidgetDataTypeId(int appWidgetId) {
        return prefs().getInt(getDataTypeIdWidgetKey(appWidgetId), -1);
    }

    public static void removeStatsWidgetDataTypeId(int appWidgetId) {
        prefs().edit().remove(getDataTypeIdWidgetKey(appWidgetId)).apply();
    }

    @NonNull private static String getDataTypeIdWidgetKey(int appWidgetId) {
        return DeletablePrefKey.STATS_WIDGET_DATA_TYPE.name() + appWidgetId;
    }

    public static void setStatsWidgetHasData(boolean hasData, int appWidgetId) {
        prefs().edit().putBoolean(getHasDataWidgetKey(appWidgetId), hasData).apply();
    }

    public static boolean getStatsWidgetHasData(int appWidgetId) {
        return prefs().getBoolean(getHasDataWidgetKey(appWidgetId), false);
    }

    public static void removeStatsWidgetHasData(int appWidgetId) {
        prefs().edit().remove(getHasDataWidgetKey(appWidgetId)).apply();
    }

    @NonNull private static String getHasDataWidgetKey(int appWidgetId) {
        return DeletablePrefKey.STATS_WIDGET_HAS_DATA.name() + appWidgetId;
    }

    public static void setSystemNotificationsEnabled(boolean enabled) {
        setBoolean(UndeletablePrefKey.SYSTEM_NOTIFICATIONS_ENABLED, enabled);
    }

    public static boolean getSystemNotificationsEnabled() {
        return getBoolean(UndeletablePrefKey.SYSTEM_NOTIFICATIONS_ENABLED, true);
    }

    public static void setShouldShowPostSignupInterstitial(boolean shouldShow) {
        setBoolean(UndeletablePrefKey.SHOULD_SHOW_POST_SIGNUP_INTERSTITIAL, shouldShow);
    }

    public static boolean shouldShowPostSignupInterstitial() {
        return getBoolean(UndeletablePrefKey.SHOULD_SHOW_POST_SIGNUP_INTERSTITIAL, true);
    }

    private static List<String> getPostWithHWAccelerationOff() {
        String idsAsString = getString(DeletablePrefKey.AZTEC_EDITOR_DISABLE_HW_ACC_KEYS, "");
        return Arrays.asList(idsAsString.split(","));
    }

    public static void setFeatureAnnouncementShownVersion(int version) {
        setInt(UndeletablePrefKey.FEATURE_ANNOUNCEMENT_SHOWN_VERSION, version);
    }

    public static int getFeatureAnnouncementShownVersion() {
        return getInt(UndeletablePrefKey.FEATURE_ANNOUNCEMENT_SHOWN_VERSION, -1);
    }

    public static int getLastFeatureAnnouncementAppVersionCode() {
        return getInt(UndeletablePrefKey.LAST_FEATURE_ANNOUNCEMENT_APP_VERSION_CODE);
    }

    public static void setLastFeatureAnnouncementAppVersionCode(int version) {
        setInt(UndeletablePrefKey.LAST_FEATURE_ANNOUNCEMENT_APP_VERSION_CODE, version);
    }

    /*
     * adds a local site ID to the top of list of recently chosen sites
     */
    public static void addPostWithHWAccelerationOff(int localSiteId, int localPostId) {
        if (localSiteId == 0 || localPostId == 0 || isPostWithHWAccelerationOff(localSiteId, localPostId)) {
            return;
        }
        String key = localSiteId + "-" + localPostId;
        List<String> currentIds = new ArrayList<>(getPostWithHWAccelerationOff());
        currentIds.add(key);
        // store in prefs
        String idsAsString = TextUtils.join(",", currentIds);
        setString(DeletablePrefKey.AZTEC_EDITOR_DISABLE_HW_ACC_KEYS, idsAsString);
    }

    public static boolean isPostWithHWAccelerationOff(int localSiteId, int localPostId) {
        if (localSiteId == 0 || localPostId == 0) {
            return false;
        }
        List<String> currentIds = getPostWithHWAccelerationOff();
        String key = localSiteId + "-" + localPostId;
        if (currentIds.contains(key)) {
            return true;
        }
        return false;
    }
}
