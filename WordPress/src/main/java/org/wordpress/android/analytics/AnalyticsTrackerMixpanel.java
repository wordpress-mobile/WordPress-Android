package org.wordpress.android.analytics;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.PackageUtils;

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
    private static final String VERSION_CODE = "version_code";

    public AnalyticsTrackerMixpanel() {
        mAggregatedProperties = new EnumMap<AnalyticsTracker.Stat, JSONObject>(AnalyticsTracker.Stat.class);
        mMixpanel = MixpanelAPI.getInstance(WordPress.getContext(), BuildConfig.MIXPANEL_TOKEN);
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
        if (Build.VERSION.SDK_INT < 16) {
            notification = builder.getNotification();
        } else {
            notification = builder.build();
        }
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        nm.notify(0, notification);
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

    private void retrieveAndRegisterEmailAddressIfApplicable() {
        // Once the email address is bound to a mixpanel profile, we don't need to set (and get it) a second time.
        if (AppPrefs.getMixpanelEmailRetrievalCheck()) {
            return;
        }
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    if (jsonObject != null && !TextUtils.isEmpty(jsonObject.getString("email"))) {
                        String email = jsonObject.getString("email");
                        setValueForPeopleProperty("$email", email);
                        AppPrefs.setMixpanelEmailRetrievalCheck(true);
                    }
                } catch (JSONException e) {
                    AppLog.e(T.UTILS, "Can't get email field from json response: " + jsonObject);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.UTILS, volleyError);
            }
        };

        String path = "/me";
        WordPress.getRestClientUtils().get(path, listener, errorListener);
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
        refreshMetadata();
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
    public void refreshMetadata() {
        // Register super properties
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        int sessionCount = preferences.getInt(SESSION_COUNT, 0);
        boolean connected = WordPress.hasDotComToken(WordPress.getContext());
        boolean jetpackUser = WordPress.wpDB.hasAnyJetpackBlogs();
        int numBlogs = WordPress.wpDB.getVisibleAccounts().size();
        try {
            JSONObject properties = new JSONObject();
            properties.put(MIXPANEL_PLATFORM, "Android");
            properties.put(MIXPANEL_SESSION_COUNT, sessionCount);
            properties.put(DOTCOM_USER, connected);
            properties.put(JETPACK_USER, jetpackUser);
            properties.put(MIXPANEL_NUMBER_OF_BLOGS, numBlogs);
            properties.put(VERSION_CODE, PackageUtils.getVersionCode(WordPress.getContext()));
            mMixpanel.registerSuperProperties(properties);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        // Application opened and start.
        if (connected) {
            String username = preferences.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            mMixpanel.identify(username);
            try {
                mMixpanel.getPeople().identify(username);
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("$username", username);
                jsonObj.put("$first_name", username);
                mMixpanel.getPeople().set(jsonObj);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.UTILS, e);
            } catch (OutOfMemoryError outOfMemoryError) {
                // ignore exception
            }

            retrieveAndRegisterEmailAddressIfApplicable();
        }
    }

    @Override
    public void clearAllData() {
        mMixpanel.clearSuperProperties();
        try {
            mMixpanel.getPeople().clearPushRegistrationId();
        } catch (OutOfMemoryError outOfMemoryError) {
            // ignore exception
        }
    }

    private AnalyticsTrackerMixpanelInstructionsForStat instructionsForStat(
            AnalyticsTracker.Stat stat) {
        AnalyticsTrackerMixpanelInstructionsForStat instructions = null;
        switch (stat) {
            case APPLICATION_STARTED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Application Started");
                instructions.setSuperPropertyToIncrement("Application Started");
                break;
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
            case THEMES_ACCESSED_THEMES_BROWSER:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Themes - Accessed Theme Browser");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_theme_browser");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_theme_browser");
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
            case READER_ACCESSED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_reader");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_reader");
                break;
            case READER_OPENED_ARTICLE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Opened Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_article");
                instructions.setCurrentDateForPeopleProperty("last_time_opened_reader_article");
                break;
            case READER_LIKED_ARTICLE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Liked Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_liked_article");
                instructions.setCurrentDateForPeopleProperty("last_time_liked_reader_article");
                break;
            case READER_REBLOGGED_ARTICLE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Reblogged Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_reblogged_article");
                instructions.setCurrentDateForPeopleProperty("last_time_reblogged_article");
                break;
            case READER_INFINITE_SCROLL:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Infinite Scroll");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                        "number_of_times_reader_performed_infinite_scroll");
                instructions.setCurrentDateForPeopleProperty("last_time_performed_reader_infinite_scroll");
                break;
            case READER_FOLLOWED_READER_TAG:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Followed Reader Tag");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_followed_reader_tag");
                instructions.setCurrentDateForPeopleProperty("last_time_followed_reader_tag");
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
                instructions.setCurrentDateForPeopleProperty("last_time_loaded_freshly_pressed");
                break;
            case READER_COMMENTED_ON_ARTICLE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Commented on Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement(
                        "number_of_times_commented_on_reader_article");
                instructions.setCurrentDateForPeopleProperty("last_time_commented_on_article");
                break;
            case READER_FOLLOWED_SITE:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Followed Site");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_followed_site");
                instructions.setCurrentDateForPeopleProperty("last_time_followed_site");
                break;
            case READER_BLOCKED_BLOG:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Blocked Blog");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_blocked_a_blog");
                instructions.setCurrentDateForPeopleProperty("last_time_blocked_a_blog");
                break;
            case READER_BLOG_PREVIEW:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Blog Preview");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_viewed_blog_preview");
                instructions.setCurrentDateForPeopleProperty("last_time_viewed_blog_preview");
                break;
            case READER_TAG_PREVIEW:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Reader - Tag Preview");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_viewed_tag_preview");
                instructions.setCurrentDateForPeopleProperty("last_time_viewed_tag_preview");
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
            case EDITOR_CLOSED_POST:
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
            case EDITOR_PUBLISHED_POST_WITH_PHOTO:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_posts_published_with_photos");
                instructions.setCurrentDateForPeopleProperty("last_time_published_post_with_photo");
                break;
            case EDITOR_PUBLISHED_POST_WITH_VIDEO:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_posts_published_with_videos");
                instructions.setCurrentDateForPeopleProperty("last_time_published_post_with_video");
              break;
            case EDITOR_PUBLISHED_POST_WITH_CATEGORIES:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                                "number_of_posts_published_with_categories");
                instructions.setCurrentDateForPeopleProperty("last_time_published_post_with_categories");
                break;
            case EDITOR_PUBLISHED_POST_WITH_TAGS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                      mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(
                              "number_of_posts_published_with_tags");
                instructions.setCurrentDateForPeopleProperty("last_time_published_post_with_tags");
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
            case OPENED_SETTINGS:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Site Menu - Opened Settings");
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
            case STATS_OPENED_WEB_VERSION:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Stats - Opened Web Version");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_web_version_of_stats");
                instructions.setCurrentDateForPeopleProperty("last_time_accessed_web_version_of_stats");
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
                        mixpanelInstructionsForEventName("Selected Install Jetpack");
                break;
            case PUSH_NOTIFICATION_RECEIVED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Push Notification Received");
                break;
            case SUPPORT_OPENED_HELPSHIFT_SCREEN:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Support - Opened Helpshift Screen");
                instructions.addSuperPropertyToFlag("opened_helpshift_screen");
                break;
            case LOGIN_FAILED:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Failed Login");
                break;
            case LOGIN_FAILED_TO_GUESS_XMLRPC:
                instructions = AnalyticsTrackerMixpanelInstructionsForStat.
                        mixpanelInstructionsForEventName("Login - Failed To Guess XMLRPC");
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

    private void incrementSuperProperty(String property) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
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
            Object valueForProperty = properties.get(property);
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

    private void incrementSessionCount() {
        // Tracking session count will help us isolate users who just installed the app
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        int sessionCount = preferences.getInt(SESSION_COUNT, 0);
        sessionCount++;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SESSION_COUNT, sessionCount);
        editor.commit();
    }
}

