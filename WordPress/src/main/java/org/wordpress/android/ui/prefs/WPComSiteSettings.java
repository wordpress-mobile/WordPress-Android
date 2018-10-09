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
    private static final String PORTFOLIO_ENABLED_KEY = "jetpack_portfolio";
    private static final String PORTFOLIO_POSTS_PER_PAGE_KEY = "jetpack_portfolio_posts_per_page";
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

                        mRemoteSettings.setLocalTableId(mSite.getId());
                        deserializeWpComRestResponse(mSite, response);
                        if (!mRemoteSettings.equals(mSettings)) {
                            // postFormats setting is not returned by this api call so copy it over
                            final Map<String, String> currentPostFormats = mSettings.getPostFormats();

                            // Local settings
                            boolean location = mSettings.getLocation();
                            mSettings.copyFrom(mRemoteSettings);
                            mSettings.setPostFormats(currentPostFormats);
                            mSettings.setLocation(location);

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
                        mRemoteSettings.setCategories(models);
                        mSettings.setCategories(models);
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

        mRemoteSettings.setUsername(site.getUsername());
        mRemoteSettings.setPassword(site.getPassword());
        mRemoteSettings.setAddress(response.optString(URL_KEY, ""));
        mRemoteSettings.setTitle(response.optString(GET_TITLE_KEY, ""));
        mRemoteSettings.setTagline(response.optString(GET_DESC_KEY, ""));
        mRemoteSettings.setLanguageId(settingsObject.optInt(LANGUAGE_ID_KEY, -1));
        mRemoteSettings.setSiteIconMediaId(settingsObject.optInt(SITE_ICON_KEY, 0));
        mRemoteSettings.setPrivacy(settingsObject.optInt(PRIVACY_KEY, -2));
        mRemoteSettings.setDefaultCategory(settingsObject.optInt(DEF_CATEGORY_KEY, 1));
        mRemoteSettings.setDefaultPostFormat(settingsObject.optString(DEF_POST_FORMAT_KEY, STANDARD_POST_FORMAT_KEY));
        mRemoteSettings.setLanguage(languageIdToLanguageCode(Integer.toString(mRemoteSettings.getLanguageId())));
        mRemoteSettings.setAllowComments(settingsObject.optBoolean(ALLOW_COMMENTS_KEY, true));
        mRemoteSettings.setSendPingbacks(settingsObject.optBoolean(SEND_PINGBACKS_KEY, false));
        mRemoteSettings.setReceivePingbacks(settingsObject.optBoolean(RECEIVE_PINGBACKS_KEY, true));
        mRemoteSettings.setShouldCloseAfter(settingsObject.optBoolean(CLOSE_OLD_COMMENTS_KEY, false));
        mRemoteSettings.setCloseCommentAfter(settingsObject.optInt(CLOSE_OLD_COMMENTS_DAYS_KEY, 0));
        mRemoteSettings.setShouldThreadComments(settingsObject.optBoolean(THREAD_COMMENTS_KEY, false));
        mRemoteSettings.setThreadingLevels(settingsObject.optInt(THREAD_COMMENTS_DEPTH_KEY, 0));
        mRemoteSettings.setShouldPageComments(settingsObject.optBoolean(PAGE_COMMENTS_KEY, false));
        mRemoteSettings.setCommentsPerPage(settingsObject.optInt(PAGE_COMMENT_COUNT_KEY, 0));
        mRemoteSettings.setCommentApprovalRequired(settingsObject.optBoolean(COMMENT_MODERATION_KEY, false));
        mRemoteSettings.setCommentsRequireIdentity(settingsObject.optBoolean(REQUIRE_IDENTITY_KEY, false));
        mRemoteSettings.setCommentsRequireUserAccount(settingsObject.optBoolean(REQUIRE_USER_ACCOUNT_KEY, true));
        mRemoteSettings.setCommentAutoApprovalKnownUsers(settingsObject.optBoolean(WHITELIST_KNOWN_USERS_KEY, false));
        mRemoteSettings.setMaxLinks(settingsObject.optInt(MAX_LINKS_KEY, 0));
        mRemoteSettings.setHoldForModeration(new ArrayList<String>());
        mRemoteSettings.setBlacklist(new ArrayList<String>());
        mRemoteSettings.setSharingLabel(settingsObject.optString(SHARING_LABEL_KEY, ""));
        mRemoteSettings.setSharingButtonStyle(settingsObject.optString(SHARING_BUTTON_STYLE_KEY,
                DEFAULT_SHARING_BUTTON_STYLE));
        mRemoteSettings.setAllowCommentLikes(settingsObject.optBoolean(SHARING_COMMENT_LIKES_KEY, false));
        mRemoteSettings.setTwitterUsername(settingsObject.optString(TWITTER_USERNAME_KEY, ""));
        mRemoteSettings.setStartOfWeek(settingsObject.optString(START_OF_WEEK_KEY, ""));
        mRemoteSettings.setDateFormat(settingsObject.optString(DATE_FORMAT_KEY, ""));
        mRemoteSettings.setTimeFormat(settingsObject.optString(TIME_FORMAT_KEY, ""));
        mRemoteSettings.setTimezone(settingsObject.optString(TIMEZONE_KEY, ""));
        mRemoteSettings.setPostsPerPage(settingsObject.optInt(POSTS_PER_PAGE_KEY, 0));
        mRemoteSettings.setAmpSupported(settingsObject.optBoolean(AMP_SUPPORTED_KEY, false));
        mRemoteSettings.setAmpEnabled(settingsObject.optBoolean(AMP_ENABLED_KEY, false));

        boolean reblogsDisabled = settingsObject.optBoolean(SHARING_REBLOGS_DISABLED_KEY, false);
        boolean likesDisabled = settingsObject.optBoolean(SHARING_LIKES_DISABLED_KEY, false);
        mRemoteSettings.setAllowReblogButton(!reblogsDisabled);
        mRemoteSettings.setAllowLikeButton(!likesDisabled);

        String modKeys = settingsObject.optString(MODERATION_KEYS_KEY, "");
        if (modKeys.length() > 0) {
            Collections.addAll(mRemoteSettings.getHoldForModeration(), modKeys.split("\n"));
        }
        String blacklistKeys = settingsObject.optString(BLACKLIST_KEYS_KEY, "");
        if (blacklistKeys.length() > 0) {
            Collections.addAll(mRemoteSettings.getBlacklist(), blacklistKeys.split("\n"));
        }

        if (settingsObject.optString(COMMENT_SORT_ORDER_KEY, "").equals("asc")) {
            mRemoteSettings.setSortCommentsBy(ASCENDING_SORT);
        } else {
            mRemoteSettings.setSortCommentsBy(DESCENDING_SORT);
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
            mRemoteSettings.setShowRelatedPosts(settingsObject.optBoolean(RELATED_POSTS_ENABLED_KEY, false));
            mRemoteSettings.setShowRelatedPostHeader(settingsObject.optBoolean(RELATED_POSTS_HEADER_KEY, false));
            mRemoteSettings.setShowRelatedPostImages(settingsObject.optBoolean(RELATED_POSTS_IMAGES_KEY, false));
        }

        mRemoteSettings.setPortfolioEnabled(settingsObject.optBoolean(PORTFOLIO_ENABLED_KEY, false));
        mRemoteSettings.setPortfolioPostsPerPage(settingsObject.optInt(PORTFOLIO_POSTS_PER_PAGE_KEY, 0));
    }

    /**
     * Need to use JSONObject's instead of HashMap<String, String> to serialize array values (Jetpack Whitelist)
     */
    private JSONObject serializeWpComParamsToJSONObject() throws JSONException {
        JSONObject params = new JSONObject();

        if (mSettings.getTitle() != null && !mSettings.getTitle().equals(mRemoteSettings.getTitle())) {
            params.put(SET_TITLE_KEY, mSettings.getTitle());
        }
        if (mSettings.getTagline() != null && !mSettings.getTagline().equals(mRemoteSettings.getTagline())) {
            params.put(SET_DESC_KEY, mSettings.getTagline());
        }
        if (mSettings.getLanguageId() != mRemoteSettings.getLanguageId()) {
            params.put(LANGUAGE_ID_KEY, String.valueOf((mSettings.getLanguageId())));
        }
        if (mSettings.getSiteIconMediaId() != mRemoteSettings.getSiteIconMediaId()) {
            params.put(SITE_ICON_KEY, String.valueOf((mSettings.getSiteIconMediaId())));
        }
        if (mSettings.getPrivacy() != mRemoteSettings.getPrivacy()) {
            params.put(PRIVACY_KEY, String.valueOf((mSettings.getPrivacy())));
        }
        if (mSettings.getDefaultCategory() != mRemoteSettings.getDefaultCategory()) {
            params.put(DEF_CATEGORY_KEY, String.valueOf(mSettings.getDefaultCategory()));
        }
        if (mSettings.getDefaultPostFormat() != null && !mSettings.getDefaultPostFormat()
                                                                  .equals(mRemoteSettings.getDefaultPostFormat())) {
            params.put(DEF_POST_FORMAT_KEY, mSettings.getDefaultPostFormat());
        }
        if (mSettings.getShowRelatedPosts() != mRemoteSettings.getShowRelatedPosts()
            || mSettings.getShowRelatedPostHeader() != mRemoteSettings.getShowRelatedPostHeader()
            || mSettings.getShowRelatedPostImages() != mRemoteSettings.getShowRelatedPostImages()) {
            params.put(RELATED_POSTS_ENABLED_KEY, String.valueOf(mSettings.getShowRelatedPosts()));
            params.put(RELATED_POSTS_HEADER_KEY, String.valueOf(mSettings.getShowRelatedPostHeader()));
            params.put(RELATED_POSTS_IMAGES_KEY, String.valueOf(mSettings.getShowRelatedPostImages()));
        }
        if (mSettings.getAllowComments() != mRemoteSettings.getAllowComments()) {
            params.put(ALLOW_COMMENTS_KEY, String.valueOf(mSettings.getAllowComments()));
        }
        if (mSettings.getSendPingbacks() != mRemoteSettings.getSendPingbacks()) {
            params.put(SEND_PINGBACKS_KEY, String.valueOf(mSettings.getSendPingbacks()));
        }
        if (mSettings.getReceivePingbacks() != mRemoteSettings.getReceivePingbacks()) {
            params.put(RECEIVE_PINGBACKS_KEY, String.valueOf(mSettings.getReceivePingbacks()));
        }
        if (mSettings.getCommentApprovalRequired() != mRemoteSettings.getCommentApprovalRequired()) {
            params.put(COMMENT_MODERATION_KEY, String.valueOf(mSettings.getCommentApprovalRequired()));
        }
        if (mSettings.getCloseCommentAfter() != mRemoteSettings.getCloseCommentAfter()
            || mSettings.getShouldCloseAfter() != mRemoteSettings.getShouldCloseAfter()) {
            params.put(CLOSE_OLD_COMMENTS_KEY, String.valueOf(mSettings.getShouldCloseAfter()));
            params.put(CLOSE_OLD_COMMENTS_DAYS_KEY, String.valueOf(mSettings.getCloseCommentAfter()));
        }
        if (mSettings.getSortCommentsBy() != mRemoteSettings.getSortCommentsBy()) {
            if (mSettings.getSortCommentsBy() == ASCENDING_SORT) {
                params.put(COMMENT_SORT_ORDER_KEY, "asc");
            } else if (mSettings.getSortCommentsBy() == DESCENDING_SORT) {
                params.put(COMMENT_SORT_ORDER_KEY, "desc");
            }
        }
        if (mSettings.getThreadingLevels() != mRemoteSettings.getThreadingLevels()
            || mSettings.getShouldThreadComments() != mRemoteSettings.getShouldThreadComments()) {
            params.put(THREAD_COMMENTS_KEY, String.valueOf(mSettings.getShouldThreadComments()));
            params.put(THREAD_COMMENTS_DEPTH_KEY, String.valueOf(mSettings.getThreadingLevels()));
        }
        if (mSettings.getCommentsPerPage() != mRemoteSettings.getCommentsPerPage()
            || mSettings.getShouldPageComments() != mRemoteSettings.getShouldPageComments()) {
            params.put(PAGE_COMMENTS_KEY, String.valueOf(mSettings.getShouldPageComments()));
            params.put(PAGE_COMMENT_COUNT_KEY, String.valueOf(mSettings.getCommentsPerPage()));
        }
        if (mSettings.getCommentsRequireIdentity() != mRemoteSettings.getCommentsRequireIdentity()) {
            params.put(REQUIRE_IDENTITY_KEY, String.valueOf(mSettings.getCommentsRequireIdentity()));
        }
        if (mSettings.getCommentsRequireUserAccount() != mRemoteSettings.getCommentsRequireUserAccount()) {
            params.put(REQUIRE_USER_ACCOUNT_KEY, String.valueOf(mSettings.getCommentsRequireUserAccount()));
        }
        if (mSettings.getCommentAutoApprovalKnownUsers() != mRemoteSettings.getCommentAutoApprovalKnownUsers()) {
            params.put(WHITELIST_KNOWN_USERS_KEY, String.valueOf(mSettings.getCommentAutoApprovalKnownUsers()));
        }
        if (mSettings.getMaxLinks() != mRemoteSettings.getMaxLinks()) {
            params.put(MAX_LINKS_KEY, String.valueOf(mSettings.getMaxLinks()));
        }
        if (mSettings.getHoldForModeration() != null && !mSettings.getHoldForModeration()
                                                                  .equals(mRemoteSettings.getHoldForModeration())) {
            StringBuilder builder = new StringBuilder();
            for (String key : mSettings.getHoldForModeration()) {
                builder.append(key);
                builder.append("\n");
            }
            if (builder.length() > 1) {
                params.put(MODERATION_KEYS_KEY, builder.substring(0, builder.length() - 1));
            } else {
                params.put(MODERATION_KEYS_KEY, "");
            }
        }
        if (mSettings.getBlacklist() != null && !mSettings.getBlacklist().equals(mRemoteSettings.getBlacklist())) {
            StringBuilder builder = new StringBuilder();
            for (String key : mSettings.getBlacklist()) {
                builder.append(key);
                builder.append("\n");
            }
            if (builder.length() > 1) {
                params.put(BLACKLIST_KEYS_KEY, builder.substring(0, builder.length() - 1));
            } else {
                params.put(BLACKLIST_KEYS_KEY, "");
            }
        }
        if (mSettings.getSharingLabel() != null && !mSettings.getSharingLabel()
                                                             .equals(mRemoteSettings.getSharingLabel())) {
            params.put(SHARING_LABEL_KEY, String.valueOf(mSettings.getSharingLabel()));
        }
        if (mSettings.getSharingButtonStyle() != null && !mSettings.getSharingButtonStyle()
                                                                   .equals(mRemoteSettings.getSharingButtonStyle())) {
            params.put(SHARING_BUTTON_STYLE_KEY, mSettings.getSharingButtonStyle());
        }
        if (mSettings.getAllowReblogButton() != mRemoteSettings.getAllowReblogButton()) {
            params.put(SHARING_REBLOGS_DISABLED_KEY, String.valueOf(!mSettings.getAllowReblogButton()));
        }
        if (mSettings.getAllowLikeButton() != mRemoteSettings.getAllowLikeButton()) {
            params.put(SHARING_LIKES_DISABLED_KEY, String.valueOf(!mSettings.getAllowLikeButton()));
        }
        if (mSettings.getAllowCommentLikes() != mRemoteSettings.getAllowCommentLikes()) {
            params.put(SHARING_COMMENT_LIKES_KEY, String.valueOf(mSettings.getAllowCommentLikes()));
        }
        if (mSettings.getTwitterUsername() != null && !mSettings.getTwitterUsername()
                                                                .equals(mRemoteSettings.getTwitterUsername())) {
            params.put(TWITTER_USERNAME_KEY, mSettings.getTwitterUsername());
        }
        if (mSettings.getStartOfWeek() != null && !mSettings.getStartOfWeek()
                                                            .equals(mRemoteSettings.getStartOfWeek())) {
            params.put(START_OF_WEEK_KEY, mSettings.getStartOfWeek());
        }
        if (mSettings.getDateFormat() != null && !mSettings.getDateFormat().equals(mRemoteSettings.getDateFormat())) {
            params.put(DATE_FORMAT_KEY, mSettings.getDateFormat());
        }
        if (mSettings.getTimeFormat() != null && !mSettings.getTimeFormat().equals(mRemoteSettings.getTimeFormat())) {
            params.put(TIME_FORMAT_KEY, mSettings.getTimeFormat());
        }
        if (!StringUtils.equals(mSettings.getTimezone(), mRemoteSettings.getTimezone())) {
            params.put(TIMEZONE_KEY, mSettings.getTimezone());
        }
        if (mSettings.getPostsPerPage() != mRemoteSettings.getPostsPerPage()) {
            params.put(POSTS_PER_PAGE_KEY, String.valueOf(mSettings.getPostsPerPage()));
        }
        if (mSettings.getAmpSupported() != mRemoteSettings.getAmpSupported()) {
            params.put(AMP_SUPPORTED_KEY, String.valueOf(mSettings.getAmpSupported()));
        }
        if (mSettings.getAmpEnabled() != mRemoteSettings.getAmpEnabled()) {
            params.put(AMP_ENABLED_KEY, String.valueOf(mSettings.getAmpEnabled()));
        }
        if (mSettings.isPortfolioEnabled() != mRemoteSettings.isPortfolioEnabled()) {
            params.put(PORTFOLIO_ENABLED_KEY, String.valueOf(mSettings.isPortfolioEnabled()));
            params.put(PORTFOLIO_POSTS_PER_PAGE_KEY, String.valueOf(mSettings.getPortfolioPostsPerPage()));
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
