package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.SiteSettingsModel;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MapUtils;
import org.xmlrpc.android.ApiHelper.Method;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class SelfHostedSiteSettings extends SiteSettingsInterface {
    // XML-RPC wp.getOptions keys
    public static final String PRIVACY_KEY = "blog_public";
    public static final String DEF_CATEGORY_KEY = "default_category";
    public static final String DEF_POST_FORMAT_KEY = "default_post_format";
    public static final String ALLOW_COMMENTS_KEY = "default_comment_status";
    public static final String SEND_PINGBACKS_KEY = "default_pingback_flag";
    public static final String RECEIVE_PINGBACKS_KEY = "default_ping_status";
    public static final String CLOSE_OLD_COMMENTS_KEY = "close_comments_for_old_posts";
    public static final String CLOSE_OLD_COMMENTS_DAYS_KEY = "close_comments_days_old";
    public static final String THREAD_COMMENTS_KEY = "thread_comments";
    public static final String THREAD_COMMENTS_DEPTH_KEY = "thread_comments_depth";
    public static final String PAGE_COMMENTS_KEY = "page_comments";
    public static final String PAGE_COMMENT_COUNT_KEY = "comments_per_page";
    public static final String COMMENT_SORT_ORDER_KEY = "comment_order";
    public static final String COMMENT_MODERATION_KEY = "comment_moderation";
    public static final String REQUIRE_IDENTITY_KEY = "require_name_email";
    public static final String REQUIRE_USER_ACCOUNT_KEY = "comment_registration";
    public static final String WHITELIST_KNOWN_USERS_KEY = "comment_whitelist";
    public static final String MAX_LINKS_KEY = "comment_max_links";
    public static final String MODERATION_KEYS_KEY = "moderation_keys";
    public static final String BLACKLIST_KEYS_KEY = "blacklist_keys";
    public static final String SOFTWARE_VERSION_KEY = "software_version";

    private static final String BLOG_URL_KEY = "blog_url";
    private static final String BLOG_TITLE_KEY = "blog_title";
    private static final String BLOG_USERNAME_KEY = "username";
    private static final String BLOG_PASSWORD_KEY = "password";
    private static final String BLOG_TAGLINE_KEY = "blog_tagline";
    private static final String BLOG_CATEGORY_ID_KEY = "categoryId";
    private static final String BLOG_CATEGORY_PARENT_ID_KEY = "parentId";
    private static final String BLOG_CATEGORY_DESCRIPTION_KEY = "categoryDescription";
    private static final String BLOG_CATEGORY_NAME_KEY = "categoryName";

    // Requires WordPress 4.5.x or higher
    private static final int REQUIRED_MAJOR_VERSION = 4;
    private static final int REQUIRED_MINOR_VERSION = 3;

    private static final String OPTION_ALLOWED = "open";
    private static final String OPTION_DISALLOWED = "closed";

    SelfHostedSiteSettings(Activity host, Blog blog, SiteSettingsListener listener) {
        super(host, blog, listener);
    }

    @Override
    public SiteSettingsInterface init(boolean fetch) {
        super.init(fetch);

        if (mSettings.defaultCategory == 0) {
            mSettings.defaultCategory = siteSettingsPreferences(mActivity).getInt(DEF_CATEGORY_PREF_KEY, 0);
        }
        if (TextUtils.isEmpty(mSettings.defaultPostFormat) || mSettings.defaultPostFormat.equals("0")) {
            mSettings.defaultPostFormat = siteSettingsPreferences(mActivity).getString(DEF_FORMAT_PREF_KEY, "0");
        }
        mSettings.language = siteSettingsPreferences(mActivity).getString(LANGUAGE_PREF_KEY, LanguageUtils.getPatchedCurrentDeviceLanguage(null));

        return this;
    }

    @Override
    public void saveSettings() {
        super.saveSettings();

        final Map<String, String> params = serializeSelfHostedParams();
        if (params == null || params.isEmpty()) return;

        XMLRPCCallback callback = new XMLRPCCallback() {
            @Override
            public void onSuccess(long id, final Object result) {
                notifySavedOnUiThread(null);
                mRemoteSettings.copyFrom(mSettings);

                if (result != null) {
                    HashMap<String, Object> properties = new HashMap<>();
                    if (result instanceof Map) {
                        Map<String, Object> resultMap = (Map) result;
                        Set<String> keys = resultMap.keySet();
                        for (String key : keys) {
                            Object currentValue = resultMap.get(key);
                            if (currentValue != null) {
                                properties.put(SAVED_ITEM_PREFIX + key, currentValue);
                            }
                        }
                    }
                    AnalyticsUtils.trackWithCurrentBlogDetails(
                            AnalyticsTracker.Stat.SITE_SETTINGS_SAVED_REMOTELY, properties);
                }
            }

            @Override
            public void onFailure(long id, final Exception error) {
                notifySavedOnUiThread(error);
            }
        };
        final Object[] callParams = {
                mBlog.getRemoteBlogId(), mSettings.username, mSettings.password, params
        };

        XMLRPCClientInterface xmlrpcInterface = instantiateInterface();
        if (xmlrpcInterface == null) return;
        xmlrpcInterface.callAsync(callback, Method.SET_OPTIONS, callParams);
    }

    /**
     * Request remote site data via XML-RPC.
     */
    @Override
    protected void fetchRemoteData() {
        new Thread() {
            @Override
            public void run() {
                Object[] params = {mBlog.getRemoteBlogId(), mBlog.getUsername(), mBlog.getPassword()};

                // Need two interfaces or the first call gets aborted
                instantiateInterface().callAsync(mOptionsCallback, Method.GET_OPTIONS, params);
                instantiateInterface().callAsync(mCategoriesCallback, Method.GET_CATEGORIES, params);
            }
        }.start();
    }

    /**
     * Handles response to fetching self-hosted site categories via XML-RPC.
     */
    private final XMLRPCCallback mCategoriesCallback = new XMLRPCCallback() {
        @Override
        public void onSuccess(long id, Object result) {
            if (result instanceof Object[]) {
                AppLog.d(AppLog.T.API, "Received Categories XML-RPC response.");
                credentialsVerified(true);

                mRemoteSettings.localTableId = mBlog.getRemoteBlogId();
                deserializeCategoriesResponse(mRemoteSettings, (Object[]) result);
                mSettings.categories = mRemoteSettings.categories;
                SiteSettingsTable.saveCategories(mSettings.categories);
                notifyUpdatedOnUiThread(null);
            } else {
                // Response is considered an error if we are unable to parse it
                AppLog.w(AppLog.T.API, "Error parsing Categories XML-RPC response: " + result);
                notifyUpdatedOnUiThread(new XMLRPCException("Unknown response object"));
            }
        }

        @Override
        public void onFailure(long id, Exception error) {
            AppLog.w(AppLog.T.API, "Error Categories XML-RPC response: " + error);
            notifyUpdatedOnUiThread(error);
        }
    };

    /**
     * Handles response to fetching self-hosted site options via XML-RPC.
     */
    private final XMLRPCCallback mOptionsCallback = new XMLRPCCallback() {
        @Override
        public void onSuccess(long id, final Object result) {
            if (result instanceof Map) {
                AppLog.d(AppLog.T.API, "Received Options XML-RPC response.");

                if (!versionSupported((Map) result) && mActivity != null) {
                    notifyUpdatedOnUiThread(new XMLRPCException(mActivity.getString(R.string.site_settings_unsupported_version_error)));
                    return;
                }

                credentialsVerified(true);

                deserializeOptionsResponse(mRemoteSettings, (Map) result);

                // postFormats setting is not returned by this api call so copy it over
                final Map<String, String> currentPostFormats = mSettings.postFormats;

                mSettings.copyFrom(mRemoteSettings);

                mSettings.postFormats = currentPostFormats;

                SiteSettingsTable.saveSettings(mSettings);
                notifyUpdatedOnUiThread(null);
            } else {
                // Response is considered an error if we are unable to parse it
                AppLog.w(AppLog.T.API, "Error parsing Options XML-RPC response: " + result);
                notifyUpdatedOnUiThread(new XMLRPCException("Unknown response object"));
            }
        }

        @Override
        public void onFailure(long id, final Exception error) {
            AppLog.w(AppLog.T.API, "Error Options XML-RPC response: " + error);
            notifyUpdatedOnUiThread(error);
        }
    };

    private boolean versionSupported(Map map) {
        String version = getNestedMapValue(map, SOFTWARE_VERSION_KEY);
        if (TextUtils.isEmpty(version)) return false;
        String[] split = version.split("\\.");
        return split.length > 0 &&
                Integer.valueOf(split[0]) >= REQUIRED_MAJOR_VERSION &&
                Integer.valueOf(split[1]) >= REQUIRED_MINOR_VERSION;
    }

    private Map<String, String> serializeSelfHostedParams() {
        Map<String, String> params = new HashMap<>();

        if (mSettings.title != null && !mSettings.title.equals(mRemoteSettings.title)) {
            params.put(BLOG_TITLE_KEY, mSettings.title);
        }
        if (mSettings.tagline != null && !mSettings.tagline.equals(mRemoteSettings.tagline)) {
            params.put(BLOG_TAGLINE_KEY, mSettings.tagline);
        }
        if (mSettings.privacy != mRemoteSettings.privacy) {
            params.put(PRIVACY_KEY, String.valueOf(mSettings.privacy));
        }
        if (mSettings.defaultCategory != mRemoteSettings.defaultCategory) {
            params.put(DEF_CATEGORY_KEY, String.valueOf(mSettings.defaultCategory));
        }
        if (mSettings.defaultPostFormat != null && !mSettings.defaultPostFormat.equals(mRemoteSettings.defaultPostFormat)) {
            params.put(DEF_POST_FORMAT_KEY, mSettings.defaultPostFormat);
        }
        if (mSettings.allowComments != mRemoteSettings.allowComments) {
            params.put(ALLOW_COMMENTS_KEY, String.valueOf(mSettings.allowComments));
        }
        if (mSettings.sendPingbacks != mRemoteSettings.sendPingbacks) {
            params.put(SEND_PINGBACKS_KEY, mSettings.sendPingbacks ? "1" : "0");
        }
        if (mSettings.receivePingbacks != mRemoteSettings.receivePingbacks) {
            params.put(RECEIVE_PINGBACKS_KEY, mSettings.receivePingbacks ? OPTION_ALLOWED : OPTION_DISALLOWED);
        }
        if (mSettings.commentApprovalRequired != mRemoteSettings.commentApprovalRequired) {
            params.put(COMMENT_MODERATION_KEY, String.valueOf(mSettings.commentApprovalRequired));
        }
        if (mSettings.closeCommentAfter != mRemoteSettings.closeCommentAfter) {
            if (mSettings.closeCommentAfter <= 0) {
                params.put(CLOSE_OLD_COMMENTS_KEY, String.valueOf(0));
            } else {
                params.put(CLOSE_OLD_COMMENTS_KEY, String.valueOf(1));
                params.put(CLOSE_OLD_COMMENTS_DAYS_KEY, String.valueOf(mSettings.closeCommentAfter));
            }
        }
        if (mSettings.sortCommentsBy != mRemoteSettings.sortCommentsBy) {
            if (mSettings.sortCommentsBy == ASCENDING_SORT) {
                params.put(COMMENT_SORT_ORDER_KEY, "asc");
            } else if (mSettings.sortCommentsBy == DESCENDING_SORT) {
                params.put(COMMENT_SORT_ORDER_KEY, "desc");
            }
        }
        if (mSettings.threadingLevels != mRemoteSettings.threadingLevels) {
            if (mSettings.threadingLevels <= 1) {
                params.put(THREAD_COMMENTS_KEY, String.valueOf(0));
            } else {
                params.put(PAGE_COMMENTS_KEY, String.valueOf(1));
                params.put(THREAD_COMMENTS_DEPTH_KEY, String.valueOf(mSettings.threadingLevels));
            }
        }
        if (mSettings.commentsPerPage != mRemoteSettings.commentsPerPage) {
            if (mSettings.commentsPerPage <= 0) {
                params.put(PAGE_COMMENTS_KEY, String.valueOf(0));
            } else{
                params.put(PAGE_COMMENTS_KEY, String.valueOf(1));
                params.put(PAGE_COMMENT_COUNT_KEY, String.valueOf(mSettings.commentsPerPage));
            }
        }
        if (mSettings.commentsRequireIdentity != mRemoteSettings.commentsRequireIdentity) {
            params.put(REQUIRE_IDENTITY_KEY, String.valueOf(mSettings.commentsRequireIdentity ? 1 : 0));
        }
        if (mSettings.commentsRequireUserAccount != mRemoteSettings.commentsRequireUserAccount) {
            params.put(REQUIRE_USER_ACCOUNT_KEY, String.valueOf(mSettings.commentsRequireUserAccount ? 1 : 0));
        }
        if (mSettings.commentAutoApprovalKnownUsers != mRemoteSettings.commentAutoApprovalKnownUsers) {
            params.put(WHITELIST_KNOWN_USERS_KEY, String.valueOf(mSettings.commentAutoApprovalKnownUsers));
        }
        if (mSettings.maxLinks != mRemoteSettings.maxLinks) {
            params.put(MAX_LINKS_KEY, String.valueOf(mSettings.maxLinks));
        }
        if (mSettings.holdForModeration != null && !mSettings.holdForModeration.equals(mRemoteSettings.holdForModeration)) {
            StringBuilder builder = new StringBuilder();
            for (String key : mSettings.holdForModeration) {
                builder.append(key);
                builder.append("\n");
            }
            if (builder.length() > 1) {
                params.put(MODERATION_KEYS_KEY, builder.substring(0, builder.length() - 1));
            } else {
                params.put(MODERATION_KEYS_KEY, "");
            }
        }
        if (mSettings.blacklist != null && !mSettings.blacklist.equals(mRemoteSettings.blacklist)) {
            StringBuilder builder = new StringBuilder();
            for (String key : mSettings.blacklist) {
                builder.append(key);
                builder.append("\n");
            }
            if (builder.length() > 1) {
                params.put(BLACKLIST_KEYS_KEY, builder.substring(0, builder.length() - 1));
            } else {
                params.put(BLACKLIST_KEYS_KEY, "");
            }
        }

        return params;
    }

    /**
     * Sets values from a self-hosted XML-RPC response object.
     */
    private void deserializeOptionsResponse(SiteSettingsModel model, Map response) {
        if (mBlog == null || response == null) return;

        model.username = mBlog.getUsername();
        model.password = mBlog.getPassword();
        model.address = getNestedMapValue(response, BLOG_URL_KEY);
        model.title = getNestedMapValue(response, BLOG_TITLE_KEY);
        model.tagline = getNestedMapValue(response, BLOG_TAGLINE_KEY);
        model.privacy = Integer.valueOf(getNestedMapValue(response, PRIVACY_KEY));
        model.defaultCategory = Integer.valueOf(getNestedMapValue(response, DEF_CATEGORY_KEY));
        model.defaultPostFormat = getNestedMapValue(response, DEF_POST_FORMAT_KEY);
        model.allowComments = OPTION_ALLOWED.equals(getNestedMapValue(response, ALLOW_COMMENTS_KEY));
        model.receivePingbacks = OPTION_ALLOWED.equals(getNestedMapValue(response, RECEIVE_PINGBACKS_KEY));
        String sendPingbacks = getNestedMapValue(response, SEND_PINGBACKS_KEY);
        String approvalRequired = getNestedMapValue(response, COMMENT_MODERATION_KEY);
        String identityRequired = getNestedMapValue(response, REQUIRE_IDENTITY_KEY);
        String accountRequired = getNestedMapValue(response, REQUIRE_USER_ACCOUNT_KEY);
        String knownUsers = getNestedMapValue(response, WHITELIST_KNOWN_USERS_KEY);
        model.sendPingbacks = !TextUtils.isEmpty(sendPingbacks) && Integer.valueOf(sendPingbacks) > 0;
        model.commentApprovalRequired = !TextUtils.isEmpty(approvalRequired) && Boolean.valueOf(approvalRequired);
        model.commentsRequireIdentity = !TextUtils.isEmpty(identityRequired) && Integer.valueOf(identityRequired) > 0;
        model.commentsRequireUserAccount = !TextUtils.isEmpty(accountRequired) && Integer.valueOf(identityRequired) > 0;
        model.commentAutoApprovalKnownUsers = !TextUtils.isEmpty(knownUsers) && Boolean.valueOf(knownUsers);
        model.maxLinks = Integer.valueOf(getNestedMapValue(response, MAX_LINKS_KEY));
        mRemoteSettings.holdForModeration = new ArrayList<>();
        mRemoteSettings.blacklist = new ArrayList<>();

        String modKeys = getNestedMapValue(response, MODERATION_KEYS_KEY);
        if (modKeys.length() > 0) {
            Collections.addAll(mRemoteSettings.holdForModeration, modKeys.split("\n"));
        }
        String blacklistKeys = getNestedMapValue(response, BLACKLIST_KEYS_KEY);
        if (blacklistKeys.length() > 0) {
            Collections.addAll(mRemoteSettings.blacklist, blacklistKeys.split("\n"));
        }

        String close = getNestedMapValue(response, CLOSE_OLD_COMMENTS_KEY);
        if (!TextUtils.isEmpty(close) && Boolean.valueOf(close)) {
            mRemoteSettings.closeCommentAfter = Integer.valueOf(getNestedMapValue(response, CLOSE_OLD_COMMENTS_DAYS_KEY));
        } else {
            mRemoteSettings.closeCommentAfter = 0;
        }

        String thread = getNestedMapValue(response, THREAD_COMMENTS_KEY);
        if (!TextUtils.isEmpty(thread) && Integer.valueOf(thread) > 0) {
            mRemoteSettings.threadingLevels = Integer.valueOf(getNestedMapValue(response, THREAD_COMMENTS_DEPTH_KEY));
        } else {
            mRemoteSettings.threadingLevels = 0;
        }

        String page = getNestedMapValue(response, PAGE_COMMENTS_KEY);
        if (!TextUtils.isEmpty(page) && Boolean.valueOf(page)) {
            mRemoteSettings.commentsPerPage = Integer.valueOf(getNestedMapValue(response, PAGE_COMMENT_COUNT_KEY));
        } else {
            mRemoteSettings.commentsPerPage = 0;
        }

        if (getNestedMapValue(response, COMMENT_SORT_ORDER_KEY).equals("asc")) {
            mRemoteSettings.sortCommentsBy = ASCENDING_SORT;
        } else {
            mRemoteSettings.sortCommentsBy = DESCENDING_SORT;
        }
    }

    private void deserializeCategoriesResponse(SiteSettingsModel model, Object[] response) {
        model.categories = new CategoryModel[response.length];

        for (int i = 0; i < response.length; ++i) {
            if (response[i] instanceof Map) {
                Map category = (Map) response[i];
                CategoryModel categoryModel = new CategoryModel();
                categoryModel.id = MapUtils.getMapInt(category, BLOG_CATEGORY_ID_KEY);
                categoryModel.parentId = MapUtils.getMapInt(category, BLOG_CATEGORY_PARENT_ID_KEY);
                categoryModel.description = MapUtils.getMapStr(category, BLOG_CATEGORY_DESCRIPTION_KEY);
                categoryModel.name = MapUtils.getMapStr(category, BLOG_CATEGORY_NAME_KEY);
                model.categories[i] = categoryModel;
            }
        }
    }

    /**
     * Helper method to get a value from a nested Map. Used to parse self-hosted response objects.
     */
    private String getNestedMapValue(Map map, String key) {
        if (map != null && key != null) {
            return MapUtils.getMapStr((Map) map.get(key), "value");
        }

        return "";
    }
}
