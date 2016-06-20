package org.wordpress.android.analytics;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.util.EnumMap;
import java.util.Map;

public class AnalyticsTrackerMixpanel extends Tracker {
    public static final String SESSION_COUNT = "sessionCount";

    private MixpanelAPI mMixpanel;
    private EnumMap<AnalyticsTracker.Stat, JSONObject> mAggregatedProperties;
    private static final String MIXPANEL_PLATFORM = "platform";
    private static final String MIXPANEL_SESSION_COUNT = "session_count";
    private static final String DOTCOM_USER = "dotcom_user";
    private static final String JETPACK_USER = "jetpack_user";
    private static final String MIXPANEL_NUMBER_OF_BLOGS = "number_of_blogs";
    private static final String VERSION_CODE = "version_code";
    private static final String APP_LOCALE = "app_locale";
    private static final String MIXPANEL_ANON_ID = "mixpanel_user_anon_id";

    public AnalyticsTrackerMixpanel(Context context, String token) throws IllegalArgumentException {
        super(context);
        mAggregatedProperties = new EnumMap<>(AnalyticsTracker.Stat.class);
        mMixpanel = MixpanelAPI.getInstance(context, token);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void showNotification(Context context, PendingIntent intent, int notificationIcon, CharSequence title,
                                        CharSequence message) {
        final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final Notification.Builder builder = new Notification.Builder(context).setSmallIcon(notificationIcon)
                .setTicker(message).setWhen(System.currentTimeMillis()).setContentTitle(title).setContentText(message)
                .setContentIntent(intent);
        Notification notification;
        notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        nm.notify(0, notification);
    }

    String getAnonIdPrefKey() {
        return MIXPANEL_ANON_ID;
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

        // Just a security check we're tracking the correct user
        if (getWordPressComUserName() == null && getAnonID() == null) {
            this.clearAllData();
            generateNewAnonID();
            mMixpanel.identify(getAnonID());
        }

        trackMixpanelEventForInstructions(instructions, properties);
        trackMixpanelPropertiesForInstructions(instructions);
    }

    private void trackMixpanelPropertiesForInstructions(AnalyticsTrackerMixpanelInstructionsForStat instructions) {
        if (instructions.getPeoplePropertyToIncrement() != null && !instructions.getPeoplePropertyToIncrement()
                                                                                .isEmpty()) {
            incrementPeopleProperty(instructions.getPeoplePropertyToIncrement());
        }

        if (instructions.getSuperPropertyToIncrement() != null && !instructions.getSuperPropertyToIncrement()
                                                                               .isEmpty()) {
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

        if  (instructions.getPeoplePropertiesToAssign() != null
                && instructions.getPeoplePropertiesToAssign().size() > 0) {
            for (Map.Entry<String, Object> entry: instructions.getPeoplePropertiesToAssign().entrySet()) {
                setValueForPeopleProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private void setValueForPeopleProperty(String peopleProperty, Object value) {
        try {
            mMixpanel.getPeople().set(peopleProperty, value);
        } catch (OutOfMemoryError outOfMemoryError) {
            // ignore exception
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
                for (Object o : properties.entrySet()) {
                    Map.Entry pairs = (Map.Entry) o;
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
    public void registerPushNotificationToken(String regId) {
        try {
            mMixpanel.getPeople().setPushRegistrationId(regId);
        } catch (OutOfMemoryError outOfMemoryError) {
            // ignore exception
        }
    }

    @Override
    public void endSession() {
        mAggregatedProperties.clear();
        mMixpanel.flush();
    }

    @Override
    public void flush() {
        mMixpanel.flush();
    }

    @Override
    public void refreshMetadata(boolean isUserConnected, boolean isWordPressComUser, boolean isJetpackUser,
                                int sessionCount, int numBlogs, int versionCode, String username, String email) {
        // Register super properties
        try {
            JSONObject properties = new JSONObject();
            properties.put(MIXPANEL_PLATFORM, "Android");
            properties.put(MIXPANEL_SESSION_COUNT, sessionCount);
            properties.put(DOTCOM_USER, isUserConnected);
            properties.put(JETPACK_USER, isJetpackUser);
            properties.put(MIXPANEL_NUMBER_OF_BLOGS, numBlogs);
            properties.put(VERSION_CODE, versionCode);
            properties.put(APP_LOCALE, mContext.getResources().getConfiguration().locale.toString());
            mMixpanel.registerSuperProperties(properties);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }


        if (isUserConnected && isWordPressComUser) {
            setWordPressComUserName(username);
            // Re-unify the user
            if (getAnonID() != null) {
                mMixpanel.alias(getWordPressComUserName(), getAnonID());
                clearAnonID();
            } else {
                mMixpanel.identify(username);
            }
        } else {
            // Not wpcom connected. Check if anonID is already present
            setWordPressComUserName(null);
            if (getAnonID() == null) {
                generateNewAnonID();
            }
            mMixpanel.identify(getAnonID());
        }

        // Application opened and start.
        if (isUserConnected) {
            try {
                String userID = getWordPressComUserName() != null ? getWordPressComUserName() : getAnonID();
                if (userID == null) {
                    // This should not be an option here
                    return;
                }

                mMixpanel.getPeople().identify(userID);
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("$username", userID);
                if (email != null) {
                    jsonObj.put("$email", email);
                }
                jsonObj.put("$first_name", userID);
                mMixpanel.getPeople().set(jsonObj);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.UTILS, e);
            } catch (OutOfMemoryError outOfMemoryError) {
                // ignore exception
            }
        }
    }

    @Override
    public void clearAllData() {
        super.clearAllData();
        mMixpanel.clearSuperProperties();
        try {
            mMixpanel.getPeople().clearPushRegistrationId();
        } catch (OutOfMemoryError outOfMemoryError) {
            // ignore exception
        }
    }

    private AnalyticsTrackerMixpanelInstructionsForStat instructionsForStat(
            AnalyticsTracker.Stat stat) {
        AnalyticsTrackerMixpanelInstructionsForStat instructions;
        switch (stat) {
            case APPLICATION_OPENED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Application Opened");
                instructions.setSuperPropertyToIncrement("Application Opened");
                incrementSessionCount();
                break;
            case APPLICATION_CLOSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Application Closed");
                break;
            case APPLICATION_INSTALLED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Application Installed");
                break;
            case APPLICATION_UPGRADED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Application Upgraded");
                break;
            case READER_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_reader");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_reader");
                break;
            case READER_ARTICLE_COMMENTED_ON:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Commented on Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                        "number_of_times_commented_on_reader_article");
                instructions.setCurrentDateForPeopleProperty("last_time_commented_on_article");
                break;
            case READER_ARTICLE_LIKED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Liked Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_liked_article");
                instructions.setCurrentDateForPeopleProperty("last_time_liked_reader_article");
                break;
            case READER_ARTICLE_OPENED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Opened Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_article");
                instructions.setCurrentDateForPeopleProperty("last_time_opened_reader_article");
                break;
            case READER_ARTICLE_UNLIKED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Unliked Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_unliked_article");
                instructions.setCurrentDateForPeopleProperty("last_time_unliked_reader_article");
                break;
            case READER_BLOG_BLOCKED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Blocked Blog");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_blocked_a_blog");
                instructions.setCurrentDateForPeopleProperty("last_time_blocked_a_blog");
                break;
            case READER_BLOG_FOLLOWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Followed Site");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_followed_site");
                instructions.setCurrentDateForPeopleProperty("last_time_followed_site");
                break;
            case READER_BLOG_PREVIEWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Blog Preview");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_viewed_blog_preview");
                instructions.setCurrentDateForPeopleProperty("last_time_viewed_blog_preview");
                break;
            case READER_BLOG_UNFOLLOWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Unfollowed Site");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_unfollowed_site");
                instructions.setCurrentDateForPeopleProperty("last_time_unfollowed_site");
                break;
            case READER_DISCOVER_VIEWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Discover Content Viewed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                        "number_of_times_discover_content_viewed");
                instructions.setCurrentDateForPeopleProperty("last_time_discover_content_viewed");
                break;
            case READER_INFINITE_SCROLL:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Infinite Scroll");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                        "number_of_times_reader_performed_infinite_scroll");
                instructions.setCurrentDateForPeopleProperty("last_time_performed_reader_infinite_scroll");
                break;
            case READER_LIST_FOLLOWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Followed List");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_followed_list");
                instructions.setCurrentDateForPeopleProperty("last_time_followed_list");
                break;
            case READER_LIST_LOADED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Loaded List");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_loaded_list");
                instructions.setCurrentDateForPeopleProperty("last_time_loaded_list");
                break;
            case READER_LIST_PREVIEWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - List Preview");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_viewed_list_preview");
                instructions.setCurrentDateForPeopleProperty("last_time_viewed_list_preview");
                break;
            case READER_LIST_UNFOLLOWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Unfollowed List");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_unfollowed_list");
                instructions.setCurrentDateForPeopleProperty("last_time_unfollowed_list");
                break;
            case READER_TAG_FOLLOWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Followed Reader Tag");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_followed_reader_tag");
                instructions.setCurrentDateForPeopleProperty("last_time_followed_reader_tag");
                break;
            case READER_TAG_LOADED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Loaded Tag");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_loaded_tag");
                instructions.setCurrentDateForPeopleProperty("last_time_loaded_tag");
                break;
            case READER_TAG_PREVIEWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Tag Preview");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_viewed_tag_preview");
                instructions.setCurrentDateForPeopleProperty("last_time_viewed_tag_preview");
                break;
            case READER_TAG_UNFOLLOWED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Unfollowed Reader Tag");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_unfollowed_reader_tag");
                instructions.setCurrentDateForPeopleProperty("last_time_unfollowed_reader_tag");
                break;
            case READER_SEARCH_LOADED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Loaded Search");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_reader_search_loaded");
                instructions.setCurrentDateForPeopleProperty("last_time_reader_search_loaded");
                break;
            case EDITOR_CREATED_POST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Created Post");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_created_post");
                instructions.setCurrentDateForPeopleProperty("last_time_created_post_in_editor");
                break;
            case EDITOR_SAVED_DRAFT:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Saved Draft");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_saved_draft");
                instructions.setCurrentDateForPeopleProperty("last_time_saved_draft");
                break;
            case EDITOR_DISCARDED_CHANGES:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Discarded Changes");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_discarded_changes");
                instructions.setCurrentDateForPeopleProperty("last_time_discarded_changes");
                break;
            case EDITOR_EDITED_IMAGE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Edited Image");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_edited_image");
                instructions.setCurrentDateForPeopleProperty("last_time_edited_image");
                break;
            case EDITOR_ENABLED_NEW_VERSION:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Enabled New Version");
                instructions.addSuperPropertyToFlag("enabled_new_editor");
                break;
            case EDITOR_TOGGLED_ON:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Toggled New Editor On");
                instructions.setPeoplePropertyToValue("enabled_new_editor", true);
                break;
            case EDITOR_TOGGLED_OFF:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Toggled New Editor Off");
                instructions.setPeoplePropertyToValue("enabled_new_editor", false);
                break;
            case EDITOR_UPLOAD_MEDIA_FAILED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Upload Media Failed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_upload_media_failed");
                instructions.setCurrentDateForPeopleProperty("last_time_editor_upload_media_failed");
                break;
            case EDITOR_UPLOAD_MEDIA_RETRIED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Retried Uploading Media");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_retried_uploading_media");
                instructions.setCurrentDateForPeopleProperty("last_time_editor_retried_uploading_media");
                break;
            case EDITOR_CLOSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Closed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_closed");
                break;
            case EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Added Photo via Local Library");
                instructions.
                        setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_added_photo_via_local_library");
                instructions.setCurrentDateForPeopleProperty("last_time_added_photo_via_local_library_to_post");
                break;
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Added Photo via WP Media Library");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                        "number_of_times_added_photo_via_wp_media_library");
                instructions.setCurrentDateForPeopleProperty("last_time_added_photo_via_wp_media_library_to_post");
                break;
            case EDITOR_ADDED_VIDEO_VIA_LOCAL_LIBRARY:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Added Video via Local Library");
                instructions.
                        setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_added_video_via_local_library");
                instructions.setCurrentDateForPeopleProperty("last_time_added_video_via_local_library_to_post");
                break;
            case EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Added Video via WP Media Library");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                        "number_of_times_added_video_via_wp_media_library");
                instructions.setCurrentDateForPeopleProperty("last_time_added_video_via_wp_media_library_to_post");
                break;
            case EDITOR_PUBLISHED_POST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Published Post");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_published_post");
                instructions.setCurrentDateForPeopleProperty("last_time_published_post");
                break;
            case EDITOR_UPDATED_POST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Updated Post");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_updated_post");
                instructions.setCurrentDateForPeopleProperty("last_time_updated_post");
                break;
            case EDITOR_SCHEDULED_POST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Scheduled Post");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_scheduled_post");
                instructions.setCurrentDateForPeopleProperty("last_time_scheduled_post");
                break;
            case EDITOR_TAPPED_BLOCKQUOTE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Blockquote Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_blockquote");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_blockquote_in_editor");
                break;
            case EDITOR_TAPPED_BOLD:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Bold Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_bold");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_bold_in_editor");
                break;
            case EDITOR_TAPPED_IMAGE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Image Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_image");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_image_in_editor");
                break;
            case EDITOR_TAPPED_ITALIC:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Italics Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_italic");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_italic_in_editor");
                break;
            case EDITOR_TAPPED_LINK:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Link Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_link");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_link_in_editor");
                break;
            case EDITOR_TAPPED_MORE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped More Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_more");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_more_in_editor");
                break;
            case EDITOR_TAPPED_STRIKETHROUGH:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Strikethrough Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_strikethrough");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_strikethrough_in_editor");
                break;
            case EDITOR_TAPPED_UNDERLINE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Underline Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_underline");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_underline_in_editor");
                break;
            case EDITOR_TAPPED_HTML:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped HTML Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_html");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_html_in_editor");
                break;
            case EDITOR_TAPPED_ORDERED_LIST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Ordered List Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_ordered_list");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_ordered_list_in_editor");
                break;
            case EDITOR_TAPPED_UNLINK:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Unlink Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_unlink");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_unlink_in_editor");
                break;
            case EDITOR_TAPPED_UNORDERED_LIST:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Editor - Tapped Unordered List Button");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_editor_tapped_unordered_list");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_unordered_list_in_editor");
                break;
            case NOTIFICATIONS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Notifications - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_notifications");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_notifications");
                break;
            case NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Notifications - Opened Notification Details");
                instructions.
                        setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_notification_details");
                instructions.setCurrentDateForPeopleProperty("last_time_opened_notification_details");
                break;
            case NOTIFICATION_APPROVED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_notifications_approved");
                break;
            case NOTIFICATION_UNAPPROVED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_notifications_unapproved");
                break;
            case NOTIFICATION_REPLIED_TO:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_notifications_replied_to");
                break;
            case NOTIFICATION_TRASHED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_notifications_trashed");
                break;
            case NOTIFICATION_FLAGGED_AS_SPAM:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_notifications_flagged_as_spam");
                break;
            case NOTIFICATION_LIKED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Notifications - Liked Comment");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_comment_likes_from_notification");
                break;
            case NOTIFICATION_UNLIKED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Notifications - Unliked Comment");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_comment_unlikes_from_notification");
                break;
            case OPENED_POSTS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Site Menu - Opened Posts");
                break;
            case OPENED_PAGES:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Site Menu - Opened Pages");
                break;
            case OPENED_COMMENTS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Site Menu - Opened Comments");
                break;
            case OPENED_VIEW_SITE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Site Menu - Opened View Site");
                break;
            case OPENED_VIEW_ADMIN:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Site Menu - Opened View Admin");
                break;
            case OPENED_MEDIA_LIBRARY:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Site Menu - Opened Media Library");
                break;
            case OPENED_BLOG_SETTINGS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Site Menu - Opened Site Settings");
                break;
            case OPENED_ACCOUNT_SETTINGS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Opened Account Settings");
                break;
            case OPENED_APP_SETTINGS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Opened App Settings");
                break;
            case OPENED_MY_PROFILE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Opened My Profile");
                break;
            case OPENED_PEOPLE_MANAGEMENT:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("People Management - Accessed List");
                break;
            case OPENED_PERSON:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("People Management - Accessed Details");
                break;
            case CREATED_ACCOUNT:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Created Account");
                instructions.setCurrentDateForPeopleProperty("$created");
                instructions.addSuperPropertyToFlag("created_account_on_mobile");
                break;
            case SHARED_ITEM:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor("number_of_items_shared");
                break;
            case ADDED_SELF_HOSTED_SITE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Added Self Hosted Site");
                instructions.setCurrentDateForPeopleProperty("last_time_added_self_hosted_site");
                break;
            case SIGNED_IN:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Signed In");
                break;
            case SIGNED_INTO_JETPACK:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Signed into Jetpack");
                instructions.addSuperPropertyToFlag("jetpack_user");
                instructions.addSuperPropertyToFlag("dotcom_user");
                break;
            case ACCOUNT_LOGOUT:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Logged Out");
                break;
            case PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Signed into Jetpack from Stats Screen");
                break;
            case STATS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_stats");
                break;
            case STATS_INSIGHTS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Insights Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_insights_screen_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_insights_screen_stats");
                break;
            case STATS_PERIOD_DAYS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Period Days Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_days_screen_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_days_screen_stats");
                break;
            case STATS_PERIOD_WEEKS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Period Weeks Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_weeks_screen_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_weeks_screen_stats");
                break;
            case STATS_PERIOD_MONTHS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Period Months Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_months_screen_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_months_screen_stats");
                break;
            case STATS_PERIOD_YEARS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Period Years Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_years_screen_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_years_screen_stats");
                break;
            case STATS_VIEW_ALL_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - View All Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_view_all_screen_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_view_all_screen_stats");
                break;
            case STATS_SINGLE_POST_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Single Post Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_single_post_screen_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_single_post_screen_stats");
                break;
            case STATS_TAPPED_BAR_CHART:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Tapped Bar Chart");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_tapped_stats_bar_chart");
                instructions.setCurrentDateForPeopleProperty("last_time_tapped_stats_bar_chart");
                break;
            case STATS_SCROLLED_TO_BOTTOM:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Scrolled to Bottom");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_scrolled_to_bottom_of_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_scrolled_to_bottom_of_stats");
                break;
            case STATS_SELECTED_INSTALL_JETPACK:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Selected Install Jetpack");
                break;
            case STATS_SELECTED_CONNECT_JETPACK:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Selected Connect Jetpack");
                break;
            case STATS_WIDGET_ADDED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Widget Added");
                break;
            case STATS_WIDGET_REMOVED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Widget Removed");
                break;
            case STATS_WIDGET_TAPPED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Widget Tapped");
                break;
            case PUSH_NOTIFICATION_RECEIVED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Push Notification - Received");
                break;
            case PUSH_NOTIFICATION_TAPPED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Push Notification - Alert Tapped");
                break;
            case SUPPORT_OPENED_HELPSHIFT_SCREEN:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Support - Opened Helpshift Screen");
                instructions.addSuperPropertyToFlag("opened_helpshift_screen");
                break;
            case SUPPORT_SENT_REPLY_TO_SUPPORT_MESSAGE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Support - Replied to Helpshift");
                instructions.addSuperPropertyToFlag("support_replied_to_helpshift");
                break;
            case LOGIN_MAGIC_LINK_EXITED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Magic Link exited");
                break;
            case LOGIN_MAGIC_LINK_FAILED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Magic Link failed");
                break;
            case LOGIN_MAGIC_LINK_OPENED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Magic Link opened");
                break;
            case LOGIN_MAGIC_LINK_REQUESTED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Magic Link requested");
                break;
            case LOGIN_MAGIC_LINK_SUCCEEDED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Magic Link succeeded");
                break;
            case LOGIN_FAILED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Failed Login");
                break;
            case LOGIN_FAILED_TO_GUESS_XMLRPC:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Failed To Guess XMLRPC");
                break;
            case LOGIN_INSERTED_INVALID_URL:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Inserted Invalid URL");
                break;
            case LOGIN_AUTOFILL_CREDENTIALS_FILLED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Auto Fill Credentials Filled");
                break;
            case LOGIN_AUTOFILL_CREDENTIALS_UPDATED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Auto Fill Credentials Updated");
                break;
            case PERSON_REMOVED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("People Management - Removed Person");
                break;
            case PERSON_UPDATED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("People Management - Updated Person");
                break;
            case PUSH_AUTHENTICATION_APPROVED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Push Authentication - Approved");
                break;
            case PUSH_AUTHENTICATION_EXPIRED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Push Authentication - Expired");
                break;
            case PUSH_AUTHENTICATION_FAILED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Push Authentication - Failed");
                break;
            case PUSH_AUTHENTICATION_IGNORED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Push Authentication - Ignored");
                break;
            case NOTIFICATION_SETTINGS_LIST_OPENED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Notification Settings - Accessed List");
                break;
            case NOTIFICATION_SETTINGS_STREAMS_OPENED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Notification Settings - Accessed Stream");
                break;
            case NOTIFICATION_SETTINGS_DETAILS_OPENED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Notification Settings - Accessed Details");
                break;
            case ME_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me Tab - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_me_tab");
                break;
            case ME_GRAVATAR_TAPPED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Tapped Gravatar");
                break;
            case ME_GRAVATAR_TOOLTIP_TAPPED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Tapped Gravatar Tooltip");
                break;
            case ME_GRAVATAR_PERMISSIONS_INTERRUPTED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Gravatar Permissions Interrupted");
                break;
            case ME_GRAVATAR_PERMISSIONS_DENIED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Gravatar Permissions Denied");
                break;
            case ME_GRAVATAR_PERMISSIONS_ACCEPTED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Gravatar Permissions Accepted");
                break;
            case ME_GRAVATAR_SHOT_NEW:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Gravatar Shot New Photo");
                break;
            case ME_GRAVATAR_GALLERY_PICKED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Gravatar Picked From Gallery");
                break;
            case ME_GRAVATAR_CROPPED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Gravatar Cropped");
                break;
            case ME_GRAVATAR_UPLOADED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Gravatar Uploaded");
                break;
            case ME_GRAVATAR_UPLOAD_UNSUCCESSFUL:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Gravatar Upload Unsuccessful");
                break;
            case ME_GRAVATAR_UPLOAD_EXCEPTION:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Me - Gravatar Upload Exception");
                break;
            case MY_SITE_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("My Site - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_my_site");
                break;
            case THEMES_ACCESSED_THEMES_BROWSER:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Accessed Theme Browser");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_theme_browser");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_theme_browser");
                break;
            case THEMES_ACCESSED_SEARCH:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Accessed Theme");
                break;
            case THEMES_CHANGED_THEME:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Changed Theme");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_changed_theme");
                instructions.setCurrentDateForPeopleProperty("last_time_changed_theme");
                break;
            case THEMES_PREVIEWED_SITE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Previewed Theme for Site");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_previewed_a_theme");
                instructions.setCurrentDateForPeopleProperty("last_time_previewed_a_theme");
                break;
            case THEMES_DEMO_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Demo Accessed");
                break;
            case THEMES_CUSTOMIZE_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Customize Accessed");
                break;
            case THEMES_SUPPORT_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Support Accessed");
                break;
            case THEMES_DETAILS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Details Accessed");
                break;
            case ACCOUNT_SETTINGS_LANGUAGE_CHANGED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Account Settings - Changed Language");
                break;
            case SITE_SETTINGS_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Settings - Site Settings Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_settings_accessed");
                break;
            case SITE_SETTINGS_ACCESSED_MORE_SETTINGS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Settings - More Settings Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_more_settings_accessed");
                break;
            case SITE_SETTINGS_ADDED_LIST_ITEM:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Settings - Added List Item");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_list_items_added");
                break;
            case SITE_SETTINGS_DELETED_LIST_ITEMS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Settings - Site Deleted List Items");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_list_items_were_deleted");
                break;
            case SITE_SETTINGS_HINT_TOAST_SHOWN:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Settings - Preference Hint Shown");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_preference_hints_viewed");
                break;
            case SITE_SETTINGS_LEARN_MORE_CLICKED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Settings - Learn More Clicked");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_learn_more_clicked");
                break;
            case SITE_SETTINGS_LEARN_MORE_LOADED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Settings - Learn More Loaded");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_learn_more_seen");
                break;
            case SITE_SETTINGS_SAVED_REMOTELY:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Settings - Saved Remotely");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_settings_updated_remotely");
                break;
            case SITE_SETTINGS_START_OVER_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Start Over Accessed");
                break;
            case SITE_SETTINGS_START_OVER_CONTACT_SUPPORT_CLICKED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Start Over Contact Support Clicked");
                break;
            case SITE_SETTINGS_EXPORT_SITE_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Export Site Accessed");
                break;
            case SITE_SETTINGS_EXPORT_SITE_REQUESTED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Export Site Requested");
                break;
            case SITE_SETTINGS_EXPORT_SITE_RESPONSE_OK:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Export Site Response OK");
                break;
            case SITE_SETTINGS_EXPORT_SITE_RESPONSE_ERROR:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Export Site Response Error");
                break;
            case SITE_SETTINGS_DELETE_SITE_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Delete Site Accessed");
                break;
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_REQUESTED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Delete Site Purchases Requested");
                break;
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOWN:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Delete Site Purchases Shown");
                break;
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOW_CLICKED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Delete Site Show Purchases Clicked");
                break;
            case SITE_SETTINGS_DELETE_SITE_REQUESTED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Delete Site Requested");
                break;
            case SITE_SETTINGS_DELETE_SITE_RESPONSE_OK:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Delete Site Response OK");
                break;
            case SITE_SETTINGS_DELETE_SITE_RESPONSE_ERROR:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Settings - Delete Site Response Error");
                break;
            case ABTEST_START:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("AB Test - Started");
                break;
            default:
                instructions = null;
                break;
        }
        return instructions;
    }

    private void incrementPeopleProperty(String property) {
        try {
            mMixpanel.getPeople().increment(property, 1);
        } catch (OutOfMemoryError outOfMemoryError) {
            // ignore exception
        }
    }

    @SuppressLint("CommitPrefEdits")
    private void incrementSuperProperty(String property) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        int propertyCount = preferences.getInt(property, 0);
        propertyCount++;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(property, propertyCount);
        editor.commit();

        try {
            JSONObject superProperties = mMixpanel.getSuperProperties();
            superProperties.put(property, propertyCount);
            mMixpanel.registerSuperProperties(superProperties);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }
    }

    private void flagSuperProperty(String property) {
        try {
            JSONObject superProperties = mMixpanel.getSuperProperties();
            superProperties.put(property, true);
            mMixpanel.registerSuperProperties(superProperties);
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
            return properties.get(property);
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


    public void incrementSessionCount() {
        // Tracking session count will help us isolate users who just installed the app
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        int sessionCount = preferences.getInt(SESSION_COUNT, 0);
        sessionCount++;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SESSION_COUNT, sessionCount);
        editor.apply();
    }
}
