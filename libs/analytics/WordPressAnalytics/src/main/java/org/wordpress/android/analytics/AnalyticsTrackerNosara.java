package org.wordpress.android.analytics;

import android.content.Context;

import com.automattic.android.tracks.TracksClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsTrackerNosara extends Tracker {

    private static final String JETPACK_USER = "jetpack_user";
    private static final String NUMBER_OF_BLOGS = "number_of_blogs";
    private static final String TRACKS_ANON_ID = "nosara_tracks_anon_id";

    private static final String EVENTS_PREFIX = "wpandroid_";

    private TracksClient mNosaraClient;

    public AnalyticsTrackerNosara(Context context) throws IllegalArgumentException {
        super(context);
        mNosaraClient = TracksClient.getClient(context);
    }

    String getAnonIdPrefKey() {
        return TRACKS_ANON_ID;
    }

    @Override
    public void track(AnalyticsTracker.Stat stat) {
        track(stat, null);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, Map<String, ?> properties) {
        if (mNosaraClient == null) {
            return;
        }

        if (!isValidEvent(stat)) {
            return;
        }

        String eventName = getEventNameForStat(stat);
        if (eventName == null) {
            AppLog.w(AppLog.T.STATS, "There is NO match for the event " + stat.name() + "stat");
            return;
        }

        Map<String, Object> predefinedEventProperties = new HashMap<String, Object>();
        switch (stat) {
            case EDITOR_ADDED_PHOTO_NEW:
            case EDITOR_ADDED_VIDEO_NEW:
                predefinedEventProperties.put("via", "device_camera");
                break;
            case EDITOR_ADDED_PHOTO_VIA_DEVICE_LIBRARY:
            case EDITOR_ADDED_VIDEO_VIA_DEVICE_LIBRARY:
                predefinedEventProperties.put("via", "device_library");
                break;
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
            case EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY:
                predefinedEventProperties.put("via", "media_library");
                break;
            case EDITOR_TAPPED_BLOCKQUOTE:
                predefinedEventProperties.put("button", "blockquote");
                break;
            case EDITOR_TAPPED_BOLD:
                predefinedEventProperties.put("button", "bold");
                break;
            case EDITOR_TAPPED_IMAGE:
                predefinedEventProperties.put("button", "image");
                break;
            case EDITOR_TAPPED_ITALIC:
                predefinedEventProperties.put("button", "italic");
                break;
            case EDITOR_TAPPED_LINK:
                predefinedEventProperties.put("button", "link");
                break;
            case EDITOR_TAPPED_MORE:
                predefinedEventProperties.put("button", "more");
                break;
            case EDITOR_TAPPED_STRIKETHROUGH:
                predefinedEventProperties.put("button", "strikethrough");
                break;
            case EDITOR_TAPPED_UNDERLINE:
                predefinedEventProperties.put("button", "underline");
                break;
            case EDITOR_TAPPED_HTML:
                predefinedEventProperties.put("button", "html");
                break;
            case EDITOR_TAPPED_ORDERED_LIST:
                predefinedEventProperties.put("button", "ordered_list");
                break;
            case EDITOR_TAPPED_UNLINK:
                predefinedEventProperties.put("button", "unlink");
                break;
            case EDITOR_TAPPED_UNORDERED_LIST:
                predefinedEventProperties.put("button", "unordered_list");
                break;
            case OPENED_POSTS:
                predefinedEventProperties.put("menu_item", "posts");
                break;
            case OPENED_PAGES:
                predefinedEventProperties.put("menu_item", "pages");
                break;
            case OPENED_COMMENTS:
                predefinedEventProperties.put("menu_item", "comments");
                break;
            case OPENED_VIEW_SITE:
                predefinedEventProperties.put("menu_item", "view_site");
                break;
            case OPENED_VIEW_SITE_FROM_HEADER:
                predefinedEventProperties.put("menu_item", "view_site_from_header");
                break;
            case OPENED_VIEW_ADMIN:
                predefinedEventProperties.put("menu_item", "view_admin");
                break;
            case OPENED_MEDIA_LIBRARY:
                predefinedEventProperties.put("menu_item", "media_library");
                break;
            case OPENED_BLOG_SETTINGS:
                predefinedEventProperties.put("menu_item", "site_settings");
                break;
            case STATS_PERIOD_DAYS_ACCESSED:
                predefinedEventProperties.put("period", "days");
                break;
            case STATS_PERIOD_WEEKS_ACCESSED:
                predefinedEventProperties.put("period", "weeks");
                break;
            case STATS_PERIOD_MONTHS_ACCESSED:
                predefinedEventProperties.put("period", "months");
                break;
            case STATS_PERIOD_YEARS_ACCESSED:
                predefinedEventProperties.put("period", "years");
                break;
            case NOTIFICATION_QUICK_ACTIONS_LIKED:
            case NOTIFICATION_QUICK_ACTIONS_REPLIED_TO:
            case NOTIFICATION_QUICK_ACTIONS_APPROVED:
                predefinedEventProperties.put("is_quick_action", true);
                break;
        }

        final String user;
        final TracksClient.NosaraUserType userType;
        if (getWordPressComUserName() != null) {
            user = getWordPressComUserName();
            userType = TracksClient.NosaraUserType.WPCOM;
        } else {
            // This is just a security checks since the anonID is already available here.
            // refresh metadata is called on login/logout/startup and it loads/generates the anonId when necessary.
            if (getAnonID() == null) {
                user = generateNewAnonID();
            } else {
                user = getAnonID();
            }
            userType = TracksClient.NosaraUserType.ANON;
        }

        // It seems that we're tracking some events with user = null. Make sure we're catching the error here.
        if (user == null) {
            try {
                throw new AnalyticsException("Trying to track analytics with an null user!");
                // TODO add CrashlyticsUtils.logException or track this error in Nosara by using a special test user.
            } catch (AnalyticsException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
            return;
        }

        // create the merged JSON Object of properties
        // Properties defined by the user have precedence over the default ones pre-defined at "event level"
        JSONObject propertiesToJSON = null;
        if (properties != null && properties.size() > 0) {
            try {
                propertiesToJSON = new JSONObject(properties);
                for (String key : predefinedEventProperties.keySet()) {
                    try {
                        if (propertiesToJSON.has(key)) {
                            AppLog.w(AppLog.T.STATS, "The user has defined a property named: '" + key + "' that will override" +
                                    "the same property pre-defined at event level. This may generate unexpected behavior!!");
                            AppLog.w(AppLog.T.STATS, "User value: " + propertiesToJSON.get(key).toString() + " - pre-defined value: " +
                                    predefinedEventProperties.get(key).toString());
                        } else {
                            propertiesToJSON.put(key, predefinedEventProperties.get(key));
                        }
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.STATS, "Error while merging user-defined properties with pre-defined properties", e);
                    }
                }
            } catch (NullPointerException e) {
                AppLog.e(AppLog.T.STATS, "A property passed to the event " + eventName + " has null key!", e);
            }
        }

        if (propertiesToJSON == null) {
            propertiesToJSON = new JSONObject(predefinedEventProperties);
        }

        if (propertiesToJSON.length() > 0) {
            mNosaraClient.track(EVENTS_PREFIX + eventName, propertiesToJSON, user, userType);
        } else {
            mNosaraClient.track(EVENTS_PREFIX + eventName, user, userType);
        }
    }

    @Override
    public void endSession() {
        this.flush();
    }

    @Override
    public void flush() {
        if (mNosaraClient == null) {
            return;
        }
        mNosaraClient.flush();
    }

    @Override
    public void refreshMetadata(AnalyticsMetadata metadata) {
        if (mNosaraClient == null) {
            return;
        }

        try {
            JSONObject properties = new JSONObject();
            properties.put(JETPACK_USER, metadata.isJetpackUser());
            properties.put(NUMBER_OF_BLOGS, metadata.getNumBlogs());
            mNosaraClient.registerUserProperties(properties);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        if (metadata.isUserConnected() && metadata.isWordPressComUser()) {
            setWordPressComUserName(metadata.getUsername());
            // Re-unify the user
            if (getAnonID() != null) {
                mNosaraClient.trackAliasUser(getWordPressComUserName(), getAnonID(), TracksClient.NosaraUserType.WPCOM);
                clearAnonID();
            }
        } else {
            // Not wpcom connected. Check if anonID is already present
            setWordPressComUserName(null);
            if (getAnonID() == null) {
                generateNewAnonID();
            }
        }


    }


    @Override
    public void clearAllData() {
        super.clearAllData();
        if (mNosaraClient == null) {
            return;
        }
        mNosaraClient.clearUserProperties();
    }

    @Override
    public void registerPushNotificationToken(String regId) {
        return;
    }

    public static String getEventNameForStat(AnalyticsTracker.Stat stat) {
        if (!isValidEvent(stat)) {
            return null;
        }

        switch (stat) {
            case APPLICATION_OPENED:
                return "application_opened";
            case APPLICATION_CLOSED:
                return "application_closed";
            case APPLICATION_INSTALLED:
                return "application_installed";
            case APPLICATION_UPGRADED:
                return "application_upgraded";
            case READER_ACCESSED:
                return "reader_accessed";
            case READER_ARTICLE_COMMENTED_ON:
                return "reader_article_commented_on";
            case READER_ARTICLE_COMMENTS_OPENED:
                return "reader_article_comments_opened";
            case READER_ARTICLE_COMMENT_LIKED:
                return "reader_article_comment_liked";
            case READER_ARTICLE_COMMENT_UNLIKED:
                return "reader_article_comment_unliked";
            case READER_ARTICLE_LIKED:
                return "reader_article_liked";
            case READER_ARTICLE_OPENED:
                return "reader_article_opened";
            case READER_ARTICLE_UNLIKED:
                return "reader_article_unliked";
            case READER_ARTICLE_RENDERED :
                return "reader_article_rendered";
            case READER_BLOG_BLOCKED:
                return "reader_blog_blocked";
            case READER_BLOG_FOLLOWED:
                return "reader_site_followed";
            case READER_BLOG_PREVIEWED:
                return "reader_blog_previewed";
            case READER_BLOG_UNFOLLOWED:
                return "reader_site_unfollowed";
            case READER_DISCOVER_VIEWED:
                return "reader_discover_viewed";
            case READER_INFINITE_SCROLL:
                return "reader_infinite_scroll_performed";
            case READER_LIST_FOLLOWED:
                return "reader_list_followed";
            case READER_LIST_LOADED:
                return "reader_list_loaded";
            case READER_LIST_PREVIEWED:
                return "reader_list_previewed";
            case READER_LIST_UNFOLLOWED:
                return "reader_list_unfollowed";
            case READER_TAG_FOLLOWED:
                return "reader_reader_tag_followed";
            case READER_TAG_LOADED:
                return "reader_tag_loaded";
            case READER_TAG_PREVIEWED:
                return "reader_tag_previewed";
            case READER_SEARCH_LOADED:
                return "reader_search_loaded";
            case READER_SEARCH_PERFORMED:
                return "reader_search_performed";
            case READER_SEARCH_RESULT_TAPPED:
                return "reader_searchcard_clicked";
            case READER_TAG_UNFOLLOWED:
                return "reader_reader_tag_unfollowed";
            case READER_GLOBAL_RELATED_POST_CLICKED:
                return "reader_related_post_from_other_site_clicked";
            case READER_LOCAL_RELATED_POST_CLICKED:
                return "reader_related_post_from_same_site_clicked";
            case READER_VIEWPOST_INTERCEPTED:
                return "reader_viewpost_intercepted";
            case READER_BLOG_POST_INTERCEPTED:
                return "reader_blog_post_intercepted";
            case READER_FEED_POST_INTERCEPTED:
                return "reader_feed_post_intercepted";
            case READER_WPCOM_BLOG_POST_INTERCEPTED:
                return "reader_wpcom_blog_post_intercepted";
            case READER_SIGN_IN_INITIATED:
                return "reader_sign_in_initiated";
            case READER_WPCOM_SIGN_IN_NEEDED:
                return "reader_wpcom_sign_in_needed";
            case READER_USER_UNAUTHORIZED:
                return "reader_user_unauthorized";
            case EDITOR_CREATED_POST:
                return "editor_post_created";
            case EDITOR_SAVED_DRAFT:
                return "editor_draft_saved";
            case EDITOR_DISCARDED_CHANGES:
                return "editor_discarded_changes";
            case EDITOR_EDITED_IMAGE:
                return "editor_image_edited";
            case EDITOR_HYBRID_ENABLED:
                return "editor_hybrid_enabled";
            case EDITOR_HYBRID_TOGGLED_OFF:
                return "editor_hybrid_toggled_off";
            case EDITOR_HYBRID_TOGGLED_ON:
                return "editor_hybrid_toggled_on";
            case EDITOR_AZTEC_ENABLED:
                return "editor_aztec_enabled";
            case EDITOR_AZTEC_TOGGLED_OFF:
                return "editor_aztec_toggled_off";
            case EDITOR_AZTEC_TOGGLED_ON:
                return "editor_aztec_toggled_on";
            case EDITOR_UPLOAD_MEDIA_FAILED:
                return "editor_upload_media_failed";
            case EDITOR_UPLOAD_MEDIA_RETRIED:
                return "editor_upload_media_retried";
            case EDITOR_CLOSED:
                return "editor_closed";
            case EDITOR_ADDED_PHOTO_NEW:
                return "editor_photo_added";
            case EDITOR_ADDED_PHOTO_VIA_DEVICE_LIBRARY:
                return "editor_photo_added";
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
                return "editor_photo_added";
            case EDITOR_ADDED_VIDEO_NEW:
                return "editor_video_added";
            case EDITOR_ADDED_VIDEO_VIA_DEVICE_LIBRARY:
                return "editor_video_added";
            case EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY:
                return "editor_video_added";
            case MEDIA_PHOTO_OPTIMIZED:
                return "media_photo_optimized";
            case MEDIA_PHOTO_OPTIMIZE_ERROR:
                return "media_photo_optimize_error";
            case MEDIA_VIDEO_OPTIMIZED:
                return "media_video_optimized";
            case MEDIA_VIDEO_OPTIMIZE_ERROR:
                return "media_video_optimize_error";
            case MEDIA_VIDEO_CANT_OPTIMIZE:
                return "media_video_cant_optimize";
            case EDITOR_PUBLISHED_POST:
                return "editor_post_published";
            case EDITOR_UPDATED_POST:
                return "editor_post_updated";
            case EDITOR_SCHEDULED_POST:
                return "editor_post_scheduled";
            case EDITOR_TAPPED_BLOCKQUOTE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_BOLD:
                return "editor_button_tapped";
            case EDITOR_TAPPED_IMAGE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ITALIC:
                return "editor_button_tapped";
            case EDITOR_TAPPED_LINK:
                return "editor_button_tapped";
            case EDITOR_TAPPED_MORE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_STRIKETHROUGH:
                return "editor_button_tapped";
            case EDITOR_TAPPED_UNDERLINE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HTML:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ORDERED_LIST:
                return "editor_button_tapped";
            case EDITOR_TAPPED_UNLINK:
                return "editor_button_tapped";
            case EDITOR_TAPPED_UNORDERED_LIST:
                return "editor_button_tapped";
            case NOTIFICATIONS_ACCESSED:
                return "notifications_accessed";
            case NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS:
                return "notifications_notification_details_opened";
            case NOTIFICATION_APPROVED:
            case NOTIFICATION_QUICK_ACTIONS_APPROVED:
                return "notifications_approved";
            case NOTIFICATION_UNAPPROVED:
                return "notifications_unapproved";
            case NOTIFICATION_REPLIED_TO:
            case NOTIFICATION_QUICK_ACTIONS_REPLIED_TO:
                return "notifications_replied_to";
            case NOTIFICATION_TRASHED:
                return "notifications_trashed";
            case NOTIFICATION_FLAGGED_AS_SPAM:
                return "notifications_flagged_as_spam";
            case NOTIFICATION_SWIPE_PAGE_CHANGED:
                return "notifications_swipe_page_changed";
            case NOTIFICATION_PENDING_DRAFTS_TAPPED:
                return "notifications_pending_drafts_tapped";
            case NOTIFICATION_PENDING_DRAFTS_IGNORED:
                return "notifications_pending_drafts_ignored";
            case NOTIFICATION_PENDING_DRAFTS_DISMISSED:
                return "notifications_pending_drafts_dismissed";
            case NOTIFICATION_PENDING_DRAFTS_SETTINGS_ENABLED:
                return "notifications_pending_drafts_settings_enabled";
            case NOTIFICATION_PENDING_DRAFTS_SETTINGS_DISABLED:
                return "notifications_pending_drafts_settings_disabled";
            case NOTIFICATION_LIKED:
            case NOTIFICATION_QUICK_ACTIONS_LIKED:
                return "notifications_comment_liked";
            case NOTIFICATION_UNLIKED:
                return "notifications_comment_unliked";
            case OPENED_POSTS:
                return "site_menu_opened";
            case OPENED_PAGES:
                return "site_menu_opened";
            case OPENED_COMMENTS:
                return "site_menu_opened";
            case OPENED_VIEW_SITE:
                return "site_menu_opened";
            case OPENED_VIEW_SITE_FROM_HEADER:
                return "site_menu_opened";
            case OPENED_VIEW_ADMIN:
                return "site_menu_opened";
            case OPENED_MEDIA_LIBRARY:
                return "site_menu_opened";
            case OPENED_BLOG_SETTINGS:
                return "site_menu_opened";
            case OPENED_ACCOUNT_SETTINGS:
                return "account_settings_opened";
            case OPENED_APP_SETTINGS:
                return "app_settings_opened";
            case OPENED_MY_PROFILE:
                return "my_profile_opened";
            case OPENED_PEOPLE_MANAGEMENT:
                return "people_management_list_opened";
            case OPENED_PERSON:
                return "people_management_details_opened";
            case CREATE_ACCOUNT_INITIATED:
                return "account_create_initiated";
            case CREATE_ACCOUNT_EMAIL_EXISTS:
                return "account_create_email_exists";
            case CREATE_ACCOUNT_USERNAME_EXISTS:
                return "account_create_username_exists";
            case CREATE_ACCOUNT_FAILED:
                return "account_create_failed";
            case CREATED_ACCOUNT:
                return "account_created";
            case CREATED_SITE:
                return "site_created";
            case SHARED_ITEM:
                return "item_shared";
            case ADDED_SELF_HOSTED_SITE:
                return "self_hosted_blog_added";
            case SIGNED_IN:
                return "signed_in";
            case SIGNED_INTO_JETPACK:
                return "signed_into_jetpack";
            case ACCOUNT_LOGOUT:
                return "account_logout";
            case PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN:
                return "stats_screen_signed_into_jetpack";
            case STATS_ACCESSED:
                return "stats_accessed";
            case STATS_INSIGHTS_ACCESSED:
                return "stats_insights_accessed";
            case STATS_PERIOD_DAYS_ACCESSED:
                return "stats_period_accessed";
            case STATS_PERIOD_WEEKS_ACCESSED:
                return "stats_period_accessed";
            case STATS_PERIOD_MONTHS_ACCESSED:
                return "stats_period_accessed";
            case STATS_PERIOD_YEARS_ACCESSED:
                return "stats_period_accessed";
            case STATS_VIEW_ALL_ACCESSED:
                return "stats_view_all_accessed";
            case STATS_SINGLE_POST_ACCESSED:
                return "stats_single_post_accessed";
            case STATS_TAPPED_BAR_CHART:
                return "stats_bar_chart_tapped";
            case STATS_SCROLLED_TO_BOTTOM:
                return "stats_scrolled_to_bottom";
            case STATS_SELECTED_INSTALL_JETPACK:
                return "stats_install_jetpack_selected";
            case STATS_SELECTED_CONNECT_JETPACK:
                return "stats_connect_jetpack_selected";
            case STATS_WIDGET_ADDED:
                return "stats_widget_added";
            case STATS_WIDGET_REMOVED:
                return "stats_widget_removed";
            case STATS_WIDGET_TAPPED:
                return "stats_widget_tapped";
            case PUSH_NOTIFICATION_RECEIVED:
                return "push_notification_received";
            case PUSH_NOTIFICATION_TAPPED:
                return "push_notification_alert_tapped";
            case SUPPORT_OPENED_HELPSHIFT_SCREEN:
                return "support_helpshift_screen_opened";
            case SUPPORT_USER_ACCEPTED_THE_SOLUTION:
                return "support_user_accepted_the_solution";
            case SUPPORT_USER_REJECTED_THE_SOLUTION:
                return "support_user_rejected_the_solution";
            case SUPPORT_USER_SENT_SCREENSHOT:
                return "support_user_sent_screenshot";
            case SUPPORT_USER_REVIEWED_THE_APP:
                return "support_user_reviewed_the_app";
            case SUPPORT_USER_REPLIED_TO_HELPSHIFT:
                return "support_user_replied_to_helpshift";
            case LOGIN_ACCESSED:
                return "login_accessed";
            case LOGIN_MAGIC_LINK_EXITED:
                return "login_magic_link_exited";
            case LOGIN_MAGIC_LINK_FAILED:
                return "login_magic_link_failed";
            case LOGIN_MAGIC_LINK_OPENED:
                return "login_magic_link_opened";
            case LOGIN_MAGIC_LINK_REQUESTED:
                return "login_magic_link_requested";
            case LOGIN_MAGIC_LINK_SUCCEEDED:
                return "login_magic_link_succeeded";
            case LOGIN_FAILED:
                return "login_failed_to_login";
            case LOGIN_FAILED_TO_GUESS_XMLRPC:
                return "login_failed_to_guess_xmlrpc";
            case LOGIN_INSERTED_INVALID_URL:
                return "login_inserted_invalid_url";
            case LOGIN_AUTOFILL_CREDENTIALS_FILLED:
                return "login_autofill_credentials_filled";
            case LOGIN_AUTOFILL_CREDENTIALS_UPDATED:
                return "login_autofill_credentials_updated";
            case PERSON_REMOVED:
                return "people_management_person_removed";
            case PERSON_UPDATED:
                return "people_management_person_updated";
            case PUSH_AUTHENTICATION_APPROVED:
                return "push_authentication_approved";
            case PUSH_AUTHENTICATION_EXPIRED:
                return "push_authentication_expired";
            case PUSH_AUTHENTICATION_FAILED:
                return "push_authentication_failed";
            case PUSH_AUTHENTICATION_IGNORED:
                return "push_authentication_ignored";
            case NOTIFICATION_SETTINGS_LIST_OPENED:
                return "notification_settings_list_opened";
            case NOTIFICATION_SETTINGS_STREAMS_OPENED:
                return "notification_settings_streams_opened";
            case NOTIFICATION_SETTINGS_DETAILS_OPENED:
                return "notification_settings_details_opened";
            case ME_ACCESSED:
                return "me_tab_accessed";
            case ME_GRAVATAR_TAPPED:
                return "me_gravatar_tapped";
            case ME_GRAVATAR_TOOLTIP_TAPPED:
                return "me_gravatar_tooltip_tapped";
            case ME_GRAVATAR_SHOT_NEW:
                return "me_gravatar_shot_new";
            case ME_GRAVATAR_GALLERY_PICKED:
                return "me_gravatar_gallery_picked";
            case ME_GRAVATAR_CROPPED:
                return "me_gravatar_cropped";
            case ME_GRAVATAR_UPLOADED:
                return "me_gravatar_uploaded";
            case ME_GRAVATAR_UPLOAD_UNSUCCESSFUL:
                return "me_gravatar_upload_unsuccessful";
            case ME_GRAVATAR_UPLOAD_EXCEPTION:
                return "me_gravatar_upload_exception";
            case MY_SITE_ACCESSED:
                return "my_site_tab_accessed";
            case THEMES_ACCESSED_THEMES_BROWSER:
                return "themes_theme_browser_accessed";
            case THEMES_ACCESSED_SEARCH:
                return "themes_search_accessed";
            case THEMES_CHANGED_THEME:
                return "themes_theme_changed";
            case THEMES_PREVIEWED_SITE:
                return "themes_theme_for_site_previewed";
            case THEMES_DEMO_ACCESSED:
                return "themes_demo_accessed";
            case THEMES_CUSTOMIZE_ACCESSED:
                return "themes_customize_accessed";
            case THEMES_SUPPORT_ACCESSED:
                return "themes_support_accessed";
            case THEMES_DETAILS_ACCESSED:
                return "themes_details_accessed";
            case ACCOUNT_SETTINGS_LANGUAGE_CHANGED:
                return "account_settings_language_changed";
            case SITE_SETTINGS_ACCESSED:
                return "site_settings_accessed";
            case SITE_SETTINGS_ACCESSED_MORE_SETTINGS:
                return "site_settings_more_settings_accessed";
            case SITE_SETTINGS_ADDED_LIST_ITEM:
                return "site_settings_added_list_item";
            case SITE_SETTINGS_DELETED_LIST_ITEMS:
                return "site_settings_deleted_list_items";
            case SITE_SETTINGS_HINT_TOAST_SHOWN:
                return "site_settings_hint_toast_shown";
            case SITE_SETTINGS_LEARN_MORE_CLICKED:
                return "site_settings_learn_more_clicked";
            case SITE_SETTINGS_LEARN_MORE_LOADED:
                return "site_settings_learn_more_loaded";
            case SITE_SETTINGS_SAVED_REMOTELY:
                return "site_settings_saved_remotely";
            case SITE_SETTINGS_START_OVER_ACCESSED:
                return "site_settings_start_over_accessed";
            case SITE_SETTINGS_START_OVER_CONTACT_SUPPORT_CLICKED:
                return "site_settings_start_over_contact_support_clicked";
            case SITE_SETTINGS_EXPORT_SITE_ACCESSED:
                return "site_settings_export_site_accessed";
            case SITE_SETTINGS_EXPORT_SITE_REQUESTED:
                return "site_settings_export_site_requested";
            case SITE_SETTINGS_EXPORT_SITE_RESPONSE_OK:
                return "site_settings_export_site_response_ok";
            case SITE_SETTINGS_EXPORT_SITE_RESPONSE_ERROR:
                return "site_settings_export_site_response_error";
            case SITE_SETTINGS_DELETE_SITE_ACCESSED:
                return "site_settings_delete_site_accessed";
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_REQUESTED:
                return "site_settings_delete_site_purchases_requested";
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOWN:
                return "site_settings_delete_site_purchases_shown";
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOW_CLICKED:
                return "site_settings_delete_site_purchases_show_clicked";
            case SITE_SETTINGS_DELETE_SITE_REQUESTED:
                return "site_settings_delete_site_requested";
            case SITE_SETTINGS_DELETE_SITE_RESPONSE_OK:
                return "site_settings_delete_site_response_ok";
            case SITE_SETTINGS_DELETE_SITE_RESPONSE_ERROR:
                return "site_settings_delete_site_response_error";
            case SITE_SETTINGS_OPTIMIZE_IMAGES_CHANGED:
                return "site_settings_optimize_images_changed";
            case ABTEST_START:
                return "abtest_start";
            case TRAIN_TRACKS_RENDER:
                return "traintracks_render";
            case TRAIN_TRACKS_INTERACT:
                return "traintracks_interact";
            case DEEP_LINKED:
                return "deep_linked";
            case DEEP_LINKED_FALLBACK:
                return "deep_linked_fallback";
            case DEEP_LINK_NOT_DEFAULT_HANDLER:
                return "deep_link_not_default_handler";
            case MEDIA_LIBRARY_ADDED_PHOTO:
                return "media_library_photo_added";
            case MEDIA_LIBRARY_ADDED_VIDEO:
                return "media_library_video_added";
            case MEDIA_UPLOAD_STARTED:
                return "media_service_upload_started";
            case MEDIA_UPLOAD_ERROR:
                return "media_service_upload_response_error";
            case MEDIA_UPLOAD_SUCCESS:
                return "media_service_upload_response_ok";
            case MEDIA_UPLOAD_CANCELED:
                return "media_service_upload_canceled";
            case MEDIA_PICKER_OPEN_CAPTURE_MEDIA:
                return "media_picker_capture_media_opened";
            case MEDIA_PICKER_OPEN_DEVICE_LIBRARY:
                return "media_picker_device_library_opened";
            case MEDIA_PICKER_OPEN_WP_MEDIA:
                return "media_picker_wordpress_library_opened";
            case MEDIA_PICKER_RECENT_MEDIA_SELECTED:
                return "media_picker_recent_media_selected";
            case MEDIA_PICKER_PREVIEW_OPENED:
                return "media_picker_preview_opened";
            case APP_PERMISSION_GRANTED:
                return "app_permission_granted";
            case APP_PERMISSION_DENIED:
                return "app_permission_denied";
            default:
                return null;
        }
    }
}
