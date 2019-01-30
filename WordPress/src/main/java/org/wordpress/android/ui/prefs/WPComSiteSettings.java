package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.JetpackSettingsModel;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class WPComSiteSettings extends SiteSettingsInterface {
    // WP.com REST keys used in response to a settings GET and POST request
    private static final String LANGUAGE_ID_KEY = "lang_id";
    private static final String SITE_ICON_KEY = "site_icon";
    private static final String PRIVACY_KEY = "blog_public";
    private static final String URL_KEY = "URL";
    private static final String DEF_CATEGORY_KEY = "default_category";
    private static final String DEF_POST_FORMAT_KEY = "default_post_format";
    private static final String RELATED_POSTS_ALLOWED_KEY = "jetpack_relatedposts_allowed";
    private static final String RELATED_POSTS_ENABLED_KEY = "jetpack_relatedposts_enabled";
    private static final String RELATED_POSTS_HEADER_KEY = "jetpack_relatedposts_show_headline";
    private static final String RELATED_POSTS_IMAGES_KEY = "jetpack_relatedposts_show_thumbnails";
    private static final String ALLOW_COMMENTS_KEY = "default_comment_status";
    private static final String SEND_PINGBACKS_KEY = "default_pingback_flag";
    private static final String RECEIVE_PINGBACKS_KEY = "default_ping_status";
    private static final String CLOSE_OLD_COMMENTS_KEY = "close_comments_for_old_posts";
    private static final String CLOSE_OLD_COMMENTS_DAYS_KEY = "close_comments_days_old";
    private static final String THREAD_COMMENTS_KEY = "thread_comments";
    private static final String THREAD_COMMENTS_DEPTH_KEY = "thread_comments_depth";
    private static final String PAGE_COMMENTS_KEY = "page_comments";
    private static final String PAGE_COMMENT_COUNT_KEY = "comments_per_page";
    private static final String COMMENT_SORT_ORDER_KEY = "comment_order";
    private static final String COMMENT_MODERATION_KEY = "comment_moderation";
    private static final String REQUIRE_IDENTITY_KEY = "require_name_email";
    private static final String REQUIRE_USER_ACCOUNT_KEY = "comment_registration";
    private static final String WHITELIST_KNOWN_USERS_KEY = "comment_whitelist";
    private static final String MAX_LINKS_KEY = "comment_max_links";
    private static final String MODERATION_KEYS_KEY = "moderation_keys";
    private static final String BLACKLIST_KEYS_KEY = "blacklist_keys";
    private static final String SHARING_LABEL_KEY = "sharing_label";
    private static final String SHARING_BUTTON_STYLE_KEY = "sharing_button_style";
    private static final String SHARING_REBLOGS_DISABLED_KEY = "disabled_reblogs";
    private static final String SHARING_LIKES_DISABLED_KEY = "disabled_likes";
    private static final String SHARING_COMMENT_LIKES_KEY = "jetpack_comment_likes_enabled";
    private static final String TWITTER_USERNAME_KEY = "twitter_via";
    private static final String JP_MONITOR_EMAIL_NOTES_KEY = "email_notifications";
    private static final String JP_MONITOR_WP_NOTES_KEY = "wp_note_notifications";
    private static final String JP_PROTECT_WHITELIST_KEY = "jetpack_protect_whitelist";
    // Jetpack modules
    private static final String SERVE_IMAGES_FROM_OUR_SERVERS = "photon";
    private static final String LAZY_LOAD_IMAGES = "lazy-images";
    private static final String SHARING_MODULE = "sharedaddy";

    private static final String START_OF_WEEK_KEY = "start_of_week";
    private static final String DATE_FORMAT_KEY = "date_format";
    private static final String TIME_FORMAT_KEY = "time_format";
    private static final String TIMEZONE_KEY = "timezone_string";
    private static final String POSTS_PER_PAGE_KEY = "posts_per_page";
    private static final String AMP_SUPPORTED_KEY = "amp_is_supported";
    private static final String AMP_ENABLED_KEY = "amp_is_enabled";
    private static final String COMMENT_LIKES = "comment-likes";

    // WP.com REST keys used to GET certain site settings
    private static final String GET_TITLE_KEY = "name";
    private static final String GET_DESC_KEY = "description";

    // WP.com REST keys used to POST updates to site settings
    private static final String SET_TITLE_KEY = "blogname";
    private static final String SET_DESC_KEY = "blogdescription";

    // WP.com REST keys used in response to a categories GET request
    private static final String CAT_ID_KEY = "ID";
    private static final String CAT_NAME_KEY = "name";
    private static final String CAT_SLUG_KEY = "slug";
    private static final String CAT_DESC_KEY = "description";
    private static final String CAT_PARENT_ID_KEY = "parent";
    private static final String CAT_POST_COUNT_KEY = "post_count";
    private static final String CAT_NUM_POSTS_KEY = "found";
    private static final String CATEGORIES_KEY = "categories";
    private static final String DEFAULT_SHARING_BUTTON_STYLE = "icon-only";

    private static final String SPEED_UP_SETTINGS_JETPACK_VERSION = "5.8";
    private static final String ACTIVE = "active";

    // used to track network fetches to prevent multiple errors from generating multiple toasts
    private int mFetchRequestCount = 0;
    private int mSaveRequestCount = 0;
    private boolean mWasFetchError = false;
    private boolean mWasSaveError = false;
    private Exception mFetchError = null;
    private Exception mSaveError = null;

    /**
     * Only instantiated by {@link SiteSettingsInterface}.
     */
    WPComSiteSettings(Context host, SiteModel site, SiteSettingsListener listener) {
        super(host, site, listener);
    }

    @Override
    public void saveSettings() {
        super.saveSettings();

        // save any Jetpack changes
        if (mSite.isJetpackConnected()) {
            pushJetpackMonitorSettings();
            pushJetpackProtectAndSsoSettings();
            if (supportsJetpackSpeedUpSettings(mSite)) {
                pushServeImagesFromOurServersModuleSettings();
                pushLazyLoadModule();
            }
        }

        pushWpSettings();
    }

    /**
     * Request remote site data via the WordPress REST API.
     */
    @Override
    protected void fetchRemoteData() {
        if (mFetchRequestCount > 0) {
            AppLog.v(AppLog.T.SETTINGS, "Network fetch prevented, there's already a fetch in progress.");
            return;
        }

        fetchCategories();
        fetchWpSettings();

        if (mSite.isJetpackConnected()) {
            fetchJetpackSettings();
        }
    }

    static boolean supportsJetpackSpeedUpSettings(SiteModel site) {
        return SiteUtils.checkMinimalJetpackVersion(site, SPEED_UP_SETTINGS_JETPACK_VERSION);
    }

    private void fetchWpSettings() {
        ++mFetchRequestCount;
        WordPress.getRestClientUtilsV1_1().getGeneralSettings(
                mSite.getSiteId(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Received response to Settings REST request.");
                        credentialsVerified(true);

                        mRemoteSettings.localTableId = mSite.getId();
                        deserializeWpComRestResponse(mSite, response);
                        if (!mRemoteSettings.equals(mSettings)) {
                            // postFormats setting is not returned by this api call so copy it over
                            final Map<String, String> currentPostFormats = mSettings.postFormats;

                            // Local settings
                            boolean location = mSettings.location;
                            mSettings.copyFrom(mRemoteSettings);
                            mSettings.postFormats = currentPostFormats;
                            mSettings.location = location;

                            SiteSettingsTable.saveSettings(mSettings);
                        }
                        onFetchResponseReceived(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error response to Settings REST request: " + error);
                        onFetchResponseReceived(error);
                    }
                });
    }

    /**
     * Request a list of post categories for a site via the WordPress REST API.
     */
    private void fetchCategories() {
        ++mFetchRequestCount;
        // TODO: Replace with FluxC (GET_CATEGORIES + TaxonomyStore.getCategoriesForSite())
        WordPress.getRestClientUtilsV1_1().getCategories(mSite.getSiteId(),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Received site Categories");
                        credentialsVerified(true);

                        CategoryModel[] models = deserializeCategoryRestResponse(response);
                        if (models == null) return;

                        SiteSettingsTable.saveCategories(models);
                        mRemoteSettings.categories = models;
                        mSettings.categories = models;
                        onFetchResponseReceived(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.d(AppLog.T.API, "Error fetching WP.com categories:" + error);
                        onFetchResponseReceived(error);
                    }
                });
    }

    private void fetchJetpackSettings() {
        fetchJetpackMonitorSettings();
        fetchJetpackProtectAndSsoSettings();
        fetchJetpackModuleSettings();
    }

    private void fetchJetpackProtectAndSsoSettings() {
        ++mFetchRequestCount;
        WordPress.getRestClientUtilsV1_1().getJetpackSettings(mSite.getSiteId(), new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                final JSONObject data = response.optJSONObject("data");

                if (data == null) {
                    AppLog.w(AppLog.T.API, "Unexpected state: Received empty Jetpack settings response");
                    onFetchResponseReceived(null);
                    return;
                }

                AppLog.v(AppLog.T.API, "Received Jetpack settings response");

                mRemoteJpSettings.monitorActive = data.optBoolean("monitor", false);
                mRemoteJpSettings.jetpackProtectEnabled = data.optBoolean("protect", false);
                mRemoteJpSettings.ssoActive = data.optBoolean("sso", false);
                mRemoteJpSettings.ssoMatchEmail = data.optBoolean("jetpack_sso_match_by_email", false);
                mRemoteJpSettings.ssoRequireTwoFactor = data.optBoolean("jetpack_sso_require_two_step", false);
                mRemoteJpSettings.commentLikes = data.optBoolean(COMMENT_LIKES, false);

                JSONObject jetpackProtectWhitelist = data.optJSONObject("jetpack_protect_global_whitelist");
                if (jetpackProtectWhitelist != null) {
                    // clear existing whitelist entries before adding items from response
                    mRemoteJpSettings.jetpackProtectWhitelist.clear();

                    JSONArray whitelistItems = jetpackProtectWhitelist.optJSONArray("local");
                    if (whitelistItems != null) {
                        for (int i = 0; i < whitelistItems.length(); ++i) {
                            String item = whitelistItems.optString(i, "");
                            if (!item.isEmpty() && !mRemoteJpSettings.jetpackProtectWhitelist.contains(item)) {
                                mRemoteJpSettings.jetpackProtectWhitelist.add(item);
                            }
                        }
                    }
                }

                mJpSettings.monitorActive = mRemoteJpSettings.monitorActive;
                mJpSettings.jetpackProtectEnabled = mRemoteJpSettings.jetpackProtectEnabled;
                mJpSettings.jetpackProtectWhitelist.clear();
                mJpSettings.jetpackProtectWhitelist.addAll(mRemoteJpSettings.jetpackProtectWhitelist);
                mJpSettings.ssoActive = mRemoteJpSettings.ssoActive;
                mJpSettings.ssoMatchEmail = mRemoteJpSettings.ssoMatchEmail;
                mJpSettings.ssoRequireTwoFactor = mRemoteJpSettings.ssoRequireTwoFactor;
                mJpSettings.commentLikes = mRemoteJpSettings.commentLikes;
                onFetchResponseReceived(null);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppLog.w(AppLog.T.API, "Error fetching Jetpack settings: " + error);
                onFetchResponseReceived(error);
            }
        });
    }

    private void fetchJetpackMonitorSettings() {
        ++mFetchRequestCount;
        WordPress.getRestClientUtilsV1_1().getJetpackMonitorSettings(
                mSite.getSiteId(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Received Jetpack Monitor module options");
                        mRemoteJpSettings.localTableId = mSite.getId();
                        deserializeJetpackRestResponse(mSite, response);
                        mJpSettings.localTableId = mRemoteJpSettings.localTableId;
                        mJpSettings.emailNotifications = mRemoteJpSettings.emailNotifications;
                        mJpSettings.wpNotifications = mRemoteJpSettings.wpNotifications;
                        onFetchResponseReceived(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error fetching Jetpack Monitor module options: " + error);
                        onFetchResponseReceived(error);
                    }
                });
    }

    private void fetchJetpackModuleSettings() {
        ++mFetchRequestCount;
        WordPress.getRestClientUtilsV1_1().getJetpackModuleSettings(
                mSite.getSiteId(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response == null) {
                            AppLog.w(AppLog.T.API, "Unexpected state: Received empty Jetpack modules response");
                            onFetchResponseReceived(null);
                            return;
                        }
                        AppLog.v(AppLog.T.API, "Received Jetpack module settings");
                        JSONArray array = response.optJSONArray("modules");
                        if (array != null) {
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject module = array.optJSONObject(i);
                                if (module == null) {
                                    continue;
                                }
                                String id = module.optString("id");
                                if (id == null) {
                                    continue;
                                }
                                boolean isActive = module.optBoolean(ACTIVE, false);
                                switch (id) {
                                    case SERVE_IMAGES_FROM_OUR_SERVERS:
                                        mRemoteJpSettings.serveImagesFromOurServers = isActive;
                                        break;
                                    case LAZY_LOAD_IMAGES:
                                        mRemoteJpSettings.lazyLoadImages = isActive;
                                        break;
                                    case SHARING_MODULE:
                                        mRemoteJpSettings.sharingEnabled = isActive;
                                        break;
                                }
                            }
                            mJpSettings.serveImagesFromOurServers = mRemoteJpSettings.serveImagesFromOurServers;
                            mJpSettings.lazyLoadImages = mRemoteJpSettings.lazyLoadImages;
                            mJpSettings.sharingEnabled = mRemoteJpSettings.sharingEnabled;
                        }
                        onFetchResponseReceived(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error fetching Jetpack module settings: " + error);
                        onFetchResponseReceived(error);
                    }
                });
    }

    private void pushWpSettings() {
        JSONObject jsonParams;
        try {
            jsonParams = serializeWpComParamsToJSONObject();
            // skip network requests if there are no changes
            if (jsonParams.length() <= 0) {
                return;
            }
        } catch (JSONException exception) {
            AppLog.w(AppLog.T.API, "Error serializing settings changes: " + exception);
            notifySaveErrorOnUiThread(exception);
            return;
        }

        ++mSaveRequestCount;
        WordPress.getRestClientUtilsV1_1().setGeneralSiteSettings(
                mSite.getSiteId(), jsonParams, new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Site Settings saved remotely");
                        mRemoteSettings.copyFrom(mSettings);

                        if (response != null) {
                            JSONObject updated = response.optJSONObject("updated");
                            if (updated == null) return;
                            HashMap<String, Object> properties = new HashMap<>();
                            Iterator<String> keys = updated.keys();
                            while (keys.hasNext()) {
                                String currentKey = keys.next();
                                Object currentValue = updated.opt(currentKey);
                                if (currentValue != null) {
                                    properties.put(SAVED_ITEM_PREFIX + currentKey, currentValue);
                                }
                            }
                            AnalyticsUtils.trackWithSiteDetails(
                                    AnalyticsTracker.Stat.SITE_SETTINGS_SAVED_REMOTELY, mSite, properties);
                        }
                        onSaveResponseReceived(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error POSTing site settings changes: " + error);
                        onSaveResponseReceived(error);
                    }
                });
    }

    private void pushJetpackProtectAndSsoSettings() {
        final Map<String, Object> params = serializeJetpackProtectAndSsoParams();
        if (params.isEmpty()) {
            AppLog.v(AppLog.T.API, "No Jetpack settings changes detected. Skipping network POST call.");
            return;
        }

        // The response object doesn't contain any relevant info so we have to create a copy of values
        // being sent over the network in case mJpSettings is modified while awaiting response
        final JetpackSettingsModel sentJpData = new JetpackSettingsModel(mJpSettings);
        ++mSaveRequestCount;
        WordPress.getRestClientUtilsV1_1().setJetpackSettings(mSite.getSiteId(), params,
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Jetpack settings updated");
                        mRemoteJpSettings.monitorActive = sentJpData.monitorActive;
                        mRemoteJpSettings.jetpackProtectEnabled = sentJpData.jetpackProtectEnabled;
                        mRemoteJpSettings.jetpackProtectWhitelist.clear();
                        mRemoteJpSettings.jetpackProtectWhitelist.addAll(sentJpData.jetpackProtectWhitelist);
                        mRemoteJpSettings.ssoActive = sentJpData.ssoActive;
                        mRemoteJpSettings.ssoMatchEmail = sentJpData.ssoMatchEmail;
                        mRemoteJpSettings.ssoRequireTwoFactor = sentJpData.ssoRequireTwoFactor;
                        mRemoteJpSettings.commentLikes = sentJpData.commentLikes;
                        onSaveResponseReceived(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error updating Jetpack settings: " + error);
                        onSaveResponseReceived(error);
                    }
                });
    }

    private void pushJetpackMonitorSettings() {
        // The response object doesn't contain any relevant info so we have to create a copy of values
        // being sent over the network in case mJpSettings is modified while awaiting response
        final JetpackSettingsModel sentJpData = new JetpackSettingsModel(mJpSettings);
        ++mSaveRequestCount;
        WordPress.getRestClientUtilsV1_1().setJetpackMonitorSettings(
                mSite.getSiteId(), serializeJetpackMonitorParams(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Jetpack Monitor module updated");
                        mRemoteJpSettings.emailNotifications = sentJpData.emailNotifications;
                        mRemoteJpSettings.wpNotifications = sentJpData.wpNotifications;
                        onSaveResponseReceived(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error updating Jetpack Monitor module: " + error.getMessage());
                        onSaveResponseReceived(error);
                    }
                });
    }

    private void pushServeImagesFromOurServersModuleSettings() {
        ++mSaveRequestCount;
        // The API returns 400 if we try to sync the same value twice so we need to keep it locally.
        if (mJpSettings.serveImagesFromOurServers != mRemoteJpSettings.serveImagesFromOurServers) {
            final boolean fallbackValue = mRemoteJpSettings.serveImagesFromOurServers;
            mRemoteJpSettings.serveImagesFromOurServers = mJpSettings.serveImagesFromOurServers;
            WordPress.getRestClientUtilsV1_1().setJetpackModuleSettings(
                    mSite.getSiteId(), SERVE_IMAGES_FROM_OUR_SERVERS, mJpSettings.serveImagesFromOurServers,
                    new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            AppLog.d(AppLog.T.API, "Jetpack module updated - Serve images from our servers");
                            onSaveResponseReceived(null);
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            mRemoteJpSettings.serveImagesFromOurServers = fallbackValue;
                            error.printStackTrace();
                            AppLog.w(AppLog.T.API,
                                     "Error updating Jetpack module - Serve images from our servers: " + error);
                            onSaveResponseReceived(error);
                        }
                    });
        }
    }

    private void pushLazyLoadModule() {
        ++mSaveRequestCount;
        // The API returns 400 if we try to sync the same value twice so we need to keep it locally.
        if (mJpSettings.lazyLoadImages != mRemoteJpSettings.lazyLoadImages) {
            final boolean fallbackValue = mRemoteJpSettings.lazyLoadImages;
            mRemoteJpSettings.lazyLoadImages = mJpSettings.lazyLoadImages;
            WordPress.getRestClientUtilsV1_1().setJetpackModuleSettings(
                    mSite.getSiteId(), LAZY_LOAD_IMAGES, mJpSettings.lazyLoadImages, new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            AppLog.d(AppLog.T.API, "Jetpack module updated - Lazy load images");
                            onSaveResponseReceived(null);
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            mRemoteJpSettings.lazyLoadImages = fallbackValue;
                            error.printStackTrace();
                            AppLog.w(AppLog.T.API, "Error updating Jetpack module - Lazy load images: " + error);
                            onSaveResponseReceived(error);
                        }
                    });
        }
    }

    private void onFetchResponseReceived(Exception error) {
        if (error != null) {
            mWasFetchError = true;
            mFetchError = error;
        } else {
            // we received successful response to GET request, notify listener to update UI
            notifyUpdatedOnUiThread();
        }
        if (--mFetchRequestCount <= 0 && mWasFetchError) {
            // all of our GET requests are completed and at least one had an error so we need to notify
            notifyFetchErrorOnUiThread(mFetchError);
            mWasFetchError = false;
            mFetchError = null;
        }
    }

    private void onSaveResponseReceived(Exception error) {
        if (error != null) {
            mWasSaveError = true;
            mSaveError = error;
        } else {
            // we received successful response to POST request, notify listener to update UI
            notifySavedOnUiThread();
        }
        if (--mSaveRequestCount <= 0 && mWasSaveError) {
            // all of our POST requests are completed and at least one had an error so we need to notify
            notifySaveErrorOnUiThread(mSaveError);
            mWasSaveError = false;
            mSaveError = null;
        }
    }

    /**
     * Sets values from a .com REST response object.
     */
    private void deserializeWpComRestResponse(SiteModel site, JSONObject response) {
        if (site == null || response == null) return;
        JSONObject settingsObject = response.optJSONObject("settings");

        mRemoteSettings.username = site.getUsername();
        mRemoteSettings.password = site.getPassword();
        mRemoteSettings.address = response.optString(URL_KEY, "");
        mRemoteSettings.title = response.optString(GET_TITLE_KEY, "");
        mRemoteSettings.tagline = response.optString(GET_DESC_KEY, "");
        mRemoteSettings.languageId = settingsObject.optInt(LANGUAGE_ID_KEY, -1);
        mRemoteSettings.siteIconMediaId = settingsObject.optInt(SITE_ICON_KEY, 0);
        mRemoteSettings.privacy = settingsObject.optInt(PRIVACY_KEY, -2);
        mRemoteSettings.defaultCategory = settingsObject.optInt(DEF_CATEGORY_KEY, 1);
        mRemoteSettings.defaultPostFormat = settingsObject.optString(DEF_POST_FORMAT_KEY, STANDARD_POST_FORMAT_KEY);
        mRemoteSettings.language = languageIdToLanguageCode(Integer.toString(mRemoteSettings.languageId));
        mRemoteSettings.allowComments = settingsObject.optBoolean(ALLOW_COMMENTS_KEY, true);
        mRemoteSettings.sendPingbacks = settingsObject.optBoolean(SEND_PINGBACKS_KEY, false);
        mRemoteSettings.receivePingbacks = settingsObject.optBoolean(RECEIVE_PINGBACKS_KEY, true);
        mRemoteSettings.shouldCloseAfter = settingsObject.optBoolean(CLOSE_OLD_COMMENTS_KEY, false);
        mRemoteSettings.closeCommentAfter = settingsObject.optInt(CLOSE_OLD_COMMENTS_DAYS_KEY, 0);
        mRemoteSettings.shouldThreadComments = settingsObject.optBoolean(THREAD_COMMENTS_KEY, false);
        mRemoteSettings.threadingLevels = settingsObject.optInt(THREAD_COMMENTS_DEPTH_KEY, 0);
        mRemoteSettings.shouldPageComments = settingsObject.optBoolean(PAGE_COMMENTS_KEY, false);
        mRemoteSettings.commentsPerPage = settingsObject.optInt(PAGE_COMMENT_COUNT_KEY, 0);
        mRemoteSettings.commentApprovalRequired = settingsObject.optBoolean(COMMENT_MODERATION_KEY, false);
        mRemoteSettings.commentsRequireIdentity = settingsObject.optBoolean(REQUIRE_IDENTITY_KEY, false);
        mRemoteSettings.commentsRequireUserAccount = settingsObject.optBoolean(REQUIRE_USER_ACCOUNT_KEY, true);
        mRemoteSettings.commentAutoApprovalKnownUsers = settingsObject.optBoolean(WHITELIST_KNOWN_USERS_KEY, false);
        mRemoteSettings.maxLinks = settingsObject.optInt(MAX_LINKS_KEY, 0);
        mRemoteSettings.holdForModeration = new ArrayList<>();
        mRemoteSettings.blacklist = new ArrayList<>();
        mRemoteSettings.sharingLabel = settingsObject.optString(SHARING_LABEL_KEY, "");
        mRemoteSettings.sharingButtonStyle = settingsObject.optString(SHARING_BUTTON_STYLE_KEY,
                                                                      DEFAULT_SHARING_BUTTON_STYLE);
        mRemoteSettings.allowCommentLikes = settingsObject.optBoolean(SHARING_COMMENT_LIKES_KEY, false);
        mRemoteSettings.twitterUsername = settingsObject.optString(TWITTER_USERNAME_KEY, "");
        mRemoteSettings.startOfWeek = settingsObject.optString(START_OF_WEEK_KEY, "");
        mRemoteSettings.dateFormat = settingsObject.optString(DATE_FORMAT_KEY, "");
        mRemoteSettings.timeFormat = settingsObject.optString(TIME_FORMAT_KEY, "");
        mRemoteSettings.timezone = settingsObject.optString(TIMEZONE_KEY, "");
        mRemoteSettings.postsPerPage = settingsObject.optInt(POSTS_PER_PAGE_KEY, 0);
        mRemoteSettings.ampSupported = settingsObject.optBoolean(AMP_SUPPORTED_KEY, false);
        mRemoteSettings.ampEnabled = settingsObject.optBoolean(AMP_ENABLED_KEY, false);

        boolean reblogsDisabled = settingsObject.optBoolean(SHARING_REBLOGS_DISABLED_KEY, false);
        boolean likesDisabled = settingsObject.optBoolean(SHARING_LIKES_DISABLED_KEY, false);
        mRemoteSettings.allowReblogButton = !reblogsDisabled;
        mRemoteSettings.allowLikeButton = !likesDisabled;

        String modKeys = settingsObject.optString(MODERATION_KEYS_KEY, "");
        if (modKeys.length() > 0) {
            Collections.addAll(mRemoteSettings.holdForModeration, modKeys.split("\n"));
        }
        String blacklistKeys = settingsObject.optString(BLACKLIST_KEYS_KEY, "");
        if (blacklistKeys.length() > 0) {
            Collections.addAll(mRemoteSettings.blacklist, blacklistKeys.split("\n"));
        }

        if (settingsObject.optString(COMMENT_SORT_ORDER_KEY, "").equals("asc")) {
            mRemoteSettings.sortCommentsBy = ASCENDING_SORT;
        } else {
            mRemoteSettings.sortCommentsBy = DESCENDING_SORT;
        }

        JSONObject jetpackProtectWhitelist = settingsObject.optJSONObject(JP_PROTECT_WHITELIST_KEY);
        if (jetpackProtectWhitelist != null) {
            JSONArray whitelistItems = jetpackProtectWhitelist.optJSONArray("local");
            if (whitelistItems != null) {
                for (int i = 0; i < whitelistItems.length(); ++i) {
                    String item = whitelistItems.optString(i, "");
                    if (!item.isEmpty() && !mRemoteJpSettings.jetpackProtectWhitelist.contains(item)) {
                        mRemoteJpSettings.jetpackProtectWhitelist.add(item);
                    }
                }
            }
        }

        if (settingsObject.optBoolean(RELATED_POSTS_ALLOWED_KEY, false)) {
            mRemoteSettings.showRelatedPosts = settingsObject.optBoolean(RELATED_POSTS_ENABLED_KEY, false);
            mRemoteSettings.showRelatedPostHeader = settingsObject.optBoolean(RELATED_POSTS_HEADER_KEY, false);
            mRemoteSettings.showRelatedPostImages = settingsObject.optBoolean(RELATED_POSTS_IMAGES_KEY, false);
        }
    }

    /**
     * Need to use JSONObject's instead of HashMap<String, String> to serialize array values (Jetpack Whitelist)
     */
    private JSONObject serializeWpComParamsToJSONObject() throws JSONException {
        JSONObject params = new JSONObject();

        if (mSettings.title != null && !mSettings.title.equals(mRemoteSettings.title)) {
            params.put(SET_TITLE_KEY, mSettings.title);
        }
        if (mSettings.tagline != null && !mSettings.tagline.equals(mRemoteSettings.tagline)) {
            params.put(SET_DESC_KEY, mSettings.tagline);
        }
        if (mSettings.languageId != mRemoteSettings.languageId) {
            params.put(LANGUAGE_ID_KEY, String.valueOf((mSettings.languageId)));
        }
        if (mSettings.siteIconMediaId != mRemoteSettings.siteIconMediaId) {
            params.put(SITE_ICON_KEY, String.valueOf((mSettings.siteIconMediaId)));
        }
        if (mSettings.privacy != mRemoteSettings.privacy) {
            params.put(PRIVACY_KEY, String.valueOf((mSettings.privacy)));
        }
        if (mSettings.defaultCategory != mRemoteSettings.defaultCategory) {
            params.put(DEF_CATEGORY_KEY, String.valueOf(mSettings.defaultCategory));
        }
        if (mSettings.defaultPostFormat != null && !mSettings.defaultPostFormat
                .equals(mRemoteSettings.defaultPostFormat)) {
            params.put(DEF_POST_FORMAT_KEY, mSettings.defaultPostFormat);
        }
        if (mSettings.showRelatedPosts != mRemoteSettings.showRelatedPosts
                || mSettings.showRelatedPostHeader != mRemoteSettings.showRelatedPostHeader
                || mSettings.showRelatedPostImages != mRemoteSettings.showRelatedPostImages) {
            params.put(RELATED_POSTS_ENABLED_KEY, String.valueOf(mSettings.showRelatedPosts));
            params.put(RELATED_POSTS_HEADER_KEY, String.valueOf(mSettings.showRelatedPostHeader));
            params.put(RELATED_POSTS_IMAGES_KEY, String.valueOf(mSettings.showRelatedPostImages));
        }
        if (mSettings.allowComments != mRemoteSettings.allowComments) {
            params.put(ALLOW_COMMENTS_KEY, String.valueOf(mSettings.allowComments));
        }
        if (mSettings.sendPingbacks != mRemoteSettings.sendPingbacks) {
            params.put(SEND_PINGBACKS_KEY, String.valueOf(mSettings.sendPingbacks));
        }
        if (mSettings.receivePingbacks != mRemoteSettings.receivePingbacks) {
            params.put(RECEIVE_PINGBACKS_KEY, String.valueOf(mSettings.receivePingbacks));
        }
        if (mSettings.commentApprovalRequired != mRemoteSettings.commentApprovalRequired) {
            params.put(COMMENT_MODERATION_KEY, String.valueOf(mSettings.commentApprovalRequired));
        }
        if (mSettings.closeCommentAfter != mRemoteSettings.closeCommentAfter
                || mSettings.shouldCloseAfter != mRemoteSettings.shouldCloseAfter) {
            params.put(CLOSE_OLD_COMMENTS_KEY, String.valueOf(mSettings.shouldCloseAfter));
            params.put(CLOSE_OLD_COMMENTS_DAYS_KEY, String.valueOf(mSettings.closeCommentAfter));
        }
        if (mSettings.sortCommentsBy != mRemoteSettings.sortCommentsBy) {
            if (mSettings.sortCommentsBy == ASCENDING_SORT) {
                params.put(COMMENT_SORT_ORDER_KEY, "asc");
            } else if (mSettings.sortCommentsBy == DESCENDING_SORT) {
                params.put(COMMENT_SORT_ORDER_KEY, "desc");
            }
        }
        if (mSettings.threadingLevels != mRemoteSettings.threadingLevels
                || mSettings.shouldThreadComments != mRemoteSettings.shouldThreadComments) {
            params.put(THREAD_COMMENTS_KEY, String.valueOf(mSettings.shouldThreadComments));
            params.put(THREAD_COMMENTS_DEPTH_KEY, String.valueOf(mSettings.threadingLevels));
        }
        if (mSettings.commentsPerPage != mRemoteSettings.commentsPerPage
                || mSettings.shouldPageComments != mRemoteSettings.shouldPageComments) {
            params.put(PAGE_COMMENTS_KEY, String.valueOf(mSettings.shouldPageComments));
            params.put(PAGE_COMMENT_COUNT_KEY, String.valueOf(mSettings.commentsPerPage));
        }
        if (mSettings.commentsRequireIdentity != mRemoteSettings.commentsRequireIdentity) {
            params.put(REQUIRE_IDENTITY_KEY, String.valueOf(mSettings.commentsRequireIdentity));
        }
        if (mSettings.commentsRequireUserAccount != mRemoteSettings.commentsRequireUserAccount) {
            params.put(REQUIRE_USER_ACCOUNT_KEY, String.valueOf(mSettings.commentsRequireUserAccount));
        }
        if (mSettings.commentAutoApprovalKnownUsers != mRemoteSettings.commentAutoApprovalKnownUsers) {
            params.put(WHITELIST_KNOWN_USERS_KEY, String.valueOf(mSettings.commentAutoApprovalKnownUsers));
        }
        if (mSettings.maxLinks != mRemoteSettings.maxLinks) {
            params.put(MAX_LINKS_KEY, String.valueOf(mSettings.maxLinks));
        }
        if (mSettings.holdForModeration != null && !mSettings.holdForModeration
                .equals(mRemoteSettings.holdForModeration)) {
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
        if (mSettings.sharingLabel != null && !mSettings.sharingLabel.equals(mRemoteSettings.sharingLabel)) {
            params.put(SHARING_LABEL_KEY, String.valueOf(mSettings.sharingLabel));
        }
        if (mSettings.sharingButtonStyle != null && !mSettings.sharingButtonStyle
                .equals(mRemoteSettings.sharingButtonStyle)) {
            params.put(SHARING_BUTTON_STYLE_KEY, mSettings.sharingButtonStyle);
        }
        if (mSettings.allowReblogButton != mRemoteSettings.allowReblogButton) {
            params.put(SHARING_REBLOGS_DISABLED_KEY, String.valueOf(!mSettings.allowReblogButton));
        }
        if (mSettings.allowLikeButton != mRemoteSettings.allowLikeButton) {
            params.put(SHARING_LIKES_DISABLED_KEY, String.valueOf(!mSettings.allowLikeButton));
        }
        if (mSettings.allowCommentLikes != mRemoteSettings.allowCommentLikes) {
            params.put(SHARING_COMMENT_LIKES_KEY, String.valueOf(mSettings.allowCommentLikes));
        }
        if (mSettings.twitterUsername != null && !mSettings.twitterUsername.equals(mRemoteSettings.twitterUsername)) {
            params.put(TWITTER_USERNAME_KEY, mSettings.twitterUsername);
        }
        if (mSettings.startOfWeek != null && !mSettings.startOfWeek.equals(mRemoteSettings.startOfWeek)) {
            params.put(START_OF_WEEK_KEY, mSettings.startOfWeek);
        }
        if (mSettings.dateFormat != null && !mSettings.dateFormat.equals(mRemoteSettings.dateFormat)) {
            params.put(DATE_FORMAT_KEY, mSettings.dateFormat);
        }
        if (mSettings.timeFormat != null && !mSettings.timeFormat.equals(mRemoteSettings.timeFormat)) {
            params.put(TIME_FORMAT_KEY, mSettings.timeFormat);
        }
        if (!StringUtils.equals(mSettings.timezone, mRemoteSettings.timezone)) {
            params.put(TIMEZONE_KEY, mSettings.timezone);
        }
        if (mSettings.postsPerPage != mRemoteSettings.postsPerPage) {
            params.put(POSTS_PER_PAGE_KEY, String.valueOf(mSettings.postsPerPage));
        }
        if (mSettings.ampSupported != mRemoteSettings.ampSupported) {
            params.put(AMP_SUPPORTED_KEY, String.valueOf(mSettings.ampSupported));
        }
        if (mSettings.ampEnabled != mRemoteSettings.ampEnabled) {
            params.put(AMP_ENABLED_KEY, String.valueOf(mSettings.ampEnabled));
        }

        return params;
    }

    private void deserializeJetpackRestResponse(SiteModel site, JSONObject response) {
        if (site == null || response == null) return;
        JSONObject settingsObject = response.optJSONObject("settings");
        mRemoteJpSettings.emailNotifications = settingsObject.optBoolean(JP_MONITOR_EMAIL_NOTES_KEY, false);
        mRemoteJpSettings.wpNotifications = settingsObject.optBoolean(JP_MONITOR_WP_NOTES_KEY, false);
    }

    private @NonNull Map<String, String> serializeJetpackMonitorParams() {
        Map<String, String> params = new HashMap<>();
        params.put(JP_MONITOR_EMAIL_NOTES_KEY, String.valueOf(mJpSettings.emailNotifications));
        params.put(JP_MONITOR_WP_NOTES_KEY, String.valueOf(mJpSettings.wpNotifications));
        return params;
    }

    private Map<String, Object> serializeJetpackProtectAndSsoParams() {
        Map<String, Object> params = new HashMap<>();
        if (mJpSettings.monitorActive != mRemoteJpSettings.monitorActive) {
            params.put("monitor", mJpSettings.monitorActive);
        }
        if (mJpSettings.jetpackProtectEnabled != mRemoteJpSettings.jetpackProtectEnabled) {
            params.put("protect", mJpSettings.jetpackProtectEnabled);
        }
        if (!mJpSettings.whitelistMatches(mRemoteJpSettings.jetpackProtectWhitelist)) {
            String whitelistArray = TextUtils.join(",", mJpSettings.jetpackProtectWhitelist);
            params.put("jetpack_protect_global_whitelist", whitelistArray);
        }
        if (mJpSettings.ssoActive != mRemoteJpSettings.ssoActive) {
            params.put("sso", mJpSettings.ssoActive);
        }
        if (mJpSettings.ssoMatchEmail != mRemoteJpSettings.ssoMatchEmail) {
            params.put("jetpack_sso_match_by_email", mJpSettings.ssoMatchEmail);
        }
        if (mJpSettings.ssoRequireTwoFactor != mRemoteJpSettings.ssoRequireTwoFactor) {
            params.put("jetpack_sso_require_two_step", mJpSettings.ssoRequireTwoFactor);
        }
        if (mJpSettings.commentLikes != mRemoteJpSettings.commentLikes) {
            params.put(COMMENT_LIKES, mJpSettings.commentLikes);
        }
        return params;
    }

    private CategoryModel deserializeCategoryFromJson(JSONObject category) throws JSONException {
        if (category == null) return null;

        CategoryModel model = new CategoryModel();
        model.id = category.getInt(CAT_ID_KEY);
        model.name = category.getString(CAT_NAME_KEY);
        model.slug = category.getString(CAT_SLUG_KEY);
        model.description = category.getString(CAT_DESC_KEY);
        model.parentId = category.getInt(CAT_PARENT_ID_KEY);
        model.postCount = category.getInt(CAT_POST_COUNT_KEY);

        return model;
    }

    private CategoryModel[] deserializeCategoryRestResponse(JSONObject response) {
        try {
            int num = response.getInt(CAT_NUM_POSTS_KEY);
            JSONArray categories = response.getJSONArray(CATEGORIES_KEY);
            CategoryModel[] models = new CategoryModel[num];

            for (int i = 0; i < num; ++i) {
                JSONObject category = categories.getJSONObject(i);
                models[i] = deserializeCategoryFromJson(category);
            }

            AppLog.d(AppLog.T.API, "Successfully fetched WP.com categories");

            return models;
        } catch (JSONException exception) {
            AppLog.d(AppLog.T.API, "Error parsing WP.com categories response:" + response);
            return null;
        }
    }
}
