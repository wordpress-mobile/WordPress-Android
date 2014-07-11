package org.wordpress.android.util.stats;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

public class AnalyticsTrackerMixpanel implements AnalyticsTracker.Tracker {
    private MixpanelAPI mMixpanel;
    private EnumMap<AnalyticsTracker.Stat, JSONObject> mAggregatedProperties;
    private static final String SESSION_COUNT = "sessionCount";
    private static final String MIXPANEL_PLATFORM = "platform";
    private static final String MIXPANEL_SESSION_COUNT = "session_count";
    private static final String DOTCOM_USER = "dotcom_user";
    private static final String JETPACK_USER = "jetpack_user";
    private static final String MIXPANEL_NUMBER_OF_BLOGS = "number_of_blogs";

    public AnalyticsTrackerMixpanel() {
        mAggregatedProperties = new EnumMap<AnalyticsTracker.Stat, JSONObject>(AnalyticsTracker.Stat.class);
        mMixpanel = MixpanelAPI.getInstance(WordPress.getContext(), BuildConfig.MIXPANEL_TOKEN);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat) {
        track(stat, null);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, Map<String, ?> properties) {
        AnalyticsTrackerMixpanelInstructionsForStat instructions = instructionsForStat(stat);

        if (instructions == null) {
            return;
        }

        trackMixpanelDataForInstructions(instructions, properties);
    }

    private void trackMixpanelDataForInstructions(AnalyticsTrackerMixpanelInstructionsForStat instructions,
                                                  Map<String, ?> properties) {
        if (instructions.getDisableForSelfHosted()) {
            return;
        }

        trackMixpanelEventForInstructions(instructions, properties);
        trackMixpanelPropertiesForInstructions(instructions);
    }

    private void trackMixpanelPropertiesForInstructions(AnalyticsTrackerMixpanelInstructionsForStat instructions) {
        if (instructions.getPeoplePropertyToIncrement() != null
                && !instructions.getPeoplePropertyToIncrement().isEmpty()) {
            incrementPeopleProperty(instructions.getPeoplePropertyToIncrement());
        }

        if (instructions.getSuperPropertyToIncrement() != null
                && !instructions.getSuperPropertyToIncrement().isEmpty()) {
            incrementSuperProperty(instructions.getSuperPropertyToIncrement());
        }

        if (instructions.getPropertyToIncrement() != null && !instructions.getPropertyToIncrement().isEmpty()) {
            incrementProperty(instructions.getPropertyToIncrement(), instructions.getStatToAttachProperty());
        }

        if (instructions.getSuperPropertiesToFlag() != null && instructions.getSuperPropertiesToFlag().size() > 0) {
            for (String superPropertyToFlag : instructions.getSuperPropertiesToFlag()) {
                flagSuperProperty(superPropertyToFlag);
            }
        }
    }

    private void trackMixpanelEventForInstructions(AnalyticsTrackerMixpanelInstructionsForStat instructions,
                                                   Map<String, ?> properties) {
        String eventName = instructions.getMixpanelEventName();
        if (eventName != null && !eventName.isEmpty()) {
            JSONObject savedPropertiesForStat = propertiesForStat(instructions.getStat());
            if (savedPropertiesForStat == null) {
                savedPropertiesForStat = new JSONObject();
            }

            // Retrieve properties user has already passed in and combine them with the saved properties
            if (properties != null) {
                Iterator iter = properties.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry pairs = (Map.Entry) iter.next();
                    String key = (String) pairs.getKey();
                    try {
                        Object value = pairs.getValue();
                        savedPropertiesForStat.put(key, value);
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.UTILS, e);
                    }
                }
            }
            mMixpanel.track(eventName, savedPropertiesForStat);
            removePropertiesForStat(instructions.getStat());
        }
    }

    @Override
    public void beginSession() {
        // Tracking session count will help us isolate users who just installed the app
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        int sessionCount = preferences.getInt(SESSION_COUNT, 0);
        sessionCount++;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SESSION_COUNT, sessionCount);
        editor.commit();

        // Register super properties
        boolean connected = WordPress.hasValidWPComCredentials(WordPress.getContext());
        boolean jetpackUser = WordPress.wpDB.hasAnyJetpackBlogs();
        int numBlogs = WordPress.wpDB.getVisibleAccounts().size();
        try {
            JSONObject properties = new JSONObject();
            properties.put(MIXPANEL_PLATFORM, "Android");
            properties.put(MIXPANEL_SESSION_COUNT, sessionCount);
            properties.put(DOTCOM_USER, connected);
            properties.put(JETPACK_USER, jetpackUser);
            properties.put(MIXPANEL_NUMBER_OF_BLOGS, numBlogs);
            mMixpanel.registerSuperProperties(properties);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        // Application opened and start.
        if (connected) {
            String username = preferences.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            mMixpanel.identify(username);
            mMixpanel.getPeople().identify(username);

            try {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("$username", username);
                jsonObj.put("$first_name", username);
                mMixpanel.getPeople().set(jsonObj);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.UTILS, e);
            }
        }
    }

    @Override
    public void endSession() {
        mAggregatedProperties.clear();
        mMixpanel.flush();
    }

    @Override
    public void clearAllData() {
        mMixpanel.clearSuperProperties();
        mMixpanel.getPeople().clearPushRegistrationId();
    }

    private AnalyticsTrackerMixpanelInstructionsForStat instructionsForStat(AnalyticsTracker.Stat stat) {
        AnalyticsTrackerMixpanelInstructionsForStat instructions = null;
        switch (stat) {
            case APPLICATION_OPENED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Application Opened");
                instructions.setSuperPropertyToIncrement("Application Opened");
                break;
            case APPLICATION_CLOSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Application Closed");
                break;
            case THEMES_ACCESSED_THEMES_BROWSER:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Accessed Theme Browser");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_theme_browser");
                break;
            case THEMES_CHANGED_THEME:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Changed Theme");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_changed_theme");
                break;
            case READER_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_reader");
                break;
            case READER_OPENED_ARTICLE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Opened Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_article");
                break;
            case READER_LIKED_ARTICLE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Liked Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_liked_article");
                break;
            case READER_REBLOGGED_ARTICLE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Reblogged Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_reblogged_article");
                break;
            case READER_INFINITE_SCROLL:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Infinite Scroll");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                        "number_of_times_reader_performed_infinite_scroll");
                break;
            case READER_FOLLOWED_READER_TAG:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Followed Reader Tag");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_followed_reader_tag");
                break;
            case READER_UNFOLLOWED_READER_TAG:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Unfollowed Reader Tag");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_unfollowed_reader_tag");
                break;
            case READER_LOADED_TAG:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Loaded Tag");
                break;
            case READER_LOADED_FRESHLY_PRESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Loaded Freshly Pressed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_loaded_freshly_pressed");
                break;
            case READER_COMMENTED_ON_ARTICLE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Commented on Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_commented_on_article");
                break;
            case READER_FOLLOWED_SITE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Followed Site");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_followed_site");
                break;
            case EDITOR_CREATED_POST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Created Post");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_created_post");
                break;
            case EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Added Photo via Local Library");
                instructions.
                        setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_added_photo_via_local_library");
                break;
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Added Photo via WP Media Library");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                        "number_of_times_added_photo_via_wp_media_library");
                break;
            case EDITOR_PUBLISHED_POST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Published Post");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_published_post");
                break;
            case EDITOR_UPDATED_POST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Updated Post");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_updated_post");
                break;
            case EDITOR_SCHEDULED_POST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Scheduled Post");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_scheduled_post");
                break;
            case EDITOR_PUBLISHED_POST_WITH_PHOTO:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_posts_published_with_photos");
                break;
            case EDITOR_PUBLISHED_POST_WITH_VIDEO:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_posts_published_with_videos");
              break;
            case EDITOR_PUBLISHED_POST_WITH_CATEGORIES:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_posts_published_with_categories");
                break;
            case EDITOR_PUBLISHED_POST_WITH_TAGS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                      mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                              "number_of_posts_published_with_tags");
                break;
            case NOTIFICATIONS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Notifications - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_notifications");
                break;
            case NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Notifications - Opened Notification Details");
                instructions.
                        setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_notification_details");
                break;
            case NOTIFICATION_PERFORMED_ACTION:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_times_notifications_performed_action_against");
                break;
            case NOTIFICATION_APPROVED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_times_notifications_approved");
                break;
            case NOTIFICATION_REPLIED_TO:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_times_notifications_replied_to");
                break;
            case NOTIFICATION_TRASHED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_times_notifications_trashed");
                break;
            case NOTIFICATION_FLAGGED_AS_SPAM:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_times_notifications_flagged_as_spam");
                break;
            case OPENED_POSTS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithPropertyIncrementor(
                                "number_of_times_opened_posts", AnalyticsTracker.Stat.APPLICATION_CLOSED);
                break;
            case OPENED_PAGES:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithPropertyIncrementor(
                                "number_of_times_opened_pages", AnalyticsTracker.Stat.APPLICATION_CLOSED);
                break;
            case OPENED_COMMENTS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithPropertyIncrementor(
                                "number_of_times_opened_comments", AnalyticsTracker.Stat.APPLICATION_CLOSED);
                break;
            case OPENED_VIEW_SITE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithPropertyIncrementor(
                                "number_of_times_opened_view_site", AnalyticsTracker.Stat.APPLICATION_CLOSED);
                break;
            case OPENED_VIEW_ADMIN:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithPropertyIncrementor(
                                "number_of_times_opened_view_admin", AnalyticsTracker.Stat.APPLICATION_CLOSED);
                instructions.
                        setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_view_admin");
                break;
            case OPENED_MEDIA_LIBRARY:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithPropertyIncrementor(
                                "number_of_times_opened_media_library", AnalyticsTracker.Stat.APPLICATION_CLOSED);
                break;
            case OPENED_SETTINGS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithPropertyIncrementor(
                                "number_of_times_opened_settings", AnalyticsTracker.Stat.APPLICATION_CLOSED);
                break;
            case CREATED_ACCOUNT:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Created Account");
                break;
            case CREATED_SITE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Created Site");
                break;
            case SHARED_ITEM:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor("number_of_items_share");
                break;
            case ADDED_SELF_HOSTED_SITE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Added Self Hosted Site");
                break;
            case SIGNED_INTO_JETPACK:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Signed into Jetpack");
                instructions.addSuperPropertyToFlag("jetpack_user");
                instructions.addSuperPropertyToFlag("dotcom_user");
                break;
            case PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Signed into Jetpack from Stats Screen");
                break;
            case STATS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_stats");
                break;
            case STATS_SELECTED_INSTALL_JETPACK:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Selected Install Jetpack");
                break;
            default:
                instructions = null;
                break;
        }
        return instructions;
    }

    private void incrementPeopleProperty(String property) {
        mMixpanel.getPeople().increment(property, 1);
    }

    private void incrementSuperProperty(String property) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        int propertyCount = preferences.getInt(property, 0);
        propertyCount++;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(property, propertyCount);
        editor.commit();

        try {
            JSONObject superProperty = new JSONObject();
            superProperty.put(property, propertyCount);
            mMixpanel.registerSuperProperties(superProperty);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }
    }

    private void flagSuperProperty(String property) {
        try {
            JSONObject superProperty = new JSONObject();
            superProperty.put(property, true);
            mMixpanel.registerSuperProperties(superProperty);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }
    }

    private void savePropertyValueForStat(String property, Object value, AnalyticsTracker.Stat stat) {
        JSONObject properties = mAggregatedProperties.get(stat);
        if (properties == null) {
            properties = new JSONObject();
            mAggregatedProperties.put(stat, properties);
        }

        try {
            properties.put(property, value);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }
    }

    private JSONObject propertiesForStat(AnalyticsTracker.Stat stat) {
        return mAggregatedProperties.get(stat);
    }

    private void removePropertiesForStat(AnalyticsTracker.Stat stat) {
        mAggregatedProperties.remove(stat);
    }

    private Object propertyForStat(String property, AnalyticsTracker.Stat stat) {
        JSONObject properties = mAggregatedProperties.get(stat);
        if (properties == null) {
            return null;
        }

        try {
            Object valueForProperty  = properties.get(property);
            return valueForProperty;
        } catch (JSONException e) {
            // We are okay with swallowing this exception as the next line will just return a null value
        }

        return null;
    }

    private void incrementProperty(String property, AnalyticsTracker.Stat stat) {
        Object currentValueObj = propertyForStat(property, stat);
        int currentValue = 1;
        if (currentValueObj != null) {
            currentValue = Integer.valueOf(currentValueObj.toString());
            currentValue++;
        }

        savePropertyValueForStat(property, Integer.toString(currentValue), stat);
    }
}

