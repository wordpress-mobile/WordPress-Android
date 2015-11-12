package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.text.TextUtils;

import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.SiteSettingsModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MapUtils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class SelfHostedSiteSettings extends SiteSettingsInterface {
    // XML-RPC wp.getOptions keys
    private static final String BLOG_URL_KEY = "blog_url";
    private static final String BLOG_TITLE_KEY = "blog_title";
    private static final String BLOG_USERNAME_KEY = "username";
    private static final String BLOG_PASSWORD_KEY = "password";
    private static final String BLOG_TAGLINE_KEY = "blog_tagline";
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
    private static final String BLOG_CATEGORY_ID_KEY = "categoryId";
    private static final String BLOG_CATEGORY_PARENT_ID_KEY = "parentId";
    private static final String BLOG_CATEGORY_DESCRIPTION_KEY = "categoryDescription";
    private static final String BLOG_CATEGORY_NAME_KEY = "categoryName";

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
        mSettings.language = siteSettingsPreferences(mActivity).getString(LANGUAGE_PREF_KEY, Locale.getDefault().getLanguage());

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
        xmlrpcInterface.callAsync(callback, ApiHelper.Methods.SET_OPTIONS, callParams);
    }

    /**
     * Request remote site data via XML-RPC.
     */
    @Override
    protected void fetchRemoteData() {
        Object[] params = {mBlog.getRemoteBlogId(), mBlog.getUsername(), mBlog.getPassword()};

        // Need two interfaces or the first call gets aborted
        instantiateInterface().callAsync(mOptionsCallback, ApiHelper.Methods.GET_OPTIONS, params);
        instantiateInterface().callAsync(mCategoriesCallback, ApiHelper.Methods.GET_CATEGORIES, params);
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
                credentialsVerified(true);

                deserializeOptionsResponse(mRemoteSettings, (Map) result);
                mSettings.copyFrom(mRemoteSettings);
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

    private Map<String, String> serializeSelfHostedParams() {
        Map<String, String> params = new HashMap<>();

        if (mSettings.title != null && !mSettings.title.equals(mRemoteSettings.title)) {
            params.put(BLOG_TITLE_KEY, mSettings.title);
        }
        if (mSettings.tagline != null && !mSettings.tagline.equals(mRemoteSettings.tagline)) {
            params.put(BLOG_TAGLINE_KEY, mSettings.tagline);
        }
        if (mSettings.sendPingbacks != mRemoteSettings.sendPingbacks) {
            params.put(SEND_PINGBACKS_KEY, mSettings.sendPingbacks ? OPTION_ALLOWED : OPTION_DISALLOWED);
        }
        if (mSettings.receivePingbacks != mRemoteSettings.receivePingbacks) {
            params.put(RECEIVE_PINGBACKS_KEY, mSettings.receivePingbacks ? OPTION_ALLOWED : OPTION_DISALLOWED);
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
        model.sendPingbacks = Boolean.valueOf(getNestedMapValue(response, SEND_PINGBACKS_KEY));
        model.receivePingbacks = OPTION_ALLOWED.equals(getNestedMapValue(response, RECEIVE_PINGBACKS_KEY));
        String approvalRequired = getNestedMapValue(response, COMMENT_MODERATION_KEY);
        String identityRequired = getNestedMapValue(response, REQUIRE_IDENTITY_KEY);
        String accountRequired = getNestedMapValue(response, REQUIRE_USER_ACCOUNT_KEY);
        String knownUsers = getNestedMapValue(response, WHITELIST_KNOWN_USERS_KEY);
        model.commentApprovalRequired = !TextUtils.isEmpty(approvalRequired) && Integer.valueOf(approvalRequired) > 0;
        model.commentsRequireIdentity = !TextUtils.isEmpty(identityRequired) && Integer.valueOf(identityRequired) > 0;
        model.commentsRequireUserAccount = !TextUtils.isEmpty(accountRequired) && Integer.valueOf(accountRequired) > 0;
        model.commentAutoApprovalKnownUsers = !TextUtils.isEmpty(knownUsers) && Integer.valueOf(knownUsers) > 0;
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

        if (Boolean.valueOf(getNestedMapValue(response, CLOSE_OLD_COMMENTS_KEY))) {
            mRemoteSettings.closeCommentAfter = Integer.valueOf(getNestedMapValue(response, CLOSE_OLD_COMMENTS_DAYS_KEY));
        } else {
            mRemoteSettings.closeCommentAfter = 0;
        }

        if (Boolean.valueOf(getNestedMapValue(response, THREAD_COMMENTS_KEY))) {
            mRemoteSettings.threadingLevels = Integer.valueOf(getNestedMapValue(response, THREAD_COMMENTS_DEPTH_KEY));
        } else {
            mRemoteSettings.threadingLevels = 0;
        }

        if (Boolean.valueOf(getNestedMapValue(response, PAGE_COMMENTS_KEY))) {
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
