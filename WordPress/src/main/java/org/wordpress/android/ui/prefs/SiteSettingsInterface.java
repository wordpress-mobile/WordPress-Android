package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import org.wordpress.android.R;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.SiteSettingsModel;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
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
public abstract class SiteSettingsInterface {
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

    public abstract void saveSettings();

    protected abstract void fetchRemoteData();

    /**
     * Instantiates the appropriate (self-hosted or .com) SiteSettingsInterface.
     */
    public static SiteSettingsInterface getInterface(Activity host, Blog blog, SiteSettingsListener listener) {
        if (host == null || blog == null) return null;

        if (blog.isDotcomFlag()) {
            return new DotComSiteSettings(host, blog, listener).init();
        } else {
            return new SelfHostedSiteSettings(host, blog, listener).init();
        }
    }

    protected final Activity mActivity;
    protected final Blog mBlog;
    protected final SiteSettingsListener mListener;
    protected final SiteSettingsModel mSettings;
    protected final SiteSettingsModel mRemoteSettings;

    private final HashMap<String, String> mLanguageCodes;

    protected SiteSettingsInterface(Activity host, Blog blog, SiteSettingsListener listener) {
        mActivity = host;
        mBlog = blog;
        mListener = listener;
        mSettings = new SiteSettingsModel();
        mRemoteSettings = new SiteSettingsModel();
        mLanguageCodes = new HashMap<>();
    }

    /**
     * Needed so that subclasses can be created before initializing. the final member variables
     * are null until object has been created so XML-RPC callbacks will not run.
     */
    public SiteSettingsInterface init() {
        generateLanguageMap();
        initSettings();

        return this;
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
     * Attempts to load cached settings for the blog then sends a request to the remote endpoint
     * to retrieve the latest blog data.
     */
    private void initSettings() {
        int tableKey = mBlog.getLocalTableBlogId();
        Cursor localSettings = SiteSettingsTable.getSettings(tableKey);

        if (localSettings != null && localSettings.moveToFirst() && localSettings.getCount() > 0) {
            mSettings.isInLocalTable = true;
            mSettings.deserializeDatabaseCursor(localSettings);
            localSettings.close();
            mSettings.language =
                    convertLanguageIdToLanguageCode(Integer.toString(mSettings.languageId));
            setLocation(mSettings.location);
            updateOnUiThread(null, mSettings);
        } else {
            mSettings.isInLocalTable = false;
            setAddress(mBlog.getHomeURL());
            setUsername(mBlog.getUsername());
            setPassword(mBlog.getPassword());
            setTitle(mBlog.getBlogName());
        }

        // Always fetch remote data to get any changes
        fetchRemoteData();
        fetchPostFormats();
    }

    protected void updateOnUiThread(final Exception error, final SiteSettingsModel settings) {
        if (mActivity == null || mListener == null) return;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListener.onSettingsUpdated(error, settings);
            }
        });
    }

    protected void saveOnUiThread(final Exception error, final SiteSettingsModel settings) {
        if (mActivity == null || mListener == null) return;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListener.onSettingsSaved(error, settings);
            }
        });
    }

    /**
     * Returns a SharedPreference instance to interface with Site Settings.
     */
    protected SharedPreferences siteSettingsPreferences() {
        return mActivity.getSharedPreferences(SITE_SETTINGS_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Helper method to create an XML-RPC interface for the current blog.
     */
    protected XMLRPCClientInterface instantiateInterface() {
        if (mBlog == null) {
            return null;
        }

        return XMLRPCFactory.instantiate(mBlog.getUri(), mBlog.getHttpuser(), mBlog.getHttppassword());
    }

    /**
     * Language IDs, used only by WordPress.com, are integer values that map to a language code.
     * https://github.com/Automattic/calypso-pre-oss/blob/72c2029b0805a73b749a2b64dd1d8655cae528d0/config/production.json#L86-L227
     *
     * Language codes are unique two-letter identifiers defined by ISO 639-1. Region dialects can
     * be defined by appending a -** where ** is the region code (en-GB -> English, Great Britain).
     * https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
     */
    protected String convertLanguageIdToLanguageCode(String id) {
        if (id != null) {
            for (String key : mLanguageCodes.keySet()) {
                if (id.equals(mLanguageCodes.get(key))) {
                    return key;
                }
            }
        }

        return "";
    }

    private void generateLanguageMap() {
        // Generate map of language codes
        String[] languageIds = mActivity.getResources().getStringArray(R.array.lang_ids);
        String[] languageCodes = mActivity.getResources().getStringArray(R.array.language_codes);
        for (int i = 0; i < languageIds.length && i < languageCodes.length; ++i) {
            mLanguageCodes.put(languageCodes[i], languageIds[i]);
        }
    }

    /**
     * Gets available post formats via XML-RPC. Since both self-hosted and .com sites retrieve the
     * format list via XML-RPC there is no need to implement this in the sub-classes.
     */
    private void fetchPostFormats() {
        XMLRPCClientInterface client = instantiateInterface();
        if (client == null) return;

        Map<String, String> args = new HashMap<>();
        args.put(ApiHelper.Params.SHOW_SUPPORTED_POST_FORMATS, "true");
        Object[] params = { mBlog.getRemoteBlogId(), mBlog.getUsername(),
                mBlog.getPassword(), args};
        client.callAsync(new XMLRPCCallback() {
            @Override
            public void onSuccess(long id, Object result) {
                if (result != null && result instanceof HashMap) {
                    Map<?, ?> resultMap = (HashMap<?, ?>) result;
                    if (resultMap.containsKey("supported")) {
                        Map allFormats = (Map) resultMap.get("all");
                        Object[] supportedFormats = (Object[]) resultMap.get("supported");
                        mRemoteSettings.postFormats = new String[supportedFormats.length];

                        for (int i = 0; i < supportedFormats.length; ++i) {
                            if (allFormats.containsKey(supportedFormats[i])) {
                                mRemoteSettings.postFormats[i] = allFormats.get(supportedFormats[i]).toString();
                            }
                        }
                    }
                    updateOnUiThread(null, mRemoteSettings);
                }
            }

            @Override
            public void onFailure(long id, Exception error) {
            }
        }, ApiHelper.Methods.GET_POST_FORMATS, params);
    }
}
