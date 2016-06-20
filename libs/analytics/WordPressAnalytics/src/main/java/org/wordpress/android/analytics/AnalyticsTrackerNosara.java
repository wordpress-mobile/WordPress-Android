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

        Map<String, Object> predefinedEventProperties = new HashMap<String, Object>();
        String eventName;

        switch (stat) {
            case APPLICATION_OPENED:
                eventName = "application_opened";
                break;
            case APPLICATION_CLOSED:
                eventName = "application_closed";
                break;
            case APPLICATION_INSTALLED:
                eventName = "application_installed";
                break;
            case APPLICATION_UPGRADED:
                eventName = "application_upgraded";
                break;
            case READER_ACCESSED:
                eventName = "reader_accessed";
                break;
            case READER_ARTICLE_COMMENTED_ON:
                eventName = "reader_article_commented_on";
                break;
            case READER_ARTICLE_LIKED:
                eventName = "reader_article_liked";
                break;
            case READER_ARTICLE_OPENED:
                eventName = "reader_article_opened";
                break;
            case READER_ARTICLE_UNLIKED:
                eventName = "reader_article_unliked";
                break;
            case READER_BLOG_BLOCKED:
                eventName = "reader_blog_blocked";
                break;
            case READER_BLOG_FOLLOWED:
                eventName = "reader_site_followed";
                break;
            case READER_BLOG_PREVIEWED:
                eventName = "reader_blog_previewed";
                break;
            case READER_BLOG_UNFOLLOWED:
                eventName = "reader_site_unfollowed";
                break;
            case READER_DISCOVER_VIEWED:
                eventName = "reader_discover_viewed";
                break;
            case READER_INFINITE_SCROLL:
                eventName = "reader_infinite_scroll_performed";
                break;
            case READER_LIST_FOLLOWED:
                eventName = "reader_list_followed";
                break;
            case READER_LIST_LOADED:
                eventName = "reader_list_loaded";
                break;
            case READER_LIST_PREVIEWED:
                eventName = "reader_list_previewed";
                break;
            case READER_LIST_UNFOLLOWED:
                eventName = "reader_list_unfollowed";
                break;
            case READER_TAG_FOLLOWED:
                eventName = "reader_reader_tag_followed";
                break;
            case READER_TAG_LOADED:
                eventName = "reader_tag_loaded";
                break;
            case READER_TAG_PREVIEWED:
                eventName = "reader_tag_previewed";
                break;
            case READER_SEARCH_LOADED:
                eventName = "reader_search_loaded";
                break;
            case READER_TAG_UNFOLLOWED:
                eventName = "reader_reader_tag_unfollowed";
                break;
            case EDITOR_CREATED_POST:
                eventName = "editor_post_created";
                break;
            case EDITOR_SAVED_DRAFT:
                eventName = "editor_draft_saved";
                break;
            case EDITOR_DISCARDED_CHANGES:
                eventName = "editor_discarded_changes";
                break;
            case EDITOR_EDITED_IMAGE:
                eventName = "editor_image_edited";
                break;
            case EDITOR_ENABLED_NEW_VERSION:
                eventName = "editor_enabled_new_version";
                break;
            case EDITOR_TOGGLED_OFF:
                eventName = "editor_toggled_off";
                break;
            case EDITOR_TOGGLED_ON:
                eventName = "editor_toggled_on";
                break;
            case EDITOR_UPLOAD_MEDIA_FAILED:
                eventName = "editor_upload_media_failed";
                break;
            case EDITOR_UPLOAD_MEDIA_RETRIED:
                eventName = "editor_upload_media_retried";
                break;
            case EDITOR_CLOSED:
                eventName = "editor_closed";
                break;
            case EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY:
                eventName = "editor_photo_added";
                predefinedEventProperties.put("via", "local_library");
                break;
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
                eventName = "editor_photo_added";
                predefinedEventProperties.put("via", "media_library");
                break;
            case EDITOR_ADDED_VIDEO_VIA_LOCAL_LIBRARY:
                eventName = "editor_video_added";
                predefinedEventProperties.put("via", "local_library");
                break;
            case EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY:
                eventName = "editor_video_added";
                predefinedEventProperties.put("via", "media_library");
                break;
            case EDITOR_PUBLISHED_POST:
                eventName = "editor_post_published";
                break;
            case EDITOR_UPDATED_POST:
                eventName = "editor_post_updated";
                break;
            case EDITOR_SCHEDULED_POST:
                eventName = "editor_post_scheduled";
                break;
            case EDITOR_TAPPED_BLOCKQUOTE:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "blockquote");
                break;
            case EDITOR_TAPPED_BOLD:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "bold");
                break;
            case EDITOR_TAPPED_IMAGE:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "image");
                break;
            case EDITOR_TAPPED_ITALIC:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "italic");
                break;
            case EDITOR_TAPPED_LINK:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "link");
                break;
            case EDITOR_TAPPED_MORE:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "more");
                break;
            case EDITOR_TAPPED_STRIKETHROUGH:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "strikethrough");
                break;
            case EDITOR_TAPPED_UNDERLINE:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "underline");
                break;
            case EDITOR_TAPPED_HTML:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "html");
                break;
            case EDITOR_TAPPED_ORDERED_LIST:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "ordered_list");
                break;
            case EDITOR_TAPPED_UNLINK:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "unlink");
                break;
            case EDITOR_TAPPED_UNORDERED_LIST:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "unordered_list");
                break;
            case NOTIFICATIONS_ACCESSED:
                eventName = "notifications_accessed";
                break;
            case NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS:
                eventName = "notifications_notification_details_opened";
                break;
            case NOTIFICATION_APPROVED:
                eventName = "notifications_approved";
                break;
            case NOTIFICATION_UNAPPROVED:
                eventName = "notifications_unapproved";
                break;
            case NOTIFICATION_REPLIED_TO:
                eventName = "notifications_replied_to";
                break;
            case NOTIFICATION_TRASHED:
                eventName = "notifications_trashed";
                break;
            case NOTIFICATION_FLAGGED_AS_SPAM:
                eventName = "notifications_flagged_as_spam";
                break;
            case NOTIFICATION_LIKED:
                eventName = "notifications_comment_liked";
                break;
            case NOTIFICATION_UNLIKED:
                eventName = "notifications_comment_unliked";
                break;
            case OPENED_POSTS:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "posts");
                break;
            case OPENED_PAGES:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "pages");
                break;
            case OPENED_COMMENTS:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "comments");
                break;
            case OPENED_VIEW_SITE:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "view_site");
                break;
            case OPENED_VIEW_ADMIN:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "view_admin");
                break;
            case OPENED_MEDIA_LIBRARY:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "media_library");
                break;
            case OPENED_BLOG_SETTINGS:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "site_settings");
                break;
            case OPENED_ACCOUNT_SETTINGS:
                eventName = "account_settings_opened";
                break;
            case OPENED_APP_SETTINGS:
                eventName = "app_settings_opened";
                break;
            case OPENED_MY_PROFILE:
                eventName = "my_profile_opened";
                break;
            case OPENED_PEOPLE_MANAGEMENT:
                eventName = "people_management_list_opened";
                break;
            case OPENED_PERSON:
                eventName = "people_management_details_opened";
                break;
            case CREATED_ACCOUNT:
                eventName = "account_created";
                break;
            case SHARED_ITEM:
                eventName = "item_shared";
                break;
            case ADDED_SELF_HOSTED_SITE:
                eventName = "self_hosted_blog_added";
                break;
            case SIGNED_IN:
                eventName = "signed_in";
                break;
            case SIGNED_INTO_JETPACK:
                eventName = "signed_into_jetpack";
                break;
            case ACCOUNT_LOGOUT:
                eventName = "account_logout";
                break;
            case PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN:
                eventName = "stats_screen_signed_into_jetpack";
                break;
            case STATS_ACCESSED:
                eventName = "stats_accessed";
                break;
            case STATS_INSIGHTS_ACCESSED:
                eventName = "stats_insights_accessed";
                break;
            case STATS_PERIOD_DAYS_ACCESSED:
                eventName = "stats_period_accessed";
                predefinedEventProperties.put("period", "days");
                break;
            case STATS_PERIOD_WEEKS_ACCESSED:
                eventName = "stats_period_accessed";
                predefinedEventProperties.put("period", "weeks");
                break;
            case STATS_PERIOD_MONTHS_ACCESSED:
                eventName = "stats_period_accessed";
                predefinedEventProperties.put("period", "months");
                break;
            case STATS_PERIOD_YEARS_ACCESSED:
                eventName = "stats_period_accessed";
                predefinedEventProperties.put("period", "years");
                break;
            case STATS_VIEW_ALL_ACCESSED:
                eventName = "stats_view_all_accessed";
                break;
            case STATS_SINGLE_POST_ACCESSED:
                eventName = "stats_single_post_accessed";
                break;
            case STATS_TAPPED_BAR_CHART:
                eventName = "stats_bar_chart_tapped";
                break;
            case STATS_SCROLLED_TO_BOTTOM:
                eventName = "stats_scrolled_to_bottom";
                break;
            case STATS_SELECTED_INSTALL_JETPACK:
                eventName = "stats_install_jetpack_selected";
                break;
            case STATS_SELECTED_CONNECT_JETPACK:
                eventName = "stats_connect_jetpack_selected";
                break;
            case STATS_WIDGET_ADDED:
                eventName = "stats_widget_added";
                break;
            case STATS_WIDGET_REMOVED:
                eventName = "stats_widget_removed";
                break;
            case STATS_WIDGET_TAPPED:
                eventName = "stats_widget_tapped";
                break;
            case PUSH_NOTIFICATION_RECEIVED:
                eventName = "push_notification_received";
                break;
            case PUSH_NOTIFICATION_TAPPED:
                eventName = "push_notification_alert_tapped";
                break;
            case SUPPORT_OPENED_HELPSHIFT_SCREEN:
                eventName = "support_helpshift_screen_opened";
                break;
            case SUPPORT_SENT_REPLY_TO_SUPPORT_MESSAGE:
                eventName = "support_reply_to_support_message_sent";
                break;
            case LOGIN_MAGIC_LINK_EXITED:
                eventName = "login_magic_link_exited";
                break;
            case LOGIN_MAGIC_LINK_FAILED:
                eventName = "login_magic_link_failed";
                break;
            case LOGIN_MAGIC_LINK_OPENED:
                eventName = "login_magic_link_opened";
                break;
            case LOGIN_MAGIC_LINK_REQUESTED:
                eventName = "login_magic_link_requested";
                break;
            case LOGIN_MAGIC_LINK_SUCCEEDED:
                eventName = "login_magic_link_succeeded";
                break;
            case LOGIN_FAILED:
                eventName = "login_failed_to_login";
                break;
            case LOGIN_FAILED_TO_GUESS_XMLRPC:
                eventName = "login_failed_to_guess_xmlrpc";
                break;
            case LOGIN_INSERTED_INVALID_URL:
                eventName = "login_inserted_invalid_url";
                break;
            case LOGIN_AUTOFILL_CREDENTIALS_FILLED:
                eventName = "login_autofill_credentials_filled";
                break;
            case LOGIN_AUTOFILL_CREDENTIALS_UPDATED:
                eventName = "login_autofill_credentials_updated";
                break;
            case PERSON_REMOVED:
                eventName = "people_management_person_removed";
                break;
            case PERSON_UPDATED:
                eventName = "people_management_person_updated";
                break;
            case PUSH_AUTHENTICATION_APPROVED:
                eventName = "push_authentication_approved";
                break;
            case PUSH_AUTHENTICATION_EXPIRED:
                eventName = "push_authentication_expired";
                break;
            case PUSH_AUTHENTICATION_FAILED:
                eventName = "push_authentication_failed";
                break;
            case PUSH_AUTHENTICATION_IGNORED:
                eventName = "push_authentication_ignored";
                break;
            case NOTIFICATION_SETTINGS_LIST_OPENED:
                eventName = "notification_settings_list_opened";
                break;
            case NOTIFICATION_SETTINGS_STREAMS_OPENED:
                eventName = "notification_settings_streams_opened";
                break;
            case NOTIFICATION_SETTINGS_DETAILS_OPENED:
                eventName = "notification_settings_details_opened";
                break;
            case ME_ACCESSED:
                eventName = "me_tab_accessed";
                break;
            case ME_GRAVATAR_TAPPED:
                eventName = "me_gravatar_tapped";
                break;
            case ME_GRAVATAR_TOOLTIP_TAPPED:
                eventName = "me_gravatar_tooltip_tapped";
                break;
            case ME_GRAVATAR_PERMISSIONS_INTERRUPTED:
                eventName = "me_gravatar_permissions_interrupted";
                break;
            case ME_GRAVATAR_PERMISSIONS_DENIED:
                eventName = "me_gravatar_permissions_denied";
                break;
            case ME_GRAVATAR_PERMISSIONS_ACCEPTED:
                eventName = "me_gravatar_permissions_accepted";
                break;
            case ME_GRAVATAR_SHOT_NEW:
                eventName = "me_gravatar_shot_new";
                break;
            case ME_GRAVATAR_GALLERY_PICKED:
                eventName = "me_gravatar_gallery_picked";
                break;
            case ME_GRAVATAR_CROPPED:
                eventName = "me_gravatar_cropped";
                break;
            case ME_GRAVATAR_UPLOADED:
                eventName = "me_gravatar_uploaded";
                break;
            case ME_GRAVATAR_UPLOAD_UNSUCCESSFUL:
                eventName = "me_gravatar_upload_unsuccessful";
                break;
            case ME_GRAVATAR_UPLOAD_EXCEPTION:
                eventName = "me_gravatar_upload_exception";
                break;
            case MY_SITE_ACCESSED:
                eventName = "my_site_tab_accessed";
                break;
            case THEMES_ACCESSED_THEMES_BROWSER:
                eventName = "themes_theme_browser_accessed";
                break;
            case THEMES_ACCESSED_SEARCH:
                eventName = "themes_search_accessed";
                break;
            case THEMES_CHANGED_THEME:
                eventName = "themes_theme_changed";
                break;
            case THEMES_PREVIEWED_SITE:
                eventName = "themes_theme_for_site_previewed";
                break;
            case THEMES_DEMO_ACCESSED:
                eventName = "themes_demo_accessed";
                break;
            case THEMES_CUSTOMIZE_ACCESSED:
                eventName = "themes_customize_accessed";
                break;
            case THEMES_SUPPORT_ACCESSED:
                eventName = "themes_support_accessed";
                break;
            case THEMES_DETAILS_ACCESSED:
                eventName = "themes_details_accessed";
                break;
            case ACCOUNT_SETTINGS_LANGUAGE_CHANGED:
                eventName = "account_settings_language_changed";
                break;
            case SITE_SETTINGS_ACCESSED:
                eventName = "site_settings_accessed";
                break;
            case SITE_SETTINGS_ACCESSED_MORE_SETTINGS:
                eventName = "site_settings_more_settings_accessed";
                break;
            case SITE_SETTINGS_ADDED_LIST_ITEM:
                eventName = "site_settings_added_list_item";
                break;
            case SITE_SETTINGS_DELETED_LIST_ITEMS:
                eventName = "site_settings_deleted_list_items";
                break;
            case SITE_SETTINGS_HINT_TOAST_SHOWN:
                eventName = "site_settings_hint_toast_shown";
                break;
            case SITE_SETTINGS_LEARN_MORE_CLICKED:
                eventName = "site_settings_learn_more_clicked";
                break;
            case SITE_SETTINGS_LEARN_MORE_LOADED:
                eventName = "site_settings_learn_more_loaded";
                break;
            case SITE_SETTINGS_SAVED_REMOTELY:
                eventName = "site_settings_saved_remotely";
                break;
            case SITE_SETTINGS_START_OVER_ACCESSED:
                eventName = "site_settings_start_over_accessed";
                break;
            case SITE_SETTINGS_START_OVER_CONTACT_SUPPORT_CLICKED:
                eventName = "site_settings_start_over_contact_support_clicked";
                break;
            case SITE_SETTINGS_EXPORT_SITE_ACCESSED:
                eventName = "site_settings_export_site_accessed";
                break;
            case SITE_SETTINGS_EXPORT_SITE_REQUESTED:
                eventName = "site_settings_export_site_requested";
                break;
            case SITE_SETTINGS_EXPORT_SITE_RESPONSE_OK:
                eventName = "site_settings_export_site_response_ok";
                break;
            case SITE_SETTINGS_EXPORT_SITE_RESPONSE_ERROR:
                eventName = "site_settings_export_site_response_error";
                break;
            case SITE_SETTINGS_DELETE_SITE_ACCESSED:
                eventName = "site_settings_delete_site_accessed";
                break;
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_REQUESTED:
                eventName = "site_settings_delete_site_purchases_requested";
                break;
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOWN:
                eventName = "site_settings_delete_site_purchases_shown";
                break;
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOW_CLICKED:
                eventName = "site_settings_delete_site_purchases_show_clicked";
                break;
            case SITE_SETTINGS_DELETE_SITE_REQUESTED:
                eventName = "site_settings_delete_site_requested";
                break;
            case SITE_SETTINGS_DELETE_SITE_RESPONSE_OK:
                eventName = "site_settings_delete_site_response_ok";
                break;
            case SITE_SETTINGS_DELETE_SITE_RESPONSE_ERROR:
                eventName = "site_settings_delete_site_response_error";
                break;
            case ABTEST_START:
                eventName = "abtest_start";
                break;
            default:
                eventName = null;
                break;
        }

        if (eventName == null) {
            AppLog.w(AppLog.T.STATS, "There is NO match for the event " + stat.name() + "stat");
            return;
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


        // create the merged JSON Object of properties
        // Properties defined by the user have precedence over the default ones pre-defined at "event level"
        final JSONObject propertiesToJSON;
        if (properties != null && properties.size() > 0) {
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
        } else{
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
    public void refreshMetadata(boolean isUserConnected, boolean isWordPressComUser, boolean isJetpackUser,
                                int sessionCount, int numBlogs, int versionCode, String username, String email) {
        if (mNosaraClient == null) {
            return;
        }

        try {
            JSONObject properties = new JSONObject();
            properties.put(JETPACK_USER, isJetpackUser);
            properties.put(NUMBER_OF_BLOGS, numBlogs);
            mNosaraClient.registerUserProperties(properties);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        if (isUserConnected && isWordPressComUser) {
            setWordPressComUserName(username);
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
}
