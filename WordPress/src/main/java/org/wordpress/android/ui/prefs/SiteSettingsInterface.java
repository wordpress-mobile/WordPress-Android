package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.SiteSettingsModel;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    public static final String LANGUAGE_PREF_KEY = "site-settings-language-pref";
    public static final String LOCATION_PREF_KEY = "site-settings-location-pref";
    public static final String DEF_CATEGORY_PREF_KEY = "site-settings-category-pref";
    public static final String DEF_FORMAT_PREF_KEY = "site-settings-format-pref";

    public static final int ASCENDING_SORT = 0;
    public static final int DESCENDING_SORT = 1;

    private static final String STANDARD_POST_FORMAT = "standard";

    /**
     * Returns a SharedPreference instance to interface with Site Settings.
     */
    public static SharedPreferences siteSettingsPreferences(Context context) {
        return context.getSharedPreferences(SITE_SETTINGS_PREFS, Context.MODE_PRIVATE);
    }

    public static boolean getGeotagging(Context context) {
        return siteSettingsPreferences(context).getBoolean(LOCATION_PREF_KEY, false);
    }

    public static String getDefaultCategory(Context context) {
        int id = siteSettingsPreferences(context).getInt(DEF_CATEGORY_PREF_KEY, 0);

        if (id != 0) {
            CategoryModel category = new CategoryModel();
            Cursor cursor = SiteSettingsTable.getCategory(id);
            if (cursor != null && cursor.moveToFirst()) {
                category.deserializeFromDatabase(cursor);
                return category.name;
            }
        }

        return "";
    }

    public static String getDefaultFormat(Context context) {
        return siteSettingsPreferences(context).getString(DEF_FORMAT_PREF_KEY, "");
    }

    public class AuthenticationError extends Exception {
    }

    public interface SiteSettingsListener {
        /**
         * Called when settings have been updated with remote changes.
         *
         * @param error
         * null if successful
         */
        void onSettingsUpdated(Exception error);

        /**
         * Called when attempt to update remote settings is finished.
         *
         * @param error
         * null if successful
         */
        void onSettingsSaved(Exception error);

        /**
         * Called when a request to validate current credentials has completed.
         *
         * @param error
         * null if successful
         */
        void onCredentialsValidated(Exception error);
    }

    public void saveSettings() {
        SiteSettingsTable.saveSettings(mSettings);
        siteSettingsPreferences(mActivity).edit().putString(LANGUAGE_PREF_KEY, mSettings.language).apply();
        siteSettingsPreferences(mActivity).edit().putBoolean(LOCATION_PREF_KEY, mSettings.location).apply();
        siteSettingsPreferences(mActivity).edit().putInt(DEF_CATEGORY_PREF_KEY, mSettings.defaultCategory).apply();
        siteSettingsPreferences(mActivity).edit().putString(DEF_FORMAT_PREF_KEY, mSettings.defaultPostFormat).apply();
    }

    protected abstract void fetchRemoteData();

    /**
     * Instantiates the appropriate (self-hosted or .com) SiteSettingsInterface.
     */
    public static SiteSettingsInterface getInterface(Activity host, Blog blog, SiteSettingsListener listener) {
        if (host == null || blog == null) return null;

        if (blog.isDotcomFlag()) {
            return new DotComSiteSettings(host, blog, listener);
        } else {
            return new SelfHostedSiteSettings(host, blog, listener);
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

    public String getPrivacyForDisplay() {
        switch (mSettings.privacy) {
            case -1:
                return mActivity.getString(R.string.privacy_private);
            case 0:
                return mActivity.getString(R.string.privacy_hidden);
            case 1:
                return mActivity.getString(R.string.privacy_public);
            default:
                return "";
        }
    }

    public String getLanguageCode() {
        return mSettings.language;
    }

    public boolean getLocation() {
        return mSettings.location;
    }

    public Map<String, String> getFormats() {
        return mSettings.postFormats;
    }

    public String[] getFormatKeys() {
        return mSettings.postFormatKeys;
    }

    public CategoryModel[] getCategories() {
        return mSettings.categories;
    }

    public Map<Integer, String> getCategoryNames() {
        Map<Integer, String> categoryNames = new HashMap<>();

        if (mSettings.categories != null && mSettings.categories.length > 0) {
            for (CategoryModel model : mSettings.categories) {
                categoryNames.put(model.id, model.name);
            }
        }

        return categoryNames;
    }

    public String getDefaultPostFormat() {
        if (TextUtils.isEmpty(mSettings.defaultPostFormat)) {
            return STANDARD_POST_FORMAT;
        }

        return mSettings.defaultPostFormat;
    }

    public String getDefaultPostFormatDisplay() {
        return getFormats().get(mSettings.defaultPostFormat);
    }

    public int getDefaultCategory() {
        return mSettings.defaultCategory;
    }

    public String getDefaultCategoryForDisplay() {
        if (getCategories() != null) {
            for (CategoryModel model : getCategories()) {
                if (model != null && model.id == getDefaultCategory()) {
                    return model.name;
                }
            }
        }

        return "";
    }

    public boolean getShowRelatedPosts() {
        return mSettings.showRelatedPosts;
    }

    public boolean getShowRelatedPostHeader() {
        return mSettings.showRelatedPostHeader;
    }

    public boolean getShowRelatedPostImages() {
        return mSettings.showRelatedPostImages;
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
        mSettings.language = languageCode;
        mSettings.languageId = Integer.valueOf(mLanguageCodes.get(languageCode));
    }

    public void setLanguageId(int languageId) {
        // Want to prevent O(n) language code lookup if there is no change
        if (mSettings.languageId != languageId) {
            mSettings.languageId = languageId;
            mSettings.language = languageIdToLanguageCode(Integer.toString(languageId));
        }
    }

    public void setLocation(boolean location) {
        mSettings.location = location;
    }

    public void setAllowComments(boolean allowComments) {
        mSettings.allowComments = allowComments;
    }

    public boolean getAllowComments() {
        return mSettings.allowComments;
    }

    public void setSendPingbacks(boolean sendPingbacks) {
        mSettings.sendPingbacks = sendPingbacks;
    }

    public boolean getSendPingbacks() {
        return mSettings.sendPingbacks;
    }

    public void setReceivePingbacks(boolean receivePingbacks) {
        mSettings.receivePingbacks = receivePingbacks;
    }

    public boolean getReceivePingbacks() {
        return mSettings.receivePingbacks;
    }

    public void setCloseAfter(int period) {
        mSettings.closeCommentAfter = period;
    }

    public int getCloseAfter() {
        return mSettings.closeCommentAfter;
    }

    public void setCommentSorting(int method) {
        mSettings.sortCommentsBy = method;
    }

    public int getCommentSorting() {
        return mSettings.sortCommentsBy;
    }

    public void setThreadingLevels(int levels) {
        mSettings.threadingLevels = levels;
    }

    public int getThreadingLevels() {
        return mSettings.threadingLevels;
    }

    public void setPagingCount(int count) {
        mSettings.commentsPerPage = count;
    }

    public int getPagingCount() {
        return mSettings.commentsPerPage;
    }

    public void setManualApproval(boolean required) {
        mSettings.commentApprovalRequired = required;
    }

    public boolean getManualApproval() {
        return mSettings.commentApprovalRequired;
    }

    public void setIdentityRequired(boolean required) {
        mSettings.commentsRequireIdentity = required;
    }

    public boolean getIdentityRequired() {
        return mSettings.commentsRequireIdentity;
    }

    public void setUserAccountRequired(boolean required) {
        mSettings.commentsRequireUserAccount = required;
    }

    public boolean getUserAccountRequired() {
        return mSettings.commentsRequireUserAccount;
    }

    public void setUseCommentWhitelist(boolean useWhitelist) {
        mSettings.commentAutoApprovalKnownUsers = useWhitelist;
    }

    public boolean getUseCommentWhitelist() {
        return mSettings.commentAutoApprovalKnownUsers;
    }

    public void setMultipleLinks(int count) {
        mSettings.maxLinks = count;
    }

    public int getMultipleLinks() {
        return mSettings.maxLinks;
    }

    public void setModerationKeys(List<String> keys) {
        mSettings.holdForModeration = keys;
    }

    public List<String> getModerationKeys() {
        if (mSettings.holdForModeration == null) return new ArrayList<>();
        return mSettings.holdForModeration;
    }

    public void setBlacklistKeys(List<String> keys) {
        mSettings.blacklist = keys;
    }

    public List<String> getBlacklistKeys() {
        if (mSettings.blacklist == null) return new ArrayList<>();
        return mSettings.blacklist;
    }

    public void setDefaultCategory(int category) {
        mSettings.defaultCategory = category;
    }

    /**
     * Sets the default post format.
     *
     * @param format
     * if null or empty default format is set to {@link SiteSettingsInterface#STANDARD_POST_FORMAT}
     */
    public void setDefaultFormat(String format) {
        if (TextUtils.isEmpty(format)) {
            mSettings.defaultPostFormat = STANDARD_POST_FORMAT;
        } else {
            mSettings.defaultPostFormat = format.toLowerCase();
        }
    }

    public String getDefaultFormat() {
        return TextUtils.isEmpty(mSettings.defaultPostFormat) ? STANDARD_POST_FORMAT : mSettings.defaultPostFormat;
    }

    public void setShowRelatedPosts(boolean relatedPosts) {
        mSettings.showRelatedPosts = relatedPosts;
    }

    public void setShowRelatedPostHeader(boolean showHeader) {
        mSettings.showRelatedPostHeader = showHeader;
    }

    public void setShowRelatedPostImages(boolean showImages) {
        mSettings.showRelatedPostImages = showImages;
    }

    /**
     * Checks if the provided list of post format IDs is the same (order dependent) as the current
     * list of Post Formats in the local settings object.
     *
     * @param ids
     * an array of post format IDs
     * @return
     * true unless the provided IDs are different from the current IDs or in a different order
     */
    public boolean isSameFormatList(CharSequence[] ids) {
        if (ids == null) return mSettings.postFormats == null;
        if (mSettings.postFormats == null || ids.length != mSettings.postFormats.size()) return false;

        String[] keys = mSettings.postFormats.keySet().toArray(new String[mSettings.postFormats.size()]);
        for (int i = 0; i < ids.length; ++i) {
            if (!keys[i].equals(ids[i])) return false;
        }

        return true;
    }

    /**
     * Checks if the provided list of category IDs is the same (order dependent) as the current
     * list of Categories in the local settings object.
     *
     * @param ids
     * an array of integers stored as Strings (for convenience)
     * @return
     * true unless the provided IDs are different from the current IDs or in a different order
     */
    public boolean isSameCategoryList(CharSequence[] ids) {
        if (ids == null) return mSettings.categories == null;
        if (mSettings.categories == null || ids.length != mSettings.categories.length) return false;

        for (int i = 0; i < ids.length; ++i) {
            if (Integer.valueOf(ids[i].toString()) != mSettings.categories[i].id) return false;
        }

        return true;
    }

    /**
     * Needed so that subclasses can be created before initializing. The final member variables
     * are null until object has been created so XML-RPC callbacks will not run.
     *
     * @return
     * returns itself for the convenience of
     * {@link SiteSettingsInterface#getInterface(Activity, Blog, SiteSettingsListener)}
     */
    public SiteSettingsInterface init(boolean fetchRemote) {
        generateLanguageMap();
        initSettings(fetchRemote);

        return this;
    }

    /**
     * Notifies listener that settings have been updated with the latest remote data.
     */
    protected void notifyUpdatedOnUiThread(final Exception error) {
        if (mActivity == null || mListener == null) return;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListener.onSettingsUpdated(error);
            }
        });
    }

    /**
     * Notifies listener that settings have been saved or an error occurred while saving.
     */
    protected void notifySavedOnUiThread(final Exception error) {
        if (mActivity == null || mListener == null) return;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListener.onSettingsSaved(error);
            }
        });
    }

    /**
     * If there is a change in verification status the listener is notified.
     */
    protected void credentialsVerified(boolean valid) {
        if (valid) {
            if (!mSettings.hasVerifiedCredentials) {
                notifyCredentialsVerifiedOnUiThread(null);
            }
            mRemoteSettings.hasVerifiedCredentials = mSettings.hasVerifiedCredentials = true;
        } else {
            if (mSettings.hasVerifiedCredentials) {
                notifyCredentialsVerifiedOnUiThread(new AuthenticationError());
            }
            mRemoteSettings.hasVerifiedCredentials = mSettings.hasVerifiedCredentials = false;
        }
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
     * Language IDs, used only by WordPress, are integer values that map to a language code.
     * https://github.com/Automattic/calypso-pre-oss/blob/72c2029b0805a73b749a2b64dd1d8655cae528d0/config/production.json#L86-L227
     *
     * Language codes are unique two-letter identifiers defined by ISO 639-1. Region dialects can
     * be defined by appending a -** where ** is the region code (en-GB -> English, Great Britain).
     * https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
     */
    protected String languageIdToLanguageCode(String id) {
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
     * Need to defer loading the cached settings to a thread so it completes after initialization.
     */
    private void loadCachedSettings() {
        Cursor localSettings = SiteSettingsTable.getSettings(mBlog.getRemoteBlogId());

        if (localSettings != null) {
            Map<Integer, CategoryModel> cachedModels = SiteSettingsTable.getAllCategories();
            mSettings.deserializeOptionsDatabaseCursor(localSettings, cachedModels);
            mSettings.language = languageIdToLanguageCode(Integer.toString(mSettings.languageId));
            if (mSettings.language == null) {
                setLanguageCode(Locale.getDefault().getLanguage());
            }
            mRemoteSettings.language = mSettings.language;
            mRemoteSettings.languageId = mSettings.languageId;
            mRemoteSettings.location = mSettings.location;
            localSettings.close();
            notifyUpdatedOnUiThread(null);
        } else {
            mSettings.isInLocalTable = false;
            setAddress(mBlog.getHomeURL());
            setUsername(mBlog.getUsername());
            setPassword(mBlog.getPassword());
            setTitle(mBlog.getBlogName());
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
                credentialsVerified(true);

                if (result != null && result instanceof HashMap) {
                    Map<?, ?> resultMap = (HashMap<?, ?>) result;
                    Map allFormats;
                    Object[] supportedFormats;
                    if (resultMap.containsKey("supported")) {
                        allFormats = (Map) resultMap.get("all");
                        supportedFormats = (Object[]) resultMap.get("supported");
                    } else {
                        allFormats = resultMap;
                        supportedFormats = allFormats.keySet().toArray();
                    }

                    mRemoteSettings.postFormats = new HashMap<>();
                    mRemoteSettings.postFormats.put("standard", "Standard");
                    for (Object supportedFormat : supportedFormats) {
                        if (allFormats.containsKey(supportedFormat)) {
                            mRemoteSettings.postFormats.put(supportedFormat.toString(), allFormats.get(supportedFormat).toString());
                        }
                    }
                    mSettings.postFormats = new HashMap<>(mRemoteSettings.postFormats);
                    String[] formatKeys = new String[mRemoteSettings.postFormats.size()];
                    mRemoteSettings.postFormatKeys = mRemoteSettings.postFormats.keySet().toArray(formatKeys);
                    mSettings.postFormatKeys = mRemoteSettings.postFormatKeys.clone();

                    notifyUpdatedOnUiThread(null);
                }
            }

            @Override
            public void onFailure(long id, Exception error) {
            }
        }, ApiHelper.Methods.GET_POST_FORMATS, params);
    }

    /**
     * Notifies listener that credentials have been validated or are incorrect.
     */
    private void notifyCredentialsVerifiedOnUiThread(final Exception error) {
        if (mActivity == null || mListener == null) return;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListener.onCredentialsValidated(error);
            }
        });
    }

    /**
     * Attempts to load cached settings for the blog then fetches remote settings.
     */
    private void initSettings(boolean fetchRemote) {
        loadCachedSettings();

        if (fetchRemote) {
            fetchRemoteData();
            fetchPostFormats();
        }
    }

    /**
     * Creates a map from language codes to WordPress language IDs.
     */
    private void generateLanguageMap() {
        // Generate map of language codes
        String[] languageIds = mActivity.getResources().getStringArray(R.array.lang_ids);
        String[] languageCodes = mActivity.getResources().getStringArray(R.array.language_codes);
        for (int i = 0; i < languageIds.length && i < languageCodes.length; ++i) {
            mLanguageCodes.put(languageCodes[i], languageIds[i]);
        }
    }
}
