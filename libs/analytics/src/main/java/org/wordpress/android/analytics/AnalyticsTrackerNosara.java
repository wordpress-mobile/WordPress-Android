package org.wordpress.android.analytics;

import android.content.Context;
import android.text.TextUtils;

import com.automattic.android.tracks.TracksClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AnalyticsTrackerNosara extends Tracker {
    private static final String JETPACK_USER = "jetpack_user";
    private static final String NUMBER_OF_BLOGS = "number_of_blogs";
    private static final String TRACKS_ANON_ID = "nosara_tracks_anon_id";
    @SuppressWarnings("checkstyle:RegexpSingleline")
    private static final String WPCOM_USER = "dotcom_user";
    private static final String IS_GUTENBERG_ENABLED = "gutenberg_enabled";
    private static final String APP_SCHEME = "app_scheme";

    private final String mEventsPrefix;

    private final TracksClient mNosaraClient;

    public AnalyticsTrackerNosara(Context context, String eventsPrefix) throws IllegalArgumentException {
        super(context);
        mEventsPrefix = eventsPrefix;
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
    @SuppressWarnings("checkstyle:methodlength")
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

        Map<String, Object> predefinedEventProperties = new HashMap<>();
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
            case EDITOR_ADDED_PHOTO_VIA_STOCK_MEDIA_LIBRARY:
                predefinedEventProperties.put("via", "stock_photos");
                break;
            case EDITOR_ADDED_PHOTO_VIA_MEDIA_EDITOR:
                predefinedEventProperties.put("via", "media_editor");
                break;
            case EDITOR_TAPPED_BLOCKQUOTE:
                predefinedEventProperties.put("button", "blockquote");
                break;
            case EDITOR_TAPPED_BOLD:
                predefinedEventProperties.put("button", "bold");
                break;
            case EDITOR_TAPPED_ELLIPSIS_COLLAPSE:
                predefinedEventProperties.put("button", "overflow_ellipsis");
                predefinedEventProperties.put("action", "made_hidden");
                break;
            case EDITOR_TAPPED_ELLIPSIS_EXPAND:
                predefinedEventProperties.put("button", "overflow_ellipsis");
                predefinedEventProperties.put("action", "made_visible");
                break;
            case EDITOR_TAPPED_HEADING:
                predefinedEventProperties.put("button", "header");
                break;
            case EDITOR_TAPPED_HEADING_1:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h1");
                break;
            case EDITOR_TAPPED_HEADING_2:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h2");
                break;
            case EDITOR_TAPPED_HEADING_3:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h3");
                break;
            case EDITOR_TAPPED_HEADING_4:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h4");
                break;
            case EDITOR_TAPPED_HEADING_5:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h5");
                break;
            case EDITOR_TAPPED_HEADING_6:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h6");
                break;
            case EDITOR_TAPPED_IMAGE:
                predefinedEventProperties.put("button", "image");
                break;
            case EDITOR_TAPPED_ITALIC:
                predefinedEventProperties.put("button", "italic");
                break;
            case EDITOR_TAPPED_LINK_ADDED:
                predefinedEventProperties.put("button", "link");
                break;
            case EDITOR_TAPPED_LIST:
                predefinedEventProperties.put("button", "list");
                break;
            case EDITOR_TAPPED_LIST_ORDERED:
                predefinedEventProperties.put("button", "ordered_list");
                break;
            case EDITOR_TAPPED_LIST_UNORDERED:
                predefinedEventProperties.put("button", "unordered_list");
                break;
            case EDITOR_TAPPED_NEXT_PAGE:
                predefinedEventProperties.put("button", "next_page");
                break;
            case EDITOR_TAPPED_PARAGRAPH:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "none");
                break;
            case EDITOR_TAPPED_PREFORMAT:
                predefinedEventProperties.put("button", "preformat");
                break;
            case EDITOR_TAPPED_READ_MORE:
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
            case EDITOR_TAPPED_ALIGN_LEFT:
                predefinedEventProperties.put("button", "align_left");
                break;
            case EDITOR_TAPPED_ALIGN_CENTER:
                predefinedEventProperties.put("button", "align_center");
                break;
            case EDITOR_TAPPED_ALIGN_RIGHT:
                predefinedEventProperties.put("button", "align_right");
                break;
            case EDITOR_TAPPED_UNDO:
                predefinedEventProperties.put("button", "undo");
                break;
            case EDITOR_TAPPED_REDO:
                predefinedEventProperties.put("button", "redo");
                break;
            case EDITOR_TAPPED_HORIZONTAL_RULE:
                predefinedEventProperties.put("button", "horizontal_rule");
                break;
            case REVISIONS_DETAIL_VIEWED_FROM_LIST:
                predefinedEventProperties.put("source", "list");
                break;
            case REVISIONS_DETAIL_VIEWED_FROM_SWIPE:
                predefinedEventProperties.put("source", "swipe");
                break;
            case REVISIONS_DETAIL_VIEWED_FROM_CHEVRON:
                predefinedEventProperties.put("source", "chevron");
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
            case OPENED_PLANS:
                predefinedEventProperties.put("menu_item", "plans");
                break;
            case OPENED_SHARING_MANAGEMENT:
                predefinedEventProperties.put("menu_item", "sharing_management");
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
            case COMMENT_QUICK_ACTION_LIKED:
            case COMMENT_QUICK_ACTION_REPLIED_TO:
            case COMMENT_QUICK_ACTION_APPROVED:
                predefinedEventProperties.put("is_quick_action", true);
                break;
            case SIGNUP_EMAIL_EPILOGUE_UNCHANGED:
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED:
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED:
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_FAILED:
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED:
            case SIGNUP_EMAIL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED:
            case SIGNUP_EMAIL_EPILOGUE_USERNAME_TAPPED:
            case SIGNUP_EMAIL_EPILOGUE_VIEWED:
                predefinedEventProperties.put("source", "email");
                break;
            case SIGNUP_SOCIAL_EPILOGUE_UNCHANGED:
            case SIGNUP_SOCIAL_BUTTON_FAILURE:
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED:
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED:
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_FAILED:
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED:
            case SIGNUP_SOCIAL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED:
            case SIGNUP_SOCIAL_EPILOGUE_USERNAME_TAPPED:
            case SIGNUP_SOCIAL_EPILOGUE_VIEWED:
                predefinedEventProperties.put("source", "social");
                break;
            case SIGNUP_SOCIAL_BUTTON_TAPPED:
                predefinedEventProperties.put("source", "google");
                break;
            case READER_POST_SAVED_FROM_OTHER_POST_LIST:
                predefinedEventProperties.put("source", "other_post_list");
                break;
            case READER_POST_SAVED_FROM_SAVED_POST_LIST:
                predefinedEventProperties.put("source", "saved_post_list");
                break;
            case READER_POST_SAVED_FROM_DETAILS:
                predefinedEventProperties.put("source", "post_details");
                break;
            case READER_POST_UNSAVED_FROM_OTHER_POST_LIST:
                predefinedEventProperties.put("source", "other_post_list");
                break;
            case READER_POST_UNSAVED_FROM_SAVED_POST_LIST:
                predefinedEventProperties.put("source", "saved_post_list");
                break;
            case READER_POST_UNSAVED_FROM_DETAILS:
                predefinedEventProperties.put("source", "post_details");
                break;
            case READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST:
                predefinedEventProperties.put("source", "saved_post_list");
                break;
            case READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST:
                predefinedEventProperties.put("source", "other_post_list");
                break;
            case QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED:
                predefinedEventProperties.put("type", "negative");
                break;
            case QUICK_START_TASK_DIALOG_POSITIVE_TAPPED:
                predefinedEventProperties.put("type", "positive");
                break;
            case QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED:
                predefinedEventProperties.put("type", "negative");
                break;
            case QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED:
                predefinedEventProperties.put("type", "positive");
                break;
            case QUICK_START_LIST_CREATE_SITE_TAPPED:
                predefinedEventProperties.put("task_name", "create_site");
                break;
            case QUICK_START_LIST_UPDATE_SITE_TITLE_TAPPED:
                predefinedEventProperties.put("task_name", "update_site_title");
                break;
            case QUICK_START_TYPE_CUSTOMIZE_VIEWED:
                predefinedEventProperties.put("type", "customize");
                break;
            case QUICK_START_TYPE_GROW_VIEWED:
                predefinedEventProperties.put("type", "grow");
                break;
            case QUICK_START_TYPE_GET_TO_KNOW_APP_VIEWED:
                predefinedEventProperties.put("type", "get_to_know_app");
                break;
            case QUICK_START_TYPE_CUSTOMIZE_DISMISSED:
                predefinedEventProperties.put("type", "customize");
                break;
            case QUICK_START_TYPE_GROW_DISMISSED:
                predefinedEventProperties.put("type", "grow");
                break;
            case QUICK_START_TYPE_GET_TO_KNOW_APP_DISMISSED:
                predefinedEventProperties.put("type", "get_to_know_app");
                break;
            case QUICK_START_LIST_CREATE_SITE_SKIPPED:
                predefinedEventProperties.put("task_name", "create_site");
                break;
            case QUICK_START_LIST_UPDATE_SITE_TITLE_SKIPPED:
                predefinedEventProperties.put("task_name", "update_site_title");
                break;
            case QUICK_START_LIST_VIEW_SITE_SKIPPED:
                predefinedEventProperties.put("task_name", "view_site");
                break;
            case QUICK_START_LIST_ADD_SOCIAL_SKIPPED:
                predefinedEventProperties.put("task_name", "share_site");
                break;
            case QUICK_START_LIST_PUBLISH_POST_SKIPPED:
                predefinedEventProperties.put("task_name", "publish_post");
                break;
            case QUICK_START_LIST_FOLLOW_SITE_SKIPPED:
                predefinedEventProperties.put("task_name", "follow_site");
                break;
            case QUICK_START_LIST_UPLOAD_ICON_SKIPPED:
                predefinedEventProperties.put("task_name", "upload_icon");
                break;
            case QUICK_START_LIST_CHECK_STATS_SKIPPED:
                predefinedEventProperties.put("task_name", "check_stats");
                break;
            case QUICK_START_LIST_VIEW_SITE_TAPPED:
                predefinedEventProperties.put("task_name", "view_site");
                break;
            case QUICK_START_LIST_ADD_SOCIAL_TAPPED:
                predefinedEventProperties.put("task_name", "share_site");
                break;
            case QUICK_START_LIST_PUBLISH_POST_TAPPED:
                predefinedEventProperties.put("task_name", "publish_post");
                break;
            case QUICK_START_LIST_FOLLOW_SITE_TAPPED:
                predefinedEventProperties.put("task_name", "follow_site");
                break;
            case QUICK_START_LIST_UPLOAD_ICON_TAPPED:
                predefinedEventProperties.put("task_name", "upload_icon");
                break;
            case QUICK_START_LIST_CHECK_STATS_TAPPED:
                predefinedEventProperties.put("task_name", "check_stats");
                break;
            case QUICK_START_CREATE_SITE_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "create_site");
                break;
            case QUICK_START_UPDATE_SITE_TITLE_COMPLETED:
                predefinedEventProperties.put("task_name", "update_site_title");
                break;
            case QUICK_START_VIEW_SITE_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "view_site");
                break;
            case QUICK_START_SHARE_SITE_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "share_site");
                break;
            case QUICK_START_PUBLISH_POST_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "publish_post");
                break;
            case QUICK_START_FOLLOW_SITE_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "follow_site");
                break;
            case QUICK_START_UPLOAD_ICON_COMPLETED:
                predefinedEventProperties.put("task_name", "upload_icon");
                break;
            case QUICK_START_CHECK_STATS_COMPLETED:
                predefinedEventProperties.put("task_name", "check_stats");
                break;
            case QUICK_START_LIST_REVIEW_PAGES_SKIPPED:
            case QUICK_START_LIST_REVIEW_PAGES_TAPPED:
            case QUICK_START_REVIEW_PAGES_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "review_pages");
                break;
            case QUICK_START_LIST_CHECK_NOTIFICATIONS_SKIPPED:
            case QUICK_START_LIST_CHECK_NOTIFICATIONS_TAPPED:
            case QUICK_START_CHECK_NOTIFICATIONS_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "check_notifications");
                break;
            case QUICK_START_LIST_UPLOAD_MEDIA_SKIPPED:
            case QUICK_START_LIST_UPLOAD_MEDIA_TAPPED:
            case QUICK_START_UPLOAD_MEDIA_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "media_upload");
                break;
            case QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED:
                predefinedEventProperties.put("type", "negative");
                break;
            case QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED:
                predefinedEventProperties.put("type", "positive");
                break;
            case APP_REVIEWS_EVENT_INCREMENTED_BY_UPLOADING_MEDIA:
                predefinedEventProperties.put("source", "media_upload");
                break;
            case APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION:
                predefinedEventProperties.put("source", "notification_details");
                break;
            case APP_REVIEWS_EVENT_INCREMENTED_BY_PUBLISHING_POST_OR_PAGE:
                predefinedEventProperties.put("source", "publishing_post_or_page");
                break;
            case APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST:
                predefinedEventProperties.put("source", "opening_reader_post");
                break;
            case QUICK_LINK_RIBBON_PAGES_TAPPED:
                predefinedEventProperties.put("button", "pages");
                break;
            case QUICK_LINK_RIBBON_POSTS_TAPPED:
                predefinedEventProperties.put("button", "posts");
                break;
            case QUICK_LINK_RIBBON_MEDIA_TAPPED:
                predefinedEventProperties.put("button", "media");
                break;
            case QUICK_LINK_RIBBON_STATS_TAPPED:
                predefinedEventProperties.put("button", "stats");
                break;
            case WELCOME_NO_SITES_INTERSTITIAL_CREATE_NEW_SITE_TAPPED:
                predefinedEventProperties.put("button", "create_new_site");
                break;
            case WELCOME_NO_SITES_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED:
                predefinedEventProperties.put("button", "add_self_hosted_site");
                break;
            case FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE:
                predefinedEventProperties.put("source", "app_upgrade");
                break;
            case FEATURE_ANNOUNCEMENT_SHOWN_FROM_APP_SETTINGS:
                predefinedEventProperties.put("source", "app_settings");
                break;
            case FEATURE_ANNOUNCEMENT_CLOSE_DIALOG_BUTTON_TAPPED:
                predefinedEventProperties.put("button", "close_dialog");
                break;
            case FEATURE_ANNOUNCEMENT_FIND_OUT_MORE_TAPPED:
                predefinedEventProperties.put("button", "find_out_more");
                break;
            case READER_ARTICLE_COMMENTED_ON:
                predefinedEventProperties.put("replying_to", "post");
                break;
            case READER_ARTICLE_COMMENT_REPLIED_TO:
                predefinedEventProperties.put("replying_to", "comment");
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
                // TODO add Crash Logging Exception or track this error in Nosara by using a special test user.
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
                            AppLog.w(AppLog.T.STATS,
                                    "The user has defined a property named: '" + key + "' that will override"
                                    + "the same property pre-defined at event level. This may generate unexpected "
                                    + "behavior!!");
                            AppLog.w(AppLog.T.STATS,
                                    "User value: " + propertiesToJSON.get(key)
                                    + " - pre-defined value: "
                                    + predefinedEventProperties.get(key).toString());
                        } else {
                            propertiesToJSON.put(key, predefinedEventProperties.get(key));
                        }
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.STATS,
                                "Error while merging user-defined properties with pre-defined properties", e);
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
            mNosaraClient.track(mEventsPrefix + eventName, propertiesToJSON, user, userType);
            String jsonString = propertiesToJSON.toString();
            AppLog.i(T.STATS, "\uD83D\uDD35 Tracked: " + eventName + ", Properties: " + jsonString);
        } else {
            mNosaraClient.track(mEventsPrefix + eventName, user, userType);
            AppLog.i(T.STATS, "\uD83D\uDD35 Tracked: " + eventName);
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
            properties.put(WPCOM_USER, metadata.isWordPressComUser());
            properties.put(APP_SCHEME, metadata.getAppScheme());
            // Only add the editor information if it was set before.
            // See: https://github.com/wordpress-mobile/WordPress-Android/pull/10300#discussion_r309145514
            if (metadata.isGutenbergEnabledVariableSet()) {
                properties.put(IS_GUTENBERG_ENABLED, metadata.isGutenbergEnabled());
            }
            mNosaraClient.registerUserProperties(properties);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        // De-anonymize user only when it's WPCOM and we have the username available (might still be waiting for it to
        //  be fetched).
        if (metadata.isUserConnected() && metadata.isWordPressComUser()
            && !TextUtils.isEmpty(metadata.getUsername())) {
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
        mNosaraClient.clearQueues();
    }

    /**
     * Returns the event name for a given Stat.
     * NOTES:
     * - Please add a new case only if the new Stat's name in lower case does not match the expected event name. In that
     *   case you also need to add the event in the `AnalyticsTrackerNosaraTest.specialNames` map.
     * - Otherwise declaring the new `AnalyticsTracker.Stat` is enough.
     * @param stat the stat to get the event name for
     * @return event name
     */
    @SuppressWarnings("checkstyle:methodlength")
    public static String getEventNameForStat(AnalyticsTracker.Stat stat) {
        if (!isValidEvent(stat)) {
            return null;
        }

        switch (stat) {
            case READER_ARTICLE_COMMENT_REPLIED_TO:
                return "reader_article_commented_on";
            case READER_BLOG_FOLLOWED:
                return "reader_site_followed";
            case READER_BLOG_UNFOLLOWED:
                return "reader_site_unfollowed";
            case READER_INFINITE_SCROLL:
                return "reader_infinite_scroll_performed";
            case READER_TAG_FOLLOWED:
                return "reader_reader_tag_followed";
            case READER_TAG_UNFOLLOWED:
                return "reader_reader_tag_unfollowed";
            case READER_SEARCH_RESULT_TAPPED:
                return "reader_searchcard_clicked";
            case READER_GLOBAL_RELATED_POST_CLICKED:
                return "reader_related_post_from_other_site_clicked";
            case READER_LOCAL_RELATED_POST_CLICKED:
                return "reader_related_post_from_same_site_clicked";
            case READER_POST_SAVED_FROM_OTHER_POST_LIST:
                return "reader_post_saved";
            case READER_POST_SAVED_FROM_SAVED_POST_LIST:
                return "reader_post_saved";
            case READER_POST_SAVED_FROM_DETAILS:
                return "reader_post_saved";
            case READER_POST_UNSAVED_FROM_OTHER_POST_LIST:
                return "reader_post_unsaved";
            case READER_POST_UNSAVED_FROM_SAVED_POST_LIST:
                return "reader_post_unsaved";
            case READER_POST_UNSAVED_FROM_DETAILS:
                return "reader_post_unsaved";
            case READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST:
                return "reader_saved_post_opened";
            case READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST:
                return "reader_saved_post_opened";
            case STATS_PERIOD_DAYS_ACCESSED:
                return "stats_period_accessed";
            case STATS_PERIOD_WEEKS_ACCESSED:
                return "stats_period_accessed";
            case STATS_PERIOD_MONTHS_ACCESSED:
                return "stats_period_accessed";
            case STATS_PERIOD_YEARS_ACCESSED:
                return "stats_period_accessed";
            case STATS_TAPPED_BAR_CHART:
                return "stats_bar_chart_tapped";
            case EDITOR_CREATED_POST:
                return "editor_post_created";
            case EDITOR_ADDED_PHOTO_VIA_DEVICE_LIBRARY:
                return "editor_photo_added";
            case EDITOR_ADDED_VIDEO_VIA_DEVICE_LIBRARY:
                return "editor_video_added";
            case EDITOR_ADDED_PHOTO_VIA_MEDIA_EDITOR:
                return "editor_photo_added";
            case EDITOR_ADDED_PHOTO_NEW:
                return "editor_photo_added";
            case EDITOR_ADDED_VIDEO_NEW:
                return "editor_video_added";
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
                return "editor_photo_added";
            case EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY:
                return "editor_video_added";
            case EDITOR_ADDED_PHOTO_VIA_STOCK_MEDIA_LIBRARY:
                return "editor_photo_added";
            case MEDIA_PICKER_OPEN_CAPTURE_MEDIA:
                return "media_picker_capture_media_opened";
            case MEDIA_PICKER_OPEN_DEVICE_LIBRARY:
                return "media_picker_device_library_opened";
            case MEDIA_PICKER_OPEN_WP_MEDIA:
                return "media_picker_wordpress_library_opened";
            case EDITOR_UPDATED_POST:
                return "editor_post_updated";
            case EDITOR_SCHEDULED_POST:
                return "editor_post_scheduled";
            case EDITOR_PUBLISHED_POST:
                return "editor_post_published";
            case EDITOR_SAVED_DRAFT:
                return "editor_draft_saved";
            case EDITOR_EDITED_IMAGE:
                return "editor_image_edited";
            case EDITOR_TAPPED_BLOCKQUOTE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_BOLD:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ELLIPSIS_COLLAPSE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ELLIPSIS_EXPAND:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_1:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_2:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_3:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_4:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_5:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_6:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HTML:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HORIZONTAL_RULE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_IMAGE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ITALIC:
                return "editor_button_tapped";
            case EDITOR_TAPPED_LINK_ADDED:
                return "editor_button_tapped";
            case EDITOR_TAPPED_LIST:
                return "editor_button_tapped";
            case EDITOR_TAPPED_LIST_ORDERED:
                return "editor_button_tapped";
            case EDITOR_TAPPED_LIST_UNORDERED:
                return "editor_button_tapped";
            case EDITOR_TAPPED_NEXT_PAGE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_PARAGRAPH:
                return "editor_button_tapped";
            case EDITOR_TAPPED_PREFORMAT:
                return "editor_button_tapped";
            case EDITOR_TAPPED_READ_MORE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_STRIKETHROUGH:
                return "editor_button_tapped";
            case EDITOR_TAPPED_UNDERLINE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ALIGN_LEFT:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ALIGN_CENTER:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ALIGN_RIGHT:
                return "editor_button_tapped";
            case EDITOR_TAPPED_REDO:
                return "editor_button_tapped";
            case EDITOR_TAPPED_UNDO:
                return "editor_button_tapped";
            case EDITOR_GUTENBERG_ENABLED:
                return "gutenberg_enabled";
            case EDITOR_GUTENBERG_DISABLED:
                return "gutenberg_disabled";
            case REVISIONS_DETAIL_VIEWED_FROM_LIST:
                return "revisions_detail_viewed";
            case REVISIONS_DETAIL_VIEWED_FROM_SWIPE:
                return "revisions_detail_viewed";
            case REVISIONS_DETAIL_VIEWED_FROM_CHEVRON:
                return "revisions_detail_viewed";
            case ME_ACCESSED:
                return "me_tab_accessed";
            case MY_SITE_ACCESSED:
                return "my_site_tab_accessed";
            case NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS:
                return "notifications_notification_details_opened";
            case NOTIFICATION_REPLIED_TO:
                return "notifications_replied_to";
            case NOTIFICATION_QUICK_ACTIONS_REPLIED_TO:
                return "notifications_replied_to";
            case NOTIFICATION_APPROVED:
                return "notifications_approved";
            case NOTIFICATION_QUICK_ACTIONS_APPROVED:
                return "notifications_approved";
            case NOTIFICATION_UNAPPROVED:
                return "notifications_unapproved";
            case NOTIFICATION_LIKED:
                return "notifications_comment_liked";
            case NOTIFICATION_QUICK_ACTIONS_LIKED:
                return "notifications_comment_liked";
            case NOTIFICATION_QUICK_ACTIONS_QUICKACTION_TOUCHED:
                return "quick_action_touched";
            case NOTIFICATION_UNLIKED:
                return "notifications_comment_unliked";
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
            case NOTIFICATION_UPLOAD_MEDIA_SUCCESS_WRITE_POST:
                return "notifications_upload_media_success_write_post";
            case NOTIFICATION_UPLOAD_POST_ERROR_RETRY:
                return "notifications_upload_post_error_retry";
            case NOTIFICATION_UPLOAD_MEDIA_ERROR_RETRY:
                return "notifications_upload_media_error_retry";
            case NOTIFICATION_RECEIVED_PROCESSING_START:
                return "notifications_received_processing_start";
            case NOTIFICATION_RECEIVED_PROCESSING_END:
                return "notifications_received_processing_end";
            case OPENED_POSTS:
                return "site_menu_opened";
            case OPENED_PAGES:
                return "site_menu_opened";
            case OPENED_PAGE_PARENT:
                return "page_parent_opened";
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
            case OPENED_PLUGIN_DIRECTORY:
                return "plugin_directory_opened";
            case OPENED_PLANS:
                return "site_menu_opened";
            case OPENED_PLANS_COMPARISON:
                return "plans_compare";
            case OPENED_SHARING_MANAGEMENT:
                return "site_menu_opened";
            case OPENED_SHARING_BUTTON_MANAGEMENT:
                return "sharing_buttons_opened";
            case ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_BUTTON_TAPPED:
                return "activitylog_filterbar_range_button_tapped";
            case ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_BUTTON_TAPPED:
                return "activitylog_filterbar_type_button_tapped";
            case ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_SELECTED:
                return "activitylog_filterbar_select_range";
            case ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_SELECTED:
                return "activitylog_filterbar_select_type";
            case ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_RESET:
                return "activitylog_filterbar_reset_range";
            case ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_RESET:
                return "activitylog_filterbar_reset_type";
            case JETPACK_BACKUP_FILTER_BAR_DATE_RANGE_BUTTON_TAPPED:
                return "jetpack_backup_filterbar_range_button_tapped";
            case JETPACK_BACKUP_FILTER_BAR_DATE_RANGE_SELECTED:
                return "jetpack_backup_filterbar_select_range";
            case JETPACK_BACKUP_FILTER_BAR_DATE_RANGE_RESET:
                return "jetpack_backup_filterbar_reset_range";
            case JETPACK_SCAN_IGNORE_THREAT_DIALOG_OPEN:
                return "jetpack_scan_ignorethreat_dialogopen";
            case JETPACK_SCAN_FIX_THREAT_DIALOG_OPEN:
                return "jetpack_scan_fixthreat_dialogopen";
            case JETPACK_SCAN_ALL_THREATS_OPEN:
                return "jetpack_scan_allthreats_open";
            case JETPACK_SCAN_ALL_THREATS_FIX_TAPPED:
                return "jetpack_scan_allthreats_fix_tapped";
            case OPENED_PLUGIN_LIST:
                return "plugin_list_opened";
            case OPENED_PLUGIN_DETAIL:
                return "plugin_detail_opened";
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
            case SHARED_ITEM_READER:
                return "item_shared_reader";
            case ADDED_SELF_HOSTED_SITE:
                return "self_hosted_blog_added";
            case INSTALL_JETPACK_CANCELLED:
                return "install_jetpack_canceled";
            case PUSH_NOTIFICATION_TAPPED:
                return "push_notification_alert_tapped";
            case LOGIN_FAILED:
                return "login_failed_to_login";
            case PAGES_SET_PARENT_CHANGES_SAVED:
                return "site_pages_set_parent_changes_saved";
            case PAGES_ADD_PAGE:
                return "site_pages_add_page";
            case PAGES_TAB_PRESSED:
                return "site_pages_tabs_pressed";
            case PAGES_OPTIONS_PRESSED:
                return "site_pages_options_pressed";
            case PAGES_SEARCH_ACCESSED:
                return "site_pages_search_accessed";
            case PAGES_EDIT_HOMEPAGE_INFO_PRESSED:
                return "site_pages_edit_homepage_info_pressed";
            case PAGES_EDIT_HOMEPAGE_ITEM_PRESSED:
                return "site_pages_edit_homepage_item_pressed";
            case SIGNUP_EMAIL_EPILOGUE_GRAVATAR_GALLERY_PICKED:
                return "signup_email_epilogue_gallery_picked";
            case SIGNUP_EMAIL_EPILOGUE_GRAVATAR_SHOT_NEW:
                return "signup_email_epilogue_shot_new";
            case SIGNUP_EMAIL_EPILOGUE_UNCHANGED:
                return "signup_epilogue_unchanged";
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED:
                return "signup_epilogue_update_display_name_failed";
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED:
                return "signup_epilogue_update_display_name_succeeded";
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_FAILED:
                return "signup_epilogue_update_username_failed";
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED:
                return "signup_epilogue_update_username_succeeded";
            case SIGNUP_EMAIL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED:
                return "signup_epilogue_username_suggestions_failed";
            case SIGNUP_EMAIL_EPILOGUE_USERNAME_TAPPED:
                return "signup_epilogue_username_tapped";
            case SIGNUP_EMAIL_EPILOGUE_VIEWED:
                return "signup_epilogue_viewed";
            case SIGNUP_SOCIAL_EPILOGUE_UNCHANGED:
                return "signup_epilogue_unchanged";
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED:
                return "signup_epilogue_update_display_name_failed";
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED:
                return "signup_epilogue_update_display_name_succeeded";
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_FAILED:
                return "signup_epilogue_update_username_failed";
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED:
                return "signup_epilogue_update_username_succeeded";
            case SIGNUP_SOCIAL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED:
                return "signup_epilogue_username_suggestions_failed";
            case SIGNUP_SOCIAL_EPILOGUE_USERNAME_TAPPED:
                return "signup_epilogue_username_tapped";
            case SIGNUP_SOCIAL_EPILOGUE_VIEWED:
                return "signup_epilogue_viewed";
            case MEDIA_LIBRARY_ADDED_PHOTO:
                return "media_library_photo_added";
            case MEDIA_LIBRARY_ADDED_VIDEO:
                return "media_library_video_added";
            case PERSON_REMOVED:
                return "people_management_person_removed";
            case PERSON_UPDATED:
                return "people_management_person_updated";
            case THEMES_ACCESSED_THEMES_BROWSER:
                return "themes_theme_browser_accessed";
            case THEMES_ACCESSED_SEARCH:
                return "themes_search_accessed";
            case THEMES_CHANGED_THEME:
                return "themes_theme_changed";
            case THEMES_PREVIEWED_SITE:
                return "themes_theme_for_site_previewed";
            case SITE_SETTINGS_ACCESSED_MORE_SETTINGS:
                return "site_settings_more_settings_accessed";
            case SITE_SETTINGS_JETPACK_SECURITY_SETTINGS_VIEWED:
                return "jetpack_settings_viewed";
            case SITE_SETTINGS_JETPACK_ALLOWLISTED_IPS_VIEWED:
                return "jetpack_allowlisted_ips_viewed";
            case SITE_SETTINGS_JETPACK_ALLOWLISTED_IPS_CHANGED:
                return "jetpack_allowlisted_ips_changed";
            case TRAIN_TRACKS_RENDER:
                return "traintracks_render";
            case TRAIN_TRACKS_INTERACT:
                return "traintracks_interact";
            case MEDIA_UPLOAD_STARTED:
                return "media_service_upload_started";
            case MEDIA_UPLOAD_ERROR:
                return "media_service_upload_response_error";
            case MEDIA_UPLOAD_SUCCESS:
                return "media_service_upload_response_ok";
            case MEDIA_UPLOAD_CANCELED:
                return "media_service_upload_canceled";
            case QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED:
                return "quick_start_task_dialog_button_tapped";
            case QUICK_START_TASK_DIALOG_POSITIVE_TAPPED:
                return "quick_start_task_dialog_button_tapped";
            case QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED:
                return "quick_start_remove_dialog_button_tapped";
            case QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED:
                return "quick_start_remove_dialog_button_tapped";
            case QUICK_START_TYPE_CUSTOMIZE_VIEWED:
                return "quick_start_list_viewed";
            case QUICK_START_TYPE_GROW_VIEWED:
                return "quick_start_list_viewed";
            case QUICK_START_TYPE_GET_TO_KNOW_APP_VIEWED:
                return "quick_start_list_viewed";
            case QUICK_START_TYPE_CUSTOMIZE_DISMISSED:
                return "quick_start_type_dismissed";
            case QUICK_START_TYPE_GROW_DISMISSED:
                return "quick_start_type_dismissed";
            case QUICK_START_TYPE_GET_TO_KNOW_APP_DISMISSED:
                return "quick_start_type_dismissed";
            case QUICK_START_LIST_CREATE_SITE_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_UPDATE_SITE_TITLE_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_VIEW_SITE_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_ADD_SOCIAL_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_PUBLISH_POST_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_FOLLOW_SITE_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_UPLOAD_ICON_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_CHECK_STATS_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_REVIEW_PAGES_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_CHECK_NOTIFICATIONS_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_UPLOAD_MEDIA_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_CREATE_SITE_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_UPDATE_SITE_TITLE_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_VIEW_SITE_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_ADD_SOCIAL_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_PUBLISH_POST_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_FOLLOW_SITE_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_UPLOAD_ICON_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_CHECK_STATS_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_REVIEW_PAGES_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_CHECK_NOTIFICATIONS_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_LIST_UPLOAD_MEDIA_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_CREATE_SITE_TASK_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_UPDATE_SITE_TITLE_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_VIEW_SITE_TASK_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_SHARE_SITE_TASK_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_PUBLISH_POST_TASK_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_FOLLOW_SITE_TASK_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_UPLOAD_ICON_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_CHECK_STATS_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_REVIEW_PAGES_TASK_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_CHECK_NOTIFICATIONS_TASK_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_UPLOAD_MEDIA_TASK_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_REQUEST_VIEWED:
                return "quick_start_request_dialog_viewed";
            case QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED:
                return "quick_start_request_dialog_button_tapped";
            case QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED:
                return "quick_start_request_dialog_button_tapped";
            case APP_REVIEWS_DECLINED_TO_RATE_APP:
                return "app_reviews_declined_to_rate_apt";
            case APP_REVIEWS_EVENT_INCREMENTED_BY_UPLOADING_MEDIA:
                return "app_reviews_significant_event_incremented";
            case APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION:
                return "app_reviews_significant_event_incremented";
            case APP_REVIEWS_EVENT_INCREMENTED_BY_PUBLISHING_POST_OR_PAGE:
                return "app_reviews_significant_event_incremented";
            case APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST:
                return "app_reviews_significant_event_incremented";
            case DOMAINS_SEARCH_SELECT_DOMAIN_TAPPED:
                return "domains_dashboard_select_domain_tapped";
            case QUICK_LINK_RIBBON_PAGES_TAPPED:
                return "quick_action_ribbon_tapped";
            case QUICK_LINK_RIBBON_POSTS_TAPPED:
                return "quick_action_ribbon_tapped";
            case QUICK_LINK_RIBBON_MEDIA_TAPPED:
                return "quick_action_ribbon_tapped";
            case QUICK_LINK_RIBBON_STATS_TAPPED:
                return "quick_action_ribbon_tapped";
            case QUICK_LINK_RIBBON_MORE_TAPPED:
                return "quick_action_ribbon_tapped";
            case OPENED_QUICK_LINK_RIBBON_MORE:
                return "site_menu_opened";
            case WELCOME_NO_SITES_INTERSTITIAL_CREATE_NEW_SITE_TAPPED:
                return "welcome_no_sites_interstitial_button_tapped";
            case WELCOME_NO_SITES_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED:
                return "welcome_no_sites_interstitial_button_tapped";
            case FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE:
                return "feature_announcement_shown";
            case FEATURE_ANNOUNCEMENT_SHOWN_FROM_APP_SETTINGS:
                return "feature_announcement_shown";
            case FEATURE_ANNOUNCEMENT_FIND_OUT_MORE_TAPPED:
                return "feature_announcement_button_tapped";
            case FEATURE_ANNOUNCEMENT_CLOSE_DIALOG_BUTTON_TAPPED:
                return "feature_announcement_button_tapped";
            case EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_SHOWN:
                return "gutenberg_unsupported_block_webview_shown";
            case EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_CLOSED:
                return "gutenberg_unsupported_block_webview_closed";
            case READER_POST_MARKED_AS_SEEN:
                return "reader_mark_as_seen";
            case READER_POST_MARKED_AS_UNSEEN:
                return "reader_mark_as_unseen";
            case COMMENT_QUICK_ACTION_APPROVED:
                return "comment_approved";
            case COMMENT_QUICK_ACTION_LIKED:
                return "comment_liked";
            case COMMENT_QUICK_ACTION_REPLIED_TO:
                return "comment_replied_to";
            case BLOGGING_PROMPTS_MY_SITE_CARD_ANSWER_PROMPT_CLICKED:
                return "blogging_prompts_my_site_card_answer_prompt_tapped";
            case BLOGGING_PROMPTS_MY_SITE_CARD_SHARE_CLICKED:
                return "blogging_prompts_my_site_card_share_tapped";
            case BLOGGING_PROMPTS_MY_SITE_CARD_VIEW_ANSWERS_CLICKED:
                return "blogging_prompts_my_site_card_view_answers_tapped";
            case BLOGGING_PROMPTS_MY_SITE_CARD_MENU_CLICKED:
                return "blogging_prompts_my_site_card_menu_tapped";
            case BLOGGING_PROMPTS_MY_SITE_CARD_MENU_VIEW_MORE_PROMPTS_CLICKED:
                return "blogging_prompts_my_site_card_menu_view_more_prompts_tapped";
            case BLOGGING_PROMPTS_MY_SITE_CARD_MENU_SKIP_THIS_PROMPT_CLICKED:
                return "blogging_prompts_my_site_card_menu_skip_this_prompt_tapped";
            case BLOGGING_PROMPTS_MY_SITE_CARD_MENU_REMOVE_FROM_DASHBOARD_CLICKED:
                return "blogging_prompts_my_site_card_menu_remove_from_dashboard_tapped";
            case BLOGGING_PROMPTS_MY_SITE_CARD_MENU_SKIP_THIS_PROMPT_UNDO_CLICKED:
                return "blogging_prompts_my_site_card_menu_skip_this_prompt_undo_tapped";
            case BLOGGING_PROMPTS_MY_SITE_CARD_MENU_REMOVE_FROM_DASHBOARD_UNDO_CLICKED:
                return "blogging_prompts_my_site_card_menu_remove_from_dashboard_undo_tapped";
            case BLOGGING_PROMPTS_MY_SITE_CARD_MENU_LEARN_MORE_CLICKED:
                return "blogging_prompts_my_site_card_menu_learn_more_tapped";
            case BLOGGING_PROMPTS_INTRODUCTION_SCREEN_VIEWED:
                return "blogging_prompts_introduction_modal_viewed";
            case BLOGGING_PROMPTS_INTRODUCTION_SCREEN_DISMISSED:
                return "blogging_prompts_introduction_modal_dismissed";
            case BLOGGING_PROMPTS_INTRODUCTION_TRY_IT_NOW_CLICKED:
                return "blogging_prompts_introduction_modal_try_it_now_tapped";
            case BLOGGING_PROMPTS_INTRODUCTION_REMIND_ME_CLICKED:
                return "blogging_prompts_introduction_modal_remind_me_tapped";
            case BLOGGING_PROMPTS_INTRODUCTION_GOT_IT_CLICKED:
                return "blogging_prompts_introduction_modal_got_it_tapped";
            case BLOGGING_PROMPTS_LIST_SCREEN_VIEWED:
                return "blogging_prompts_prompts_list_viewed";
            case JETPACK_REMOVE_FEATURE_OVERLAY_DISPLAYED:
                return "remove_feature_overlay_displayed";
            case JETPACK_REMOVE_FEATURE_OVERLAY_LINK_TAPPED:
                return "remove_feature_overlay_link_tapped";
            case JETPACK_REMOVE_FEATURE_OVERLAY_BUTTON_GET_JETPACK_APP_TAPPED:
                return "remove_feature_overlay_button_tapped";
            case JETPACK_REMOVE_FEATURE_OVERLAY_DISMISSED:
                return "remove_feature_overlay_dismissed";
            case JETPACK_REMOVE_FEATURE_OVERLAY_LEARN_MORE_TAPPED:
                return "remove_feature_overlay_link_tapped";
            case JETPACK_REMOVE_SITE_CREATION_OVERLAY_DISPLAYED:
                return "remove_site_creation_overlay_displayed";
            case JETPACK_REMOVE_SITE_CREATION_OVERLAY_BUTTON_GET_JETPACK_APP_TAPPED:
                return "remove_site_creation_overlay_button_tapped";
            case JETPACK_REMOVE_SITE_CREATION_OVERLAY_DISMISSED:
                return "remove_site_creation_overlay_dismissed";
            case JETPACK_INSTALL_FULL_PLUGIN_CARD_VIEWED:
                return "jp_install_full_plugin_card_viewed";
            case JETPACK_INSTALL_FULL_PLUGIN_CARD_TAPPED:
                return "jp_install_full_plugin_card_tapped";
            case JETPACK_INSTALL_FULL_PLUGIN_CARD_DISMISSED:
                return "jp_install_full_plugin_card_dismissed";
            case JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_SCREEN_SHOWN:
                return "jp_install_full_plugin_onboarding_modal_viewed";
            case JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_SCREEN_DISMISSED:
                return "jp_install_full_plugin_onboarding_modal_dismissed";
            case JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_INSTALL_TAPPED:
                return "jp_install_full_plugin_onboarding_modal_install_tapped";
            case JETPACK_INSTALL_FULL_PLUGIN_FLOW_VIEWED:
                return "jp_install_full_plugin_flow_viewed";
            case JETPACK_INSTALL_FULL_PLUGIN_FLOW_CANCEL_TAPPED:
                return "jp_install_full_plugin_flow_cancel_tapped";
            case JETPACK_INSTALL_FULL_PLUGIN_FLOW_INSTALL_TAPPED:
                return "jp_install_full_plugin_flow_install_tapped";
            case JETPACK_INSTALL_FULL_PLUGIN_FLOW_RETRY_TAPPED:
                return "jp_install_full_plugin_flow_retry_tapped";
            case JETPACK_INSTALL_FULL_PLUGIN_FLOW_SUCCESS:
                return "jp_install_full_plugin_flow_success";
            case JETPACK_INSTALL_FULL_PLUGIN_FLOW_DONE_TAPPED:
                return "jp_install_full_plugin_flow_done_tapped";
            case BLAZE_FEATURE_OVERLAY_DISPLAYED:
                return "blaze_overlay_displayed";
            case BLAZE_FEATURE_OVERLAY_PROMOTE_CLICKED:
                return "blaze_overlay_button_tapped";
            case BLAZE_FEATURE_OVERLAY_DISMISSED:
                return "blaze_overlay_dismissed";
            case BLAZE_CAMPAIGN_LISTING_PAGE_SHOWN:
                return "blaze_campaign_list_opened";
            case BLAZE_CAMPAIGN_DETAIL_PAGE_OPENED:
                return "blaze_campaign_details_opened";
            case WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_SHOWN:
                return "wp_individual_site_overlay_viewed";
            case WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_DISMISSED:
                return "wp_individual_site_overlay_dismissed";
            case WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_PRIMARY_TAPPED:
                return "wp_individual_site_overlay_primary_tapped";
            case DASHBOARD_CARD_PLANS_SHOWN:
                return "free_to_paid_plan_dashboard_card_shown";
            case DASHBOARD_CARD_PLANS_TAPPED:
                return "free_to_paid_plan_dashboard_card_tapped";
            case DASHBOARD_CARD_PLANS_MORE_MENU_TAPPED:
                return "free_to_paid_plan_dashboard_card_menu_tapped";
            case DASHBOARD_CARD_PLANS_HIDDEN:
                return "free_to_paid_plan_dashboard_card_hidden";
            /*
             * Please add a new case only if the new Stat's name in lower case does not match the expected event name.
             * In that case you also need to add the event in the `AnalyticsTrackerNosaraTest.specialNames` map.
             */
            default:
                return stat.name().toLowerCase(Locale.getDefault());
        }
    }
}
// CHECKSTYLE END IGNORE
