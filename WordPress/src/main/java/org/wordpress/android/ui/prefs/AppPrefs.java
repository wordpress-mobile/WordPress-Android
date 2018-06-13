package org.wordpress.android.ui.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.models.PeopleListFilter;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.stats.StatsTimeframe;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPMediaUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        // Used to flag the account created stat needs to be bumped after account information is synced.
        SHOULD_TRACK_MAGIC_LINK_SIGNUP,
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

        // visual editor available
        VISUAL_EDITOR_AVAILABLE,

        // visual editor enabled
        VISUAL_EDITOR_ENABLED,

        // aztec editor enabled
        AZTEC_EDITOR_ENABLED,

        // aztec editor toolbar expanded state
        AZTEC_EDITOR_TOOLBAR_EXPANDED,

        // When we need to show the async promo dialog
        ASYNC_PROMO_REQUIRED,

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
        ASKED_PERMISSION_LOCATION_COURSE,
        ASKED_PERMISSION_LOCATION_FINE,

        // Updated after WP.com themes have been fetched
        LAST_WP_COM_THEMES_SYNC,

        // user id last used to login with
        LAST_USED_USER_ID
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
        try {
            String value = getString(key);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
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
        return ReaderUtils.getTagFromTagName(tagName, ReaderTagType.fromInt(tagType));
    }

    public static void setReaderTag(ReaderTag tag) {
        if (tag != null && !TextUtils.isEmpty(tag.getTagSlug())) {
            setString(DeletablePrefKey.READER_TAG_NAME, tag.getTagSlug());
            setInt(DeletablePrefKey.READER_TAG_TYPE, tag.tagType.toInt());
        } else {
            prefs().edit()
                   .remove(DeletablePrefKey.READER_TAG_NAME.name())
                   .remove(DeletablePrefKey.READER_TAG_TYPE.name())
                   .apply();
        }
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

    public static StatsTimeframe getStatsTimeframe() {
        int idx = getInt(DeletablePrefKey.STATS_ITEM_INDEX);
        StatsTimeframe[] timeframeValues = StatsTimeframe.values();
        if (timeframeValues.length < idx) {
            return timeframeValues[0];
        } else {
            return timeframeValues[idx];
        }
    }

    public static void setStatsTimeframe(StatsTimeframe timeframe) {
        if (timeframe != null) {
            setInt(DeletablePrefKey.STATS_ITEM_INDEX, timeframe.ordinal());
        } else {
            prefs().edit()
                   .remove(DeletablePrefKey.STATS_ITEM_INDEX.name())
                   .apply();
        }
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

    public static int getMainPageIndex() {
        return getInt(DeletablePrefKey.MAIN_PAGE_INDEX);
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
        return getBoolean(UndeletablePrefKey.AZTEC_EDITOR_ENABLED, false);
    }

    public static boolean isAztecEditorToolbarExpanded() {
        return getBoolean(UndeletablePrefKey.AZTEC_EDITOR_TOOLBAR_EXPANDED, false);
    }

    public static void setAztecEditorToolbarExpanded(boolean isExpanded) {
        setBoolean(UndeletablePrefKey.AZTEC_EDITOR_TOOLBAR_EXPANDED, isExpanded);
    }

    // Visual Editor
    public static void setVisualEditorEnabled(boolean visualEditorEnabled) {
        setBoolean(UndeletablePrefKey.VISUAL_EDITOR_ENABLED, visualEditorEnabled);
        AnalyticsTracker.track(visualEditorEnabled ? Stat.EDITOR_HYBRID_TOGGLED_ON : Stat.EDITOR_HYBRID_TOGGLED_OFF);
    }

    public static void setVisualEditorAvailable(boolean visualEditorAvailable) {
        setBoolean(UndeletablePrefKey.VISUAL_EDITOR_AVAILABLE, visualEditorAvailable);
        if (visualEditorAvailable) {
            AnalyticsTracker.track(Stat.EDITOR_HYBRID_ENABLED);
        }
    }

    public static boolean isVisualEditorAvailable() {
        return getBoolean(UndeletablePrefKey.VISUAL_EDITOR_AVAILABLE, true);
    }

    public static boolean isVisualEditorEnabled() {
        return isVisualEditorAvailable() && getBoolean(UndeletablePrefKey.VISUAL_EDITOR_ENABLED,
                                                       !isAztecEditorEnabled());
    }

    public static boolean isAsyncPromoRequired() {
        return getBoolean(UndeletablePrefKey.ASYNC_PROMO_REQUIRED, true);
    }

    public static void setAsyncPromoRequired(boolean required) {
        setBoolean(UndeletablePrefKey.ASYNC_PROMO_REQUIRED, required);
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

    public static void setGlobalPlansFeatures(String jsonOfFeatures) {
        if (jsonOfFeatures != null) {
            setString(UndeletablePrefKey.GLOBAL_PLANS_PLANS_FEATURES, jsonOfFeatures);
        } else {
            remove(UndeletablePrefKey.GLOBAL_PLANS_PLANS_FEATURES);
        }
    }

    public static String getGlobalPlansFeatures() {
        return getString(UndeletablePrefKey.GLOBAL_PLANS_PLANS_FEATURES, "");
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
}
