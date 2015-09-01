package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.SiteSettingsModel;
import org.wordpress.android.util.AppLog;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of various settings and handles propagating changes where necessary.
 *
 * - Title
 * - Tagline
 * - Privacy
 * - Language
 * - Address
 * - Username
 * - Password
 * - Location: only stored locally
 * - Default post category
 * - Default post format
 * - Related posts
 */

public class SiteSettings {
    public static final String SITE_SETTINGS_PREFS = "site-settings-prefs";
    public static final String LOCATION_PREF_KEY = "site-settings-location-pref";

    public interface SiteSettingsListener {
        /**
         * Called when settings have been updated with remote changes.
         *
         * @param error
         *  null if successful
         */
        void onSettingsUpdated(Exception error, SiteSettingsModel container);

        /**
         * Called when attempt to update remote settings is finished.
         *
         * @param error
         *  null if successful
         */
        void onSettingsSaved(Exception error, SiteSettingsModel container);

        /**
         * Called when a request to validate current credentials has completed.
         *
         * @param valid
         *  true if the current credentials are valid
         */
        void onCredentialsValidated(boolean valid);
    }

    private Activity mActivity;
    private Blog mBlog;
    private SiteSettingsModel mSettings;
    private SiteSettingsModel mRemoteSettings;
    private SiteSettingsListener mListener;
    private HashMap<String, String> mLanguageCodes = new HashMap<>();

    public SiteSettings(Activity host, Blog blog, SiteSettingsListener listener) {
        mActivity = host;
        mBlog = blog;
        mListener = listener;

        // Generate map of language codes
        String[] languageIds = host.getResources().getStringArray(R.array.lang_ids);
        String[] languageCodes = host.getResources().getStringArray(R.array.language_codes);
        for (int i = 0; i < languageIds.length && i < languageCodes.length; ++i) {
            mLanguageCodes.put(languageCodes[i], languageIds[i]);
        }

        initSettings();
    }

    /**
     * Attempts to load cached settings for the blog then sends a request to the remote endpoint
     * to retrieve the latest blog data.
     */
    private void initSettings() {
        mSettings = new SiteSettingsModel();

        int tableKey = mBlog.getLocalTableBlogId();
        Cursor localSettings = SiteSettingsTable.getSettings(tableKey);
        mSettings.isInLocalTable = localSettings != null && localSettings.moveToFirst() && localSettings.getCount() > 0;
        if (mSettings.isInLocalTable) {
            mSettings.deserializeDatabaseCursor(localSettings);
            localSettings.close();
            setLocation(mSettings.location);
            mSettings.language =
                    convertLanguageIdToLanguageCode(Integer.toString(mSettings.languageId));
            updateOnUiThread(null, mSettings);
        } else {
            setAddress(mBlog.getHomeURL());
            setUsername(mBlog.getUsername());
            setPassword(mBlog.getPassword());
            setTitle(mBlog.getBlogName());
        }

        // Always fetch remote data to get any changes
        fetchRemoteData();
    }

    public String getAddress() {
        return mSettings.address;
    }

    public String getUsername() {
        return mSettings.username;
    }

    public String getPassword() {
        return mSettings.password;
    }

    public String getTitle() {
        return mSettings.title;
    }

    public String getTagline() {
        return mSettings.tagline;
    }

    public int getPrivacy() {
        return mSettings.privacy;
    }

    public String getLanguageCode() {
        return mSettings.language;
    }

    public String getLanguageForDisplay() {
        return "";
    }

    public boolean getLocation() {
        return mSettings.location;
    }

    public void setAddress(String address) {
        mSettings.address = address;
    }

    public void setUsername(String username) {
        mSettings.username = username;
    }

    public void setPassword(String password) {
        mSettings.password = password;
    }

    public void setTitle(String title) {
        mSettings.title = title;
    }

    public void setTagline(String tagline) {
        mSettings.tagline = tagline;
    }

    public void setPrivacy(int privacy) {
        mSettings.privacy = privacy;
    }

    public void setLanguageCode(String languageCode) {
        setLanguageId(Integer.valueOf(mLanguageCodes.get(languageCode)));
    }

    public void setLanguageId(int languageId) {
        if (mSettings.languageId != languageId) {
            mSettings.languageId = languageId;
            mSettings.language = convertLanguageIdToLanguageCode(Integer.toString(languageId));
        }
    }

    public void setLocation(boolean location) {
        mSettings.location = location;
        siteSettingsPreferences().edit().putBoolean(LOCATION_PREF_KEY, location).apply();
    }

    /**
     * Request remote site data via the WordPress REST API.
     */
    public void fetchRemoteData() {
        if (mBlog.isDotcomFlag()) {
            WordPress.getRestClientUtils().getGeneralSettings(
                    String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            AppLog.d(AppLog.T.API, "Received response to Settings REST request.");
                            mRemoteSettings = new SiteSettingsModel();
                            mRemoteSettings.localTableId = mBlog.getLocalTableBlogId();
                            mRemoteSettings.deserializeDotComRestResponse(mBlog, response);
                            mRemoteSettings.language = convertLanguageIdToLanguageCode(
                                    Integer.toString(mRemoteSettings.languageId));
                            if (!mRemoteSettings.isTheSame(mSettings)) {
                                updateOnUiThread(null, mRemoteSettings);
                                mSettings.copyFrom(mRemoteSettings);
                                SiteSettingsTable.saveSettings(mSettings);
                            }
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AppLog.w(AppLog.T.API, "Error response to Settings REST request: " + error);
                            updateOnUiThread(error, null);
                        }
                    });
        } else {
            XMLRPCClientInterface xmlrpcInterface = instantiateInterface();
            if (xmlrpcInterface == null) return;
            Object[] params = {mBlog.getRemoteBlogId(), mBlog.getUsername(), mBlog.getPassword()};
            xmlrpcInterface.callAsync(mXmlRpcFetchCallback, ApiHelper.Methods.GET_OPTIONS, params);
        }
    }

    public void saveSettings() {
        // Save current settings and attempt to sync remotely
        SiteSettingsTable.saveSettings(mSettings);

        final Map<String, String> params =
                mSettings.serializeParams(mBlog.isDotcomFlag(), mRemoteSettings);
        if (params == null || params.isEmpty()) return;

        if (mBlog.isDotcomFlag()) {
            WordPress.getRestClientUtils().setGeneralSiteSettings(
                    String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            AppLog.d(AppLog.T.API, "Site Settings saved remotely");
                            saveOnUiThread(null, mSettings);
                            mRemoteSettings.copyFrom(mSettings);
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AppLog.w(AppLog.T.API, "Error POSTing site settings changes: " + error);
                            saveOnUiThread(error, null);
                        }
                    }, params);
        } else {
            XMLRPCCallback callback = new XMLRPCCallback() {
                @Override
                public void onSuccess(long id, final Object result) {
                    saveOnUiThread(null, mSettings);
                    mRemoteSettings.copyFrom(mSettings);
                }

                @Override
                public void onFailure(long id, final Exception error) {
                    saveOnUiThread(error, null);
                }
            };
            final Object[] callParams = {
                    mBlog.getRemoteBlogId(), mSettings.username, mSettings.password, params
            };

            XMLRPCClientInterface xmlrpcInterface = instantiateInterface();
            if (xmlrpcInterface == null) return;
            xmlrpcInterface.callAsync(callback, ApiHelper.Methods.SET_OPTIONS, callParams);
        }
    }

    /**
     * Handles response to fetching self-hosted site options via XML-RPC.
     */
    private final XMLRPCCallback mXmlRpcFetchCallback = new XMLRPCCallback() {
        @Override
        public void onSuccess(long id, final Object result) {
            if (result instanceof Map) {
                AppLog.d(AppLog.T.API, "Received response to Settings XML-RPC request.");
                mRemoteSettings = new SiteSettingsModel();
                mRemoteSettings.localTableId = mBlog.getLocalTableBlogId();
                mRemoteSettings.deserializeSelfHostedXmlRpcResponse(mBlog, (Map) result);
                if (!mRemoteSettings.isTheSame(mSettings)) {
                    updateOnUiThread(null, mRemoteSettings);
                    mSettings.copyFrom(mRemoteSettings);
                    SiteSettingsTable.saveSettings(mSettings);
                    if (mSettings.isInLocalTable) {
                    }
                }
            } else {
                // Response is considered an error if we are unable to parse it
                AppLog.w(AppLog.T.API, "Error parsing response to Settings XML-RPC request: " + result);
                updateOnUiThread(new XMLRPCException("Unknown response object"), null);
            }
        }

        @Override
        public void onFailure(long id, final Exception error) {
            AppLog.w(AppLog.T.API, "Error response to Settings XML-RPC request: " + error);
            updateOnUiThread(error, null);
        }
    };

    private void updateOnUiThread(final Exception error, final SiteSettingsModel settings) {
        if (mActivity == null || mListener == null) return;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onSettingsUpdated(error, settings);
                }
            }
        });
    }

    private void saveOnUiThread(final Exception error, final SiteSettingsModel settings) {
        if (mActivity == null || mListener == null) return;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onSettingsSaved(error, settings);
                }
            }
        });
    }

    /**
     * Returns a SharedPreference instance to interface with Site Settings.
     */
    private SharedPreferences siteSettingsPreferences() {
        return mActivity.getSharedPreferences(SITE_SETTINGS_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Language IDs, used only by WordPress.com, are integer values that map to a language code.
     * https://github.com/Automattic/calypso-pre-oss/blob/72c2029b0805a73b749a2b64dd1d8655cae528d0/config/production.json#L86-L227
     *
     * Language codes are unique two-letter identifiers defined by ISO 639-1. Region dialects can
     * be defined by appending a -** where ** is the region code (en-GB -> English, Great Britain).
     * https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
     */
    private String convertLanguageIdToLanguageCode(String id) {
        if (id != null) {
            for (String key : mLanguageCodes.keySet()) {
                if (id.equals(mLanguageCodes.get(key))) {
                    return key;
                }
            }
        }

        return "";
    }

    /**
     * Helper method to create an XML-RPC interface for the current blog.
     */
    private XMLRPCClientInterface instantiateInterface() {
        if (mBlog == null) {
            return null;
        }

        return XMLRPCFactory.instantiate(mBlog.getUri(), mBlog.getHttpuser(), mBlog.getHttppassword());
    }
}
