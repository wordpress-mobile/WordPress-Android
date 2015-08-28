package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.database.Cursor;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MapUtils;
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

    /**
     * Holds blog settings and provides methods to (de)serialize .com and self-hosted network calls.
     */
    public class SettingsContainer {
        public String address;
        public String username;
        public String password;
        public String title;
        public String tagline;
        public String language;
        public int languageId;
        public int privacy;

        boolean isInLocalTable;

        private Map<String, String> serializeParams(boolean dotCom, SettingsContainer remote) {
            if (dotCom) return serializeDotComParams(remote);
            else return serializeSelfHostedParams(remote);
        }

        private Map<String, String> serializeDotComParams(SettingsContainer remoteSettings) {
            return null;
        }

        private Map<String, String> serializeSelfHostedParams(SettingsContainer remoteSettings) {
            return null;
        }

        /**
         * Copies data from another {@link org.wordpress.android.ui.prefs.SiteSettings.SettingsContainer}.
         */
        private void copyFrom(SettingsContainer other) {
            if (other == null) return;
            address = other.address;
            username = other.username;
            password = other.password;
            title = other.title;
            tagline = other.tagline;
            language = other.language;
            languageId = other.languageId;
            privacy = other.privacy;
        }

        /**
         * Sets values from a .com REST response object.
         */
        private void deserializeDotComRestResponse(JSONObject response) {
            if (response == null) return;
            JSONObject settingsObject = response.optJSONObject("settings");

            username = mBlog.getUsername();
            password = mBlog.getPassword();
            address = response.optString(RestClientUtils.SITE_URL_KEY, "");
            title = response.optString(RestClientUtils.SITE_TITLE_KEY, "");
            tagline = response.optString(RestClientUtils.SITE_DESC_KEY, "");
            language = convertLanguageIdToLanguageCode(
                    settingsObject.optString(RestClientUtils.SITE_LANGUAGE_ID_KEY));
            privacy = settingsObject.optInt(RestClientUtils.SITE_PRIVACY_KEY, -2);
        }

        /**
         * Sets values from a self-hosted XML-RPC response object.
         */
        private void deserializeSelfHostedXmlRpcResponse(Map response) {
            if (response == null) return;
            username = mBlog.getUsername();
            password = mBlog.getPassword();
            address = getNestedMapValue(response, "blog_url");
            title = getNestedMapValue(response, "blog_title");
            tagline = getNestedMapValue(response, "blog_tagline");
        }

        /**
         * Sets values from a local database {@link Cursor}.
         */
        private void deserializeDatabaseCursor(Cursor cursor) {
            if (cursor == null) return;
            address = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.ADDRESS_COLUMN_NAME));
            username = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.USERNAME_COLUMN_NAME));
            password = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.PASSWORD_COLUMN_NAME));
            title = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.TITLE_COLUMN_NAME));
            tagline = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.TAGLINE_COLUMN_NAME));
            languageId = cursor.getInt(cursor.getColumnIndex(SiteSettingsTable.LANGUAGE_COLUMN_NAME));
            privacy = cursor.getInt(cursor.getColumnIndex(SiteSettingsTable.PRIVACY_COLUMN_NAME));
        }
    }

    public interface SiteSettingsListener {
        /**
         * Called when settings have been updated with remote changes.
         *
         * @param error
         *  null if successful
         */
        void onSettingsUpdated(Exception error, SettingsContainer container);

        /**
         * Called when attempt to update remote settings is finished.
         *
         * @param error
         *  null if successful
         */
        void onSettingsSaved(Exception error, SettingsContainer container);
    }

    private Activity mActivity;
    private Blog mBlog;
    private SettingsContainer mSettings;
    private SettingsContainer mRemoteSettings;
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
        mSettings = new SettingsContainer();

        Cursor localSettings = SiteSettingsTable.getSettings(mBlog.getUrl());
        mSettings.isInLocalTable = localSettings != null;
        if (mSettings.isInLocalTable) {
            mSettings.deserializeDatabaseCursor(localSettings);
        } else {
            setAddress(mBlog.getUrl());
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

    public String getLanguageCode() {
        return mSettings.language;
    }

    public String getLanguageForDisplay() {
        return "";
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

    public void setLanguageCode(String languageCode) {
        mSettings.language = languageCode;
    }

    public void setLanguageId(int languageId) {
        mSettings.languageId = languageId;
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
                            mRemoteSettings = new SettingsContainer();
                            mRemoteSettings.deserializeDotComRestResponse(response);
                            mListener.onSettingsUpdated(null, mRemoteSettings);
                            SiteSettingsTable.saveSettings(mRemoteSettings);
                            mRemoteSettings.isInLocalTable = true;
                            if (!mSettings.isInLocalTable) mSettings.copyFrom(mRemoteSettings);
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AppLog.w(AppLog.T.API, "Error response to Settings REST request: " + error);
                            mListener.onSettingsUpdated(error, null);
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
        final Map<String, String> params =
                mSettings.serializeParams(mBlog.isDotcomFlag(), mRemoteSettings);
        if (params == null || params.size() == 0) return;

        if (mBlog.isDotcomFlag()) {
            WordPress.getRestClientUtils().setGeneralSiteSettings(
                    String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            AppLog.d(AppLog.T.API, "Site Settings saved remotely");
                            mListener.onSettingsSaved(null, mSettings);
                            mRemoteSettings.copyFrom(mSettings);
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AppLog.w(AppLog.T.API, "Error POSTing site settings changes: " + error);
                            mListener.onSettingsSaved(error, null);
                        }
                    }, params);
        } else {
            XMLRPCCallback callback = new XMLRPCCallback() {
                @Override
                public void onSuccess(long id, final Object result) {
                    mListener.onSettingsSaved(null, mSettings);
                    mRemoteSettings.copyFrom(mSettings);
                }

                @Override
                public void onFailure(long id, final Exception error) {
                    mListener.onSettingsSaved(error, null);
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
                mRemoteSettings = new SettingsContainer();
                mRemoteSettings.deserializeSelfHostedXmlRpcResponse((Map) result);
                SiteSettingsTable.saveSettings(mSettings);
                mRemoteSettings.isInLocalTable = true;
                if (!mSettings.isInLocalTable) mSettings.copyFrom(mRemoteSettings);
                mListener.onSettingsUpdated(null, mRemoteSettings);
            } else {
                // Response is considered an error if we are unable to parse it
                AppLog.w(AppLog.T.API, "Error parsing response to Settings XML-RPC request: " + result);
                mListener.onSettingsUpdated(new XMLRPCException("Unknown response object"), null);
            }
        }

        @Override
        public void onFailure(long id, final Exception error) {
            AppLog.w(AppLog.T.API, "Error response to Settings XML-RPC request: " + error);
            mListener.onSettingsUpdated(error, null);
        }
    };

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
