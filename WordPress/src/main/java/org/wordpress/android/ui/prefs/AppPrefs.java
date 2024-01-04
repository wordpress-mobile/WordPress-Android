package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.model.JetpackCapability;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.models.PeopleListFilter;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase;
import org.wordpress.android.ui.mysite.SelectedSiteRepository;
import org.wordpress.android.ui.mysite.tabs.MySiteTabType;
import org.wordpress.android.ui.posts.AuthorFilterSelection;
import org.wordpress.android.ui.quickstart.QuickStartType;
import org.wordpress.android.ui.quickstart.QuickStartType.NewSiteQuickStartType;
import org.wordpress.android.ui.reader.tracker.ReaderTab;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.usecase.social.JetpackSocialFlow;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPMediaUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppPrefs {
    public static final int SELECTED_SITE_UNAVAILABLE = -1;

    private static final int THEME_IMAGE_SIZE_WIDTH_DEFAULT = 400;

    // store twice as many recent sites as we show
    private static final int MAX_RECENTLY_PICKED_SITES_TO_SHOW = 5;
    private static final int MAX_RECENTLY_PICKED_SITES_TO_SAVE = MAX_RECENTLY_PICKED_SITES_TO_SHOW * 2;

    private static final Gson GSON = new Gson();

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

        READER_TAGS_UPDATE_TIMESTAMP,
        // last selected tag in the reader
        READER_TAG_NAME,
        READER_TAG_TYPE,
        READER_TAG_WAS_FOLLOWING,

        // currently active tab on the main Reader screen when the user is in Reader
        READER_ACTIVE_TAB,

        // last selected subfilter in the reader
        READER_SUBFILTER,

        // title of the last active page in ReaderSubsActivity
        READER_SUBS_PAGE_TITLE,

        // index of the last active page in main activity
        MAIN_PAGE_INDEX,

        // index of the last active item in Stats activity
        STATS_ITEM_INDEX,

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

        AVATAR_VERSION,
        GUTENBERG_DEFAULT_FOR_NEW_POSTS,
        USER_IN_GUTENBERG_ROLLOUT_GROUP,
        SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS,
        SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS_PHASE_2,
        GUTENBERG_OPT_IN_DIALOG_SHOWN,
        GUTENBERG_FOCAL_POINT_PICKER_TOOLTIP_SHOWN,

        IS_QUICK_START_NOTICE_REQUIRED,
        LAST_SKIPPED_QUICK_START_TASK,
        LAST_SELECTED_QUICK_START_TYPE,

        POST_LIST_AUTHOR_FILTER,
        POST_LIST_VIEW_LAYOUT_TYPE,

        // Widget settings
        STATS_WIDGET_SELECTED_SITE_ID,
        STATS_WIDGET_COLOR_MODE,
        STATS_WIDGET_DATA_TYPE,
        STATS_WIDGET_HAS_DATA,

        // Keep the local_blog_id + local_post_id values that have HW Acc. turned off
        AZTEC_EDITOR_DISABLE_HW_ACC_KEYS,

        // timestamp of the last update of the reader css styles
        READER_CSS_UPDATED_TIMESTAMP,
        // Identifier of the next page for the discover /cards endpoint
        READER_CARDS_ENDPOINT_PAGE_HANDLE,
        // used to tell the server to return a different set of data so the content on discover tab doesn't look static
        READER_CARDS_ENDPOINT_REFRESH_COUNTER,

        // Used to delete recommended tags saved as followed tags in tbl_tags
        // Need to be done just once for a logged out user
        READER_RECOMMENDED_TAGS_DELETED_FOR_LOGGED_OUT_USER,
        MANUAL_FEATURE_CONFIG,
        SITE_JETPACK_CAPABILITIES,
        REMOVED_QUICK_START_CARD_TYPE,
        PINNED_DYNAMIC_CARD,
        // PUBLISHED_POST_COUNT will increase until it reaches ReviewViewModel.TARGET_COUNT_POST_PUBLISHED
        PUBLISHED_POST_COUNT,
        IN_APP_REVIEW_SHOWN,
        BLOGGING_REMINDERS_SHOWN,
        SHOULD_SCHEDULE_CREATE_SITE_NOTIFICATION,
        SHOULD_SHOW_WEEKLY_ROUNDUP_NOTIFICATION,

        SKIPPED_BLOGGING_PROMPT_DAY,
        OPEN_WEB_LINKS_WITH_JETPACK_OVERLAY_LAST_SHOWN_TIMESTAMP,
        OPEN_WEB_LINKS_WITH_JETPACK,
        SHOULD_HIDE_JETPACK_FEATURE_CARD,
        JETPACK_FEATURE_CARD_LAST_SHOWN_TIMESTAMP,
        SWITCH_TO_JETPACK_MENU_CARD_SHOWN_TIMESTAMP,
        SHOULD_HIDE_SWITCH_TO_JETPACK_MENU_CARD,
        SHOULD_HIDE_JETPACK_INSTALL_FULL_PLUGIN_CARD,
        SHOULD_SHOW_JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING,
        SHOULD_HIDE_PROMOTE_WITH_BLAZE_CARD,
        SHOULD_HIDE_DASHBOARD_PLANS_CARD,

        // Jetpack Individual Plugin overlay for WordPress app
        WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_SHOWN_COUNT,
        WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_LAST_SHOWN_TIMESTAMP,
        NOTIFICATIONS_PERMISSION_WARNING_DISMISSED,
        HAS_SAVED_PRIVACY_SETTINGS,
        SHOULD_HIDE_BLAZE_OVERLAY,

        // Should show Jetpack Social no connections UI
        SHOULD_SHOW_JETPACK_SOCIAL_NO_CONNECTIONS,
        SHOULD_HIDE_ACTIVITY_DASHBOARD_CARD,
        SHOULD_HIDE_PAGES_DASHBOARD_CARD,
        SHOULD_HIDE_TODAY_STATS_DASHBOARD_CARD,
        SHOULD_HIDE_POST_DASHBOARD_CARD,
        SHOULD_HIDE_NEXT_STEPS_DASHBOARD_CARD,
        SHOULD_HIDE_GET_TO_KNOW_THE_APP_DASHBOARD_CARD,

        SHOULD_SHOW_SITE_ITEM_AS_QUICK_LINK_IN_DASHBOARD,
        SHOULD_SHOW_DEFAULT_QUICK_LINK_IN_DASHBOARD,
        SHOULD_HIDE_BLOGANUARY_NUDGE_CARD,
        SHOULD_HIDE_SOTW2023_NUDGE_CARD,
        SHOULD_HIDE_DYNAMIC_CARD,
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
        ASKED_PERMISSION_IMAGES_READ,
        ASKED_PERMISSION_VIDEO_READ,
        ASKED_PERMISSION_AUDIO_READ,
        ASKED_PERMISSION_CAMERA,

        ASKED_PERMISSION_NOTIFICATIONS,

        // Updated after WP.com themes have been fetched
        LAST_WP_COM_THEMES_SYNC,

        // user id last used to login with
        LAST_USED_USER_ID,

        // last user access status in reader
        LAST_READER_KNOWN_ACCESS_TOKEN_STATUS,
        LAST_READER_KNOWN_USER_ID,

        // used to indicate that we already obtained and tracked the installation referrer
        IS_INSTALLATION_REFERRER_OBTAINED,

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

        // Used to indicate whether or not the stories intro screen must be shown
        SHOULD_SHOW_STORIES_INTRO,

        // Used to indicate whether or not the device running out of storage warning should be shown
        SHOULD_SHOW_STORAGE_WARNING,

        // Used to indicate whether or not bookmarked posts pseudo id should be updated after invalid pseudo id fix
        // (Internal Ref:p3hLNG-18u)
        SHOULD_UPDATE_BOOKMARKED_POSTS_PSEUDO_ID,

        // Tracks which block types are considered "new" via impression counts
        GUTENBERG_BLOCK_TYPE_IMPRESSIONS,

        // Used to identify the App Settings for initial screen that is updated when the variant is assigned
        wp_pref_initial_screen,

        // Indicates if this is the first time the user sees the blogging prompts onboarding dialog
        IS_FIRST_TIME_BLOGGING_PROMPTS_ONBOARDING,

        // Indicates if this is the first time we try to login to Jetpack automatically
        IS_FIRST_TRY_LOGIN_JETPACK,

        // Indicates if this is the first time we try to get the user flags in Jetpack automatically
        IS_FIRST_TRY_USER_FLAGS_JETPACK,

        // Indicates if this is the first time we try sync the blogging reminders in Jetpack automatically
        IS_FIRST_TRY_BLOGGING_REMINDERS_SYNC_JETPACK,

        // Indicates if this is the first time we try to get the reader saved posts in Jetpack automatically
        IS_FIRST_TRY_READER_SAVED_POSTS_JETPACK,

        // Indicates if the user has completed the Jetpack migration flow
        IS_JETPACK_MIGRATION_COMPLETED,

        // Indicates if the the Jetpack migration flow is in progress (useful for resetting after interruptions)
        IS_JETPACK_MIGRATION_IS_IN_PROGRESS,

        // Indicates if the user is eligible for the Jetpack migration flow
        IS_JETPACK_MIGRATION_ELIGIBLE,
    }

    static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
    }

    static Map<String, ?> getAllPrefs() {
        return prefs().getAll();
    }

    private static String getString(PrefKey key) {
        return getString(key, "");
    }

    private static String getString(PrefKey key, String defaultValue) {
        return prefs().getString(key.name(), defaultValue);
    }

    public static void setString(PrefKey key, String value) {
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

    public static void putLong(final PrefKey key, final long value) {
        prefs().edit().putLong(key.name(), value).apply();
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

    public static void putInt(final PrefKey key, final int value) {
        prefs().edit().putInt(key.name(), value).apply();
    }

    public static void setInt(PrefKey key, int value) {
        setString(key, Integer.toString(value));
    }

    public static boolean getBoolean(PrefKey key, boolean def) {
        String value = getString(key, Boolean.toString(def));
        return Boolean.parseBoolean(value);
    }

    public static void putBoolean(final PrefKey key, final boolean value) {
        prefs().edit().putBoolean(key.name(), value).apply();
    }

    public static void setBoolean(PrefKey key, boolean value) {
        setString(key, Boolean.toString(value));
    }

    public static void putStringSet(final PrefKey key, final Set<String> value) {
        prefs().edit().putStringSet(key.name(), value).apply();
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

    public static void setReaderActiveTab(ReaderTab readerTab) {
        setInt(DeletablePrefKey.READER_ACTIVE_TAB, readerTab != null ? readerTab.getId() : 0);
    }

    public static ReaderTab getReaderActiveTab() {
        int lastTabId = getInt(DeletablePrefKey.READER_ACTIVE_TAB);
        return lastTabId != 0 ? ReaderTab.Companion.fromId(lastTabId) : null;
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

    public static int getMainPageIndex(int maxIndexValue) {
        int value = getInt(DeletablePrefKey.MAIN_PAGE_INDEX);
        return value > maxIndexValue ? 0 : value;
    }

    public static void setMainPageIndex(int index) {
        setInt(DeletablePrefKey.MAIN_PAGE_INDEX, index);
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

    /**
     * This method should only be used by specific client classes that need access to the persisted selected site
     * instance due to the fact that the in-memory selected site instance might not be yet available.
     * <p>
     * The source of truth should always be the {@link SelectedSiteRepository} in-memory mechanism and as such access
     * to this method is limited to this class.
     */
    public static int getSelectedSite() {
        return getInt(DeletablePrefKey.SELECTED_SITE_LOCAL_ID, SELECTED_SITE_UNAVAILABLE);
    }

    /**
     * This method should only be used by specific client classes that need to update the persisted selected site
     * instance due to the fact that the in-memory selected site instance is updated as well.
     * <p>
     * The source of truth should always be the {@link SelectedSiteRepository} in-memory mechanism and as such the
     * update method should be limited to this class.
     */
    public static void setSelectedSite(int siteLocalId) {
        setInt(DeletablePrefKey.SELECTED_SITE_LOCAL_ID, siteLocalId);
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

    public static boolean isImageOptimize() {
        return getBoolean(DeletablePrefKey.IMAGE_OPTIMIZE_ENABLED, true);
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
        int defaultQuality = WPMediaUtils.OPTIMIZE_IMAGE_ENCODER_QUALITY;

        // It's necessary to check that the quality int exists in the quality array in case of changes
        // See #19644 for an example of when the array's values were changed
        Context context = WordPress.getContext();
        String[] validQualityValues = context.getResources().getStringArray(R.array.site_settings_image_quality_values);
        boolean isQualityValid = Arrays.asList(validQualityValues).contains(String.valueOf(quality));

        // If quality int does not exist in settings array, return the default quality value instead
        if (!isQualityValid) {
            setImageOptimizeQuality(defaultQuality);
            return defaultQuality;
        }

        return quality;
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
     * @deprecated As of release 13.0, replaced by SiteSettings mobile editor value
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

    public static void setGutenbergFocalPointPickerTooltipShown(boolean tooltipShown) {
        setBoolean(DeletablePrefKey.GUTENBERG_FOCAL_POINT_PICKER_TOOLTIP_SHOWN, tooltipShown);
    }

    public static boolean getGutenbergFocalPointPickerTooltipShown() {
        return getBoolean(DeletablePrefKey.GUTENBERG_FOCAL_POINT_PICKER_TOOLTIP_SHOWN, false);
    }

    public static void setGutenbergBlockTypeImpressions(Map<String, Double> newImpressions) {
        String json = GSON.toJson(newImpressions);
        setString(UndeletablePrefKey.GUTENBERG_BLOCK_TYPE_IMPRESSIONS, json);
    }

    public static Map<String, Double> getGutenbergBlockTypeImpressions() {
        String jsonString = getString(UndeletablePrefKey.GUTENBERG_BLOCK_TYPE_IMPRESSIONS, "[]");
        Map<String, Double> impressions = GSON.fromJson(jsonString, Map.class);
        return impressions;
    }

    /*
     * returns a list of local IDs of sites recently chosen in the site picker
     */
    public static ArrayList<Integer> getRecentlyPickedSiteIds() {
        return getRecentlyPickedSiteIds(MAX_RECENTLY_PICKED_SITES_TO_SHOW);
    }

    private static ArrayList<Integer> getRecentlyPickedSiteIds(int limit) {
        String idsAsString = getString(DeletablePrefKey.RECENTLY_PICKED_SITE_IDS, "");
        String[] items = idsAsString.split(",");

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

    public static void setMainFabTooltipDisabled(Boolean disable) {
        setBoolean(UndeletablePrefKey.IS_MAIN_FAB_TOOLTIP_DISABLED, disable);
    }

    public static boolean isMainFabTooltipDisabled() {
        return getBoolean(UndeletablePrefKey.IS_MAIN_FAB_TOOLTIP_DISABLED, false);
    }

    public static void setPostListFabTooltipDisabled(Boolean disable) {
        setBoolean(UndeletablePrefKey.IS_MAIN_FAB_TOOLTIP_DISABLED, disable);
    }

    public static boolean isPostListFabTooltipDisabled() {
        return getBoolean(UndeletablePrefKey.IS_MAIN_FAB_TOOLTIP_DISABLED, false);
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

    public static long getReaderTagsUpdatedTimestamp() {
        return getLong(DeletablePrefKey.READER_TAGS_UPDATE_TIMESTAMP, -1);
    }

    public static void setReaderTagsUpdatedTimestamp(long timestamp) {
        setLong(DeletablePrefKey.READER_TAGS_UPDATE_TIMESTAMP, timestamp);
    }

    public static long getReaderCssUpdatedTimestamp() {
        return getLong(DeletablePrefKey.READER_CSS_UPDATED_TIMESTAMP, 0);
    }

    public static void setReaderCssUpdatedTimestamp(long timestamp) {
        setLong(DeletablePrefKey.READER_CSS_UPDATED_TIMESTAMP, timestamp);
    }

    public static String getReaderCardsPageHandle() {
        return getString(DeletablePrefKey.READER_CARDS_ENDPOINT_PAGE_HANDLE, null);
    }

    public static void setReaderCardsPageHandle(String pageHandle) {
        setString(DeletablePrefKey.READER_CARDS_ENDPOINT_PAGE_HANDLE, pageHandle);
    }

    public static int getReaderCardsRefreshCounter() {
        return getInt(DeletablePrefKey.READER_CARDS_ENDPOINT_REFRESH_COUNTER, 0);
    }

    public static void incrementReaderCardsRefreshCounter() {
        setInt(DeletablePrefKey.READER_CARDS_ENDPOINT_REFRESH_COUNTER, getReaderCardsRefreshCounter() + 1);
    }

    public static boolean getReaderRecommendedTagsDeletedForLoggedOutUser() {
        return getBoolean(DeletablePrefKey.READER_RECOMMENDED_TAGS_DELETED_FOR_LOGGED_OUT_USER, false);
    }

    public static void setReaderRecommendedTagsDeletedForLoggedOutUser(boolean deleted) {
        setBoolean(DeletablePrefKey.READER_RECOMMENDED_TAGS_DELETED_FOR_LOGGED_OUT_USER, deleted);
    }

    public static void setShouldShowStoriesIntro(boolean shouldShow) {
        setBoolean(UndeletablePrefKey.SHOULD_SHOW_STORIES_INTRO, shouldShow);
    }

    public static boolean shouldShowStoriesIntro() {
        return getBoolean(UndeletablePrefKey.SHOULD_SHOW_STORIES_INTRO, true);
    }

    public static void setShouldShowStorageWarning(boolean shouldShow) {
        setBoolean(UndeletablePrefKey.SHOULD_SHOW_STORAGE_WARNING, shouldShow);
    }

    public static boolean shouldShowStorageWarning() {
        return getBoolean(UndeletablePrefKey.SHOULD_SHOW_STORAGE_WARNING, true);
    }

    public static void setBookmarkPostsPseudoIdsUpdated() {
        setBoolean(UndeletablePrefKey.SHOULD_UPDATE_BOOKMARKED_POSTS_PSEUDO_ID, false);
    }

    public static boolean shouldUpdateBookmarkPostsPseudoIds(ReaderTag tag) {
        return tag != null
               && tag.getTagSlug().equals(ReaderUtils.sanitizeWithDashes(ReaderTag.TAG_TITLE_FOLLOWED_SITES))
               && getBoolean(UndeletablePrefKey.SHOULD_UPDATE_BOOKMARKED_POSTS_PSEUDO_ID, true);
    }

    public static QuickStartTask getLastSkippedQuickStartTask(QuickStartType quickStartType) {
        String taskName = getString(DeletablePrefKey.LAST_SKIPPED_QUICK_START_TASK);
        if (TextUtils.isEmpty(taskName)) {
            return null;
        }
        return quickStartType.getTaskFromString(taskName);
    }

    public static void setLastSkippedQuickStartTask(@Nullable QuickStartTask task) {
        if (task == null) {
            remove(DeletablePrefKey.LAST_SKIPPED_QUICK_START_TASK);
            return;
        }
        setString(DeletablePrefKey.LAST_SKIPPED_QUICK_START_TASK, task.toString());
    }

    public static void setLastSelectedQuickStartTypeForSite(QuickStartType quickStartType, long siteLocalId) {
        Editor editor = prefs().edit();
        editor.putString(
                DeletablePrefKey.LAST_SELECTED_QUICK_START_TYPE + String.valueOf(siteLocalId),
                quickStartType.getLabel()
        );
        editor.apply();
    }

    public static QuickStartType getLastSelectedQuickStartTypeForSite(long siteLocalId) {
        return QuickStartType.Companion.fromLabel(
                prefs().getString(
                        DeletablePrefKey.LAST_SELECTED_QUICK_START_TYPE + String.valueOf(siteLocalId),
                        NewSiteQuickStartType.INSTANCE.getLabel()
                )
        );
    }

    public static void setManualFeatureConfig(boolean isEnabled, String featureKey) {
        prefs().edit().putBoolean(getManualFeatureConfigKey(featureKey), isEnabled).apply();
    }

    public static boolean getManualFeatureConfig(String featureKey) {
        return prefs().getBoolean(getManualFeatureConfigKey(featureKey), false);
    }

    public static boolean hasManualFeatureConfig(String featureKey) {
        return prefs().contains(getManualFeatureConfigKey(featureKey));
    }

    @NonNull private static String getManualFeatureConfigKey(String featureKey) {
        return DeletablePrefKey.MANUAL_FEATURE_CONFIG.name() + featureKey;
    }

    public static void incrementPublishedPostCount() {
        putInt(DeletablePrefKey.PUBLISHED_POST_COUNT, getPublishedPostCount() + 1);
    }

    public static int getPublishedPostCount() {
        return prefs().getInt(DeletablePrefKey.PUBLISHED_POST_COUNT.name(), 0);
    }

    public static void setInAppReviewsShown() {
        putBoolean(DeletablePrefKey.IN_APP_REVIEW_SHOWN, true);
    }

    public static boolean isInAppReviewsShown() {
        return prefs().getBoolean(DeletablePrefKey.IN_APP_REVIEW_SHOWN.name(), false);
    }

    public static void setBloggingRemindersShown(int siteId) {
        prefs().edit().putBoolean(getBloggingRemindersConfigKey(siteId), true).apply();
    }

    public static boolean isBloggingRemindersShown(int siteId) {
        return prefs().getBoolean(getBloggingRemindersConfigKey(siteId), false);
    }

    @NonNull private static String getBloggingRemindersConfigKey(int siteId) {
        return DeletablePrefKey.BLOGGING_REMINDERS_SHOWN.name() + siteId;
    }

    public static void setShouldScheduleCreateSiteNotification(boolean shouldSchedule) {
        setBoolean(DeletablePrefKey.SHOULD_SCHEDULE_CREATE_SITE_NOTIFICATION, shouldSchedule);
    }

    public static boolean shouldScheduleCreateSiteNotification() {
        return getBoolean(DeletablePrefKey.SHOULD_SCHEDULE_CREATE_SITE_NOTIFICATION, true);
    }

    public static void setShouldShowWeeklyRoundupNotification(long remoteSiteId, boolean shouldShow) {
        prefs().edit().putBoolean(getShouldShowWeeklyRoundupNotification(remoteSiteId), shouldShow).apply();
    }

    public static boolean shouldShowWeeklyRoundupNotification(long remoteSiteId) {
        return prefs().getBoolean(getShouldShowWeeklyRoundupNotification(remoteSiteId), true);
    }

    @NonNull private static String getShouldShowWeeklyRoundupNotification(long siteId) {
        return DeletablePrefKey.SHOULD_SHOW_WEEKLY_ROUNDUP_NOTIFICATION.name() + siteId;
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
        return currentIds.contains(key);
    }

    public static void setSiteJetpackCapabilities(long remoteSiteId, List<JetpackCapability> capabilities) {
        HashSet<String> capabilitiesSet = new HashSet(capabilities.size());
        for (JetpackCapability item : capabilities) {
            capabilitiesSet.add(item.toString());
        }

        Editor editor = prefs().edit();
        editor.putStringSet(
                DeletablePrefKey.SITE_JETPACK_CAPABILITIES + String.valueOf(remoteSiteId),
                capabilitiesSet
        );
        editor.apply();
    }

    public static List<JetpackCapability> getSiteJetpackCapabilities(long remoteSiteId) {
        List<JetpackCapability> capabilities = new ArrayList<>();
        Set<String> strings = prefs().getStringSet(
                DeletablePrefKey.SITE_JETPACK_CAPABILITIES + String.valueOf(remoteSiteId),
                new HashSet<>()
        );
        for (String item : strings) {
            capabilities.add(JetpackCapability.Companion.fromString(item));
        }
        return capabilities;
    }

    public static Date getSkippedPromptDay(int siteId) {
        long promptSkippedMillis = prefs().getLong(getSkippedBloggingPromptDayConfigKey(siteId), 0);
        if (promptSkippedMillis == 0) {
            return null;
        }
        return new Date(promptSkippedMillis);
    }

    public static void setSkippedPromptDay(@Nullable Date date, int siteId) {
        if (date == null) {
            prefs().edit().remove(getSkippedBloggingPromptDayConfigKey(siteId)).apply();
            return;
        }
        prefs().edit().putLong(getSkippedBloggingPromptDayConfigKey(siteId), date.getTime()).apply();
    }

    @NonNull private static String getSkippedBloggingPromptDayConfigKey(int siteId) {
        return DeletablePrefKey.SKIPPED_BLOGGING_PROMPT_DAY.name() + siteId;
    }

    public static String getMySiteInitialScreen(boolean isJetpackApp) {
        return getString(
                UndeletablePrefKey.wp_pref_initial_screen,
                isJetpackApp ? MySiteTabType.DASHBOARD.getLabel() : MySiteTabType.SITE_MENU.getLabel()
        );
    }

    public static Boolean getIsFirstBloggingPromptsOnboarding() {
        return getBoolean(UndeletablePrefKey.IS_FIRST_TIME_BLOGGING_PROMPTS_ONBOARDING, true);
    }

    public static void saveFirstBloggingPromptsOnboarding(final boolean isFirstTime) {
        setBoolean(UndeletablePrefKey.IS_FIRST_TIME_BLOGGING_PROMPTS_ONBOARDING, isFirstTime);
    }

    public static Boolean getIsFirstTrySharedLoginJetpack() {
        return getBoolean(UndeletablePrefKey.IS_FIRST_TRY_LOGIN_JETPACK, true);
    }

    public static void saveIsFirstTrySharedLoginJetpack(final boolean isFirstTry) {
        setBoolean(UndeletablePrefKey.IS_FIRST_TRY_LOGIN_JETPACK, isFirstTry);
    }

    public static Boolean getIsFirstTryUserFlagsJetpack() {
        return getBoolean(UndeletablePrefKey.IS_FIRST_TRY_USER_FLAGS_JETPACK, true);
    }

    public static void saveIsFirstTryUserFlagsJetpack(final boolean isFirstTry) {
        setBoolean(UndeletablePrefKey.IS_FIRST_TRY_USER_FLAGS_JETPACK, isFirstTry);
    }

    public static Boolean getIsFirstTryBloggingRemindersSyncJetpack() {
        return getBoolean(UndeletablePrefKey.IS_FIRST_TRY_BLOGGING_REMINDERS_SYNC_JETPACK, true);
    }

    public static void saveIsFirstTryBloggingRemindersSyncJetpack(final boolean isFirstTry) {
        setBoolean(UndeletablePrefKey.IS_FIRST_TRY_BLOGGING_REMINDERS_SYNC_JETPACK, isFirstTry);
    }

    public static Boolean getIsFirstTryReaderSavedPostsJetpack() {
        return getBoolean(UndeletablePrefKey.IS_FIRST_TRY_READER_SAVED_POSTS_JETPACK, true);
    }

    public static void saveIsFirstTryReaderSavedPostsJetpack(final boolean isFirstTry) {
        setBoolean(UndeletablePrefKey.IS_FIRST_TRY_READER_SAVED_POSTS_JETPACK, isFirstTry);
    }

    public static boolean getIsJetpackMigrationCompleted() {
        return getBoolean(UndeletablePrefKey.IS_JETPACK_MIGRATION_COMPLETED, false);
    }

    public static void setIsJetpackMigrationCompleted(final boolean isCompleted) {
        setBoolean(UndeletablePrefKey.IS_JETPACK_MIGRATION_COMPLETED, isCompleted);
    }

    public static boolean getIsJetpackMigrationInProgress() {
        return getBoolean(UndeletablePrefKey.IS_JETPACK_MIGRATION_IS_IN_PROGRESS, false);
    }

    public static void setIsJetpackMigrationInProgress(final boolean isInProgress) {
        setBoolean(UndeletablePrefKey.IS_JETPACK_MIGRATION_IS_IN_PROGRESS, isInProgress);
    }

    public static boolean getIsJetpackMigrationEligible() {
        return getBoolean(UndeletablePrefKey.IS_JETPACK_MIGRATION_ELIGIBLE, true);
    }

    public static void setIsJetpackMigrationEligible(final boolean isEligible) {
        setBoolean(UndeletablePrefKey.IS_JETPACK_MIGRATION_ELIGIBLE, isEligible);
    }

    public static Long getOpenWebLinksWithJetpackOverlayLastShownTimestamp() {
        return getLong(DeletablePrefKey.OPEN_WEB_LINKS_WITH_JETPACK_OVERLAY_LAST_SHOWN_TIMESTAMP, 0L);
    }

    public static void setOpenWebLinksWithJetpackOverlayLastShownTimestamp(final Long overlayLastShownTimestamp) {
        setLong(DeletablePrefKey.OPEN_WEB_LINKS_WITH_JETPACK_OVERLAY_LAST_SHOWN_TIMESTAMP, overlayLastShownTimestamp);
    }

    public static Boolean getIsOpenWebLinksWithJetpack() {
        return getBoolean(DeletablePrefKey.OPEN_WEB_LINKS_WITH_JETPACK, false);
    }

    public static void setIsOpenWebLinksWithJetpack(final boolean isOpenWebLinksWithJetpack) {
        setBoolean(DeletablePrefKey.OPEN_WEB_LINKS_WITH_JETPACK, isOpenWebLinksWithJetpack);
    }

    public static Boolean getShouldHideJetpackFeatureCard(JetpackFeatureRemovalPhase phase) {
        return prefs().getBoolean(getHideJetpackFeatureCardWithPhaseKey(phase), false);
    }

    public static void setShouldHideJetpackFeatureCard(JetpackFeatureRemovalPhase phase, final boolean isHidden) {
        prefs().edit().putBoolean(getHideJetpackFeatureCardWithPhaseKey(phase), isHidden).apply();
    }

    public static Long getJetpackFeatureCardLastShownTimestamp(JetpackFeatureRemovalPhase jetpackFeatureRemovalPhase) {
        return prefs().getLong(getJetpackFeatureCardLastShownTimeStampWithPhaseKey(jetpackFeatureRemovalPhase), 0L);
    }

    public static void setJetpackFeatureCardLastShownTimestamp(JetpackFeatureRemovalPhase jetpackFeatureRemovalPhase,
                                                               final Long lastShownTimestamp) {
        prefs().edit().putLong(getJetpackFeatureCardLastShownTimeStampWithPhaseKey(jetpackFeatureRemovalPhase),
                lastShownTimestamp).apply();
    }

    @NonNull private static String getHideJetpackFeatureCardWithPhaseKey(JetpackFeatureRemovalPhase phase) {
        return DeletablePrefKey.SHOULD_HIDE_JETPACK_FEATURE_CARD.name() + phase.getTrackingName();
    }

    @NonNull
    private static String getJetpackFeatureCardLastShownTimeStampWithPhaseKey(JetpackFeatureRemovalPhase phase) {
        return DeletablePrefKey.JETPACK_FEATURE_CARD_LAST_SHOWN_TIMESTAMP.name() + phase.getTrackingName();
    }

    public static Long getSwitchToJetpackMenuCardLastShownTimestamp() {
        return getLong(DeletablePrefKey.SWITCH_TO_JETPACK_MENU_CARD_SHOWN_TIMESTAMP, 0L);
    }

    public static void setSwitchToJetpackMenuCardLastShownTimestamp(final Long lastShownTimestamp) {
        setLong(DeletablePrefKey.SWITCH_TO_JETPACK_MENU_CARD_SHOWN_TIMESTAMP, lastShownTimestamp);
    }

    public static Boolean getShouldHideSwitchToJetpackMenuCard(JetpackFeatureRemovalPhase phase) {
        return prefs().getBoolean(getHideSwitchToJetpackMenuCardWithPhaseKey(phase), false);
    }

    public static void setShouldHideSwitchToJetpackMenuCard(JetpackFeatureRemovalPhase phase, final boolean isHidden) {
        prefs().edit().putBoolean(getHideSwitchToJetpackMenuCardWithPhaseKey(phase), isHidden).apply();
    }

    @NonNull private static String getHideSwitchToJetpackMenuCardWithPhaseKey(JetpackFeatureRemovalPhase phase) {
        return DeletablePrefKey.SHOULD_HIDE_SWITCH_TO_JETPACK_MENU_CARD.name() + phase.getTrackingName();
    }

    public static Boolean getShouldHideJetpackInstallFullPluginCard(int siteId) {
        return prefs().getBoolean(getHideJetpackInstallFullPluginCardPref(siteId), false);
    }

    public static void setShouldHideJetpackInstallFullPluginCard(int siteId, final boolean isHidden) {
        prefs().edit().putBoolean(getHideJetpackInstallFullPluginCardPref(siteId), isHidden).apply();
    }

    @NonNull private static String getHideJetpackInstallFullPluginCardPref(int siteId) {
        return DeletablePrefKey.SHOULD_HIDE_JETPACK_INSTALL_FULL_PLUGIN_CARD.name() + siteId;
    }

    public static Boolean getShouldShowJetpackFullPluginInstallOnboarding(int siteId) {
        return prefs().getBoolean(getShouldShowJetpackFullPluginInstallOnboardingPref(siteId), true);
    }

    public static void setShouldShowJetpackFullPluginInstallOnboarding(int siteId, final boolean isShown) {
        prefs().edit().putBoolean(getShouldShowJetpackFullPluginInstallOnboardingPref(siteId), isShown).apply();
    }

    @NonNull private static String getShouldShowJetpackFullPluginInstallOnboardingPref(int siteId) {
        return DeletablePrefKey.SHOULD_SHOW_JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING.name() + siteId;
    }

    public static Boolean getShouldHidePromoteWithBlazeCard(long siteId) {
        return prefs().getBoolean(getSiteIdHideBlazeKey(siteId), false);
    }

    public static void setShouldHidePromoteWithBlazeCard(long siteId, final boolean isHidden) {
        prefs().edit().putBoolean(getSiteIdHideBlazeKey(siteId), isHidden).apply();
    }

    @NonNull private static String getSiteIdHideBlazeKey(long siteId) {
        return DeletablePrefKey.SHOULD_HIDE_PROMOTE_WITH_BLAZE_CARD.name() + siteId;
    }

    public static Boolean getShouldHideDashboardPlansCard(long siteId) {
        return prefs().getBoolean(getSiteIdHideDashboardPlansCardKey(siteId), false);
    }

    public static void setShouldHideDashboardPlansCard(long siteId, final boolean isHidden) {
        prefs().edit().putBoolean(getSiteIdHideDashboardPlansCardKey(siteId), isHidden).apply();
    }

    @NonNull private static String getSiteIdHideDashboardPlansCardKey(long siteId) {
        return DeletablePrefKey.SHOULD_HIDE_DASHBOARD_PLANS_CARD.name() + siteId;
    }

    public static int getWPJetpackIndividualPluginOverlayShownCount() {
        return getInt(DeletablePrefKey.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_SHOWN_COUNT, 0);
    }

    public static void incrementWPJetpackIndividualPluginOverlayShownCount() {
        int count = getWPJetpackIndividualPluginOverlayShownCount();
        setInt(DeletablePrefKey.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_SHOWN_COUNT, count + 1);
    }

    public static long getWPJetpackIndividualPluginOverlayLastShownTimestamp() {
        return getLong(DeletablePrefKey.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_LAST_SHOWN_TIMESTAMP, 0);
    }

    public static void setWPJetpackIndividualPluginOverlayLastShownTimestamp(long timestamp) {
        setLong(DeletablePrefKey.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_LAST_SHOWN_TIMESTAMP, timestamp);
    }

    public static boolean getNotificationsPermissionsWarningDismissed() {
        return getBoolean(DeletablePrefKey.NOTIFICATIONS_PERMISSION_WARNING_DISMISSED, false);
    }

    public static void setNotificationsPermissionWarningDismissed(boolean dismissed) {
        setBoolean(DeletablePrefKey.NOTIFICATIONS_PERMISSION_WARNING_DISMISSED, dismissed);
    }

    public static Boolean getShouldHideBlazeOverlay() {
        return getBoolean(DeletablePrefKey.SHOULD_HIDE_BLAZE_OVERLAY, false);
    }

    public static void setShouldHideBlazeOverlay(final boolean isHidden) {
        setBoolean(DeletablePrefKey.SHOULD_HIDE_BLAZE_OVERLAY, isHidden);
    }

    public static Boolean getShouldShowJetpackSocialNoConnections(final long remoteSiteId,
                                                                  final JetpackSocialFlow flow) {
        return prefs().getBoolean(mapShouldShowJetpackSocialNoConnectionsKey(remoteSiteId, flow), true);
    }

    public static void setShouldShowJetpackSocialNoConnections(final boolean show, final long remoteSiteId,
                                                               final JetpackSocialFlow flow) {
        prefs().edit().putBoolean(mapShouldShowJetpackSocialNoConnectionsKey(remoteSiteId, flow), show)
               .apply();
    }

    private static String mapShouldShowJetpackSocialNoConnectionsKey(final long remoteSiteId,
                                                                     final JetpackSocialFlow flow) {
        return DeletablePrefKey.SHOULD_SHOW_JETPACK_SOCIAL_NO_CONNECTIONS.name() + "_" + remoteSiteId + "_"
               + flow.getValue();
    }

    public static void setShouldHideActivityDashboardCard(final long siteId, final boolean isHidden) {
        prefs().edit().putBoolean(getSiteIdHideActivityDashboardCardKey(siteId), isHidden).apply();
    }

    @NonNull private static String getSiteIdHideActivityDashboardCardKey(long siteId) {
        return DeletablePrefKey.SHOULD_HIDE_ACTIVITY_DASHBOARD_CARD.name() + siteId;
    }

    public static Boolean getShouldHideActivityDashboardCard(final long siteId) {
        return prefs().getBoolean(getSiteIdHideActivityDashboardCardKey(siteId), false);
    }

    public static void setShouldHidePagesDashboardCard(final long siteId, final boolean isHidden) {
        prefs().edit().putBoolean(getSiteIdHidePagesDashboardCardKey(siteId), isHidden).apply();
    }

    @NonNull private static String getSiteIdHidePagesDashboardCardKey(long siteId) {
        return DeletablePrefKey.SHOULD_HIDE_PAGES_DASHBOARD_CARD.name() + siteId;
    }

    public static Boolean getShouldHidePagesDashboardCard(final long siteId) {
        return prefs().getBoolean(getSiteIdHidePagesDashboardCardKey(siteId), false);
    }

    public static void setShouldHideTodaysStatsDashboardCard(final long siteId, final boolean isHidden) {
        prefs().edit().putBoolean(getSiteIdHideTodaysStatsDashboardCardKey(siteId), isHidden).apply();
    }

    @NonNull private static String getSiteIdHideTodaysStatsDashboardCardKey(long siteId) {
        return DeletablePrefKey.SHOULD_HIDE_TODAY_STATS_DASHBOARD_CARD.name() + siteId;
    }

    public static Boolean getShouldHideTodaysStatsDashboardCard(final long siteId) {
        return prefs().getBoolean(getSiteIdHideTodaysStatsDashboardCardKey(siteId), false);
    }

    public static void setShouldHidePostDashboardCard(final long siteId, final String postType,
                                                      final boolean isHidden) {
        prefs().edit().putBoolean(getSiteIdHidePostDashboardCardKey(siteId, postType), isHidden).apply();
    }

    @NonNull private static String getSiteIdHidePostDashboardCardKey(long siteId, final String postType) {
        return DeletablePrefKey.SHOULD_HIDE_POST_DASHBOARD_CARD.name() + postType + siteId;
    }

    public static Boolean getShouldHidePostDashboardCard(final long siteId, final String postType) {
        return prefs().getBoolean(getSiteIdHidePostDashboardCardKey(siteId, postType), false);
    }

    public static void setShouldHideNextStepsDashboardCard(final long siteId, final boolean isHidden) {
        prefs().edit().putBoolean(getSiteIdHideNextStepsDashboardCardKey(siteId), isHidden).apply();
    }

    @NonNull private static String getSiteIdHideNextStepsDashboardCardKey(long siteId) {
        return DeletablePrefKey.SHOULD_HIDE_NEXT_STEPS_DASHBOARD_CARD.name() + siteId;
    }

    public static Boolean getShouldHideNextStepsDashboardCard(final long siteId) {
        return prefs().getBoolean(getSiteIdHideNextStepsDashboardCardKey(siteId), false);
    }

    public static void setShouldHideGetToKnowTheAppDashboardCard(final long siteId, final boolean isHidden) {
        prefs().edit().putBoolean(getSiteIdHideGetToKnowTheAppDashboardCardKey(siteId), isHidden).apply();
    }

    @NonNull private static String getSiteIdHideGetToKnowTheAppDashboardCardKey(long siteId) {
        return DeletablePrefKey.SHOULD_HIDE_GET_TO_KNOW_THE_APP_DASHBOARD_CARD.name() + siteId;
    }

    public static Boolean getShouldHideGetToKnowTheAppDashboardCard(final long siteId) {
        return prefs().getBoolean(getSiteIdHideGetToKnowTheAppDashboardCardKey(siteId), false);
    }

    public static void setShouldShowSiteItemAsQuickLink(final String siteItem, final long siteId,
                                                        final boolean isHidden) {
        prefs().edit().putBoolean(getShouldShowSiteItemAsQuickLinkKey(siteItem, siteId), isHidden).apply();
    }

    @NonNull private static String getShouldShowSiteItemAsQuickLinkKey(String siteItem, long siteId) {
        return DeletablePrefKey.SHOULD_SHOW_SITE_ITEM_AS_QUICK_LINK_IN_DASHBOARD.name() + siteItem + siteId;
    }

    public static Boolean getShouldShowSiteItemAsQuickLink(String siteItem, final long siteId) {
        return prefs().getBoolean(getShouldShowSiteItemAsQuickLinkKey(siteItem, siteId), false);
    }

    public static void setShouldShowDefaultQuickLink(final String siteItem, final long siteId,
                                                        final boolean shouldShow) {
        prefs().edit().putBoolean(getShouldShowDefaultQuickLinkKey(siteItem, siteId), shouldShow).apply();
    }

    @NonNull private static String getShouldShowDefaultQuickLinkKey(String siteItem, long siteId) {
        return DeletablePrefKey.SHOULD_SHOW_DEFAULT_QUICK_LINK_IN_DASHBOARD.name() + siteItem + siteId;
    }

    public static Boolean getShouldShowDefaultQuickLink(String siteItem, final long siteId) {
        return prefs().getBoolean(getShouldShowDefaultQuickLinkKey(siteItem, siteId), true);
    }

    @NonNull
    private static String getSiteIdHideBloganuaryNudgeCardKey(long siteId) {
        return DeletablePrefKey.SHOULD_HIDE_BLOGANUARY_NUDGE_CARD.name() + siteId;
    }

    public static void setShouldHideBloganuaryNudgeCard(final long siteId, final boolean isHidden) {
        prefs().edit().putBoolean(getSiteIdHideBloganuaryNudgeCardKey(siteId), isHidden).apply();
    }

    public static boolean getShouldHideBloganuaryNudgeCard(final long siteId) {
        return prefs().getBoolean(getSiteIdHideBloganuaryNudgeCardKey(siteId), false);
    }

    public static void setShouldHideSotw2023NudgeCard(boolean isHidden) {
        prefs().edit().putBoolean(DeletablePrefKey.SHOULD_HIDE_SOTW2023_NUDGE_CARD.name(), isHidden).apply();
    }

    public static boolean getShouldHideSotw2023NudgeCard() {
        return prefs().getBoolean(DeletablePrefKey.SHOULD_HIDE_SOTW2023_NUDGE_CARD.name(), false);
    }

    public static void setShouldHideDynamicCard(@NonNull final String id, final boolean isHidden) {
        prefs().edit().putBoolean(DeletablePrefKey.SHOULD_HIDE_DYNAMIC_CARD.name() + id, isHidden).apply();
    }

    public static boolean getShouldHideDynamicCard(@NonNull final String id) {
        return prefs().getBoolean(DeletablePrefKey.SHOULD_HIDE_DYNAMIC_CARD.name() + id, false);
    }
}
