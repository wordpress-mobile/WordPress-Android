package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.SiteSettingsModel;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPPrefUtils;
import org.xmlrpc.android.ApiHelper.Method;
import org.xmlrpc.android.ApiHelper.Param;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for WordPress (.com and .org) Site Settings. The {@link SiteSettingsModel} class is
 * used to store the following settings:
 *
 * - Title
 * - Tagline
 * - Address
 * - Privacy
 * - Language
 * - Username (.org only)
 * - Password (.org only)
 * - Location (local device setting, not saved remotely)
 * - Default Category
 * - Default Format
 * - Related Posts
 * - Allow Comments
 * - Send Pingbacks
 * - Receive Pingbacks
 * - Identity Required
 * - User Account Required
 * - Close Comments After
 * - Comment Sort Order
 * - Comment Threading
 * - Comment Paging
 * - Comment User Whitelist
 * - Comment Link Limit
 * - Comment Moderation Hold Filter
 * - Comment Blacklist Filter
 *
 * This class is marked abstract. This is due to the fact that .org (self-hosted) and .com sites
 * expose different API's to query and edit their respective settings (even though the options
 * offered by each is roughly the same). To get an instance of this interface class use the
 * {@link SiteSettingsInterface#getInterface(Activity, Blog, SiteSettingsListener)} method. It will
 * determine which interface ({@link SelfHostedSiteSettings} or {@link DotComSiteSettings}) is
 * appropriate for the given blog.
 */

public abstract class SiteSettingsInterface {

    /**
     * Name of the {@link SharedPreferences} that is used to store local settings.
     */
    public static final String SITE_SETTINGS_PREFS = "site-settings-prefs";

    /**
     * Key used to access the language preference stored in {@link SharedPreferences}.
     */
    public static final String LANGUAGE_PREF_KEY = "site-settings-language-pref";

    /**
     * Key used to access the location preference stored in {@link SharedPreferences}.
     */
    public static final String LOCATION_PREF_KEY = "site-settings-location-pref";

    /**
     * Key used to access the default category preference stored in {@link SharedPreferences}.
     */
    public static final String DEF_CATEGORY_PREF_KEY = "site-settings-category-pref";

    /**
     * Key used to access the default post format preference stored in {@link SharedPreferences}.
     */
    public static final String DEF_FORMAT_PREF_KEY = "site-settings-format-pref";

    /**
     * Identifies an Ascending (oldest to newest) sort order.
     */
    public static final int ASCENDING_SORT = 0;

    /**
     * Identifies an Descending (newest to oldest) sort order.
     */
    public static final int DESCENDING_SORT = 1;

    /**
     * Used to prefix keys in an analytics property list.
     */
    protected static final String SAVED_ITEM_PREFIX = "item_saved_";

    /**
     * Key for the Standard post format. Used as default if post format is not set/known.
     */
    private static final String STANDARD_POST_FORMAT_KEY = "standard";

    /**
     * Standard post format value. Used as default display value if post format is unknown.
     */
    private static final String STANDARD_POST_FORMAT = "Standard";

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

    /**
     * Returns an instance of the {@link this#SITE_SETTINGS_PREFS} {@link SharedPreferences}.
     */
    public static SharedPreferences siteSettingsPreferences(Context context) {
        return context.getSharedPreferences(SITE_SETTINGS_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Gets the geo-tagging value stored in {@link SharedPreferences}, false by default.
     */
    public static boolean getGeotagging(Context context) {
        return siteSettingsPreferences(context).getBoolean(LOCATION_PREF_KEY, false);
    }

    /**
     * Gets the default category value stored in {@link SharedPreferences}, 0 by default.
     */
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

    /**
     * Gets the default post format value stored in {@link SharedPreferences}, "" by default.
     */
    public static String getDefaultFormat(Context context) {
        return siteSettingsPreferences(context).getString(DEF_FORMAT_PREF_KEY, "");
    }

    /**
     * Thrown when provided credentials are not valid.
     */
    public class AuthenticationError extends Exception { }

    /**
     * Interface callbacks for settings events.
     */
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

    /**
     * {@link SiteSettingsInterface} implementations should use this method to start a background
     * task to load settings data from a remote source.
     */
    protected abstract void fetchRemoteData();

    protected final Activity mActivity;
    protected final Blog mBlog;
    protected final SiteSettingsListener mListener;
    protected final SiteSettingsModel mSettings;
    protected final SiteSettingsModel mRemoteSettings;

    private final Map<String, String> mLanguageCodes;

    protected SiteSettingsInterface(Activity host, Blog blog, SiteSettingsListener listener) {
        mActivity = host;
        mBlog = blog;
        mListener = listener;
        mSettings = new SiteSettingsModel();
        mRemoteSettings = new SiteSettingsModel();
        mLanguageCodes = WPPrefUtils.generateLanguageMap(host);
    }

    public void saveSettings() {
        SiteSettingsTable.saveSettings(mSettings);
        siteSettingsPreferences(mActivity).edit().putString(LANGUAGE_PREF_KEY, mSettings.language).apply();
        siteSettingsPreferences(mActivity).edit().putBoolean(LOCATION_PREF_KEY, mSettings.location).apply();
        siteSettingsPreferences(mActivity).edit().putInt(DEF_CATEGORY_PREF_KEY, mSettings.defaultCategory).apply();
        siteSettingsPreferences(mActivity).edit().putString(DEF_FORMAT_PREF_KEY, mSettings.defaultPostFormat).apply();
    }

    public @NonNull String getTitle() {
        return mSettings.title == null ? "" : mSettings.title;
    }

    public @NonNull String getTagline() {
        return mSettings.tagline == null ? "" : mSettings.tagline;
    }

    public @NonNull String getAddress() {
        return mSettings.address == null ? "" : mSettings.address;
    }

    public int getPrivacy() {
        return mSettings.privacy;
    }

    public @NonNull String getPrivacyDescription() {
        if (mActivity != null) {
            switch (getPrivacy()) {
                case -1:
                    return mActivity.getString(R.string.site_settings_privacy_private_summary);
                case 0:
                    return mActivity.getString(R.string.site_settings_privacy_hidden_summary);
                case 1:
                    return mActivity.getString(R.string.site_settings_privacy_public_summary);
            }
        }
        return "";
    }

    public @NonNull String getLanguageCode() {
        return mSettings.language == null ? "" : mSettings.language;
    }

    public @NonNull String getUsername() {
        return mSettings.username == null ? "" : mSettings.username;
    }

    public @NonNull String getPassword() {
        return mSettings.password == null ? "" : mSettings.password;
    }

    public boolean getLocation() {
        return mSettings.location;
    }

    public @NonNull Map<String, String> getFormats() {
        if (mSettings.postFormats == null) mSettings.postFormats = new HashMap<>();
        return mSettings.postFormats;
    }

    public @NonNull CategoryModel[] getCategories() {
        if (mSettings.categories == null) mSettings.categories = new CategoryModel[0];
        return mSettings.categories;
    }

    public @NonNull Map<Integer, String> getCategoryNames() {
        Map<Integer, String> categoryNames = new HashMap<>();
        if (mSettings.categories != null && mSettings.categories.length > 0) {
            for (CategoryModel model : mSettings.categories) {
                categoryNames.put(model.id, Html.fromHtml(model.name).toString());
            }
        }

        return categoryNames;
    }

    public int getDefaultCategory() {
        return mSettings.defaultCategory;
    }

    public @NonNull String getDefaultCategoryForDisplay() {
        for (CategoryModel model : getCategories()) {
            if (model != null && model.id == getDefaultCategory()) {
                return Html.fromHtml(model.name).toString();
            }
        }

        return "";
    }

    public @NonNull String getDefaultPostFormat() {
        if (TextUtils.isEmpty(mSettings.defaultPostFormat) || !getFormats().containsKey(mSettings.defaultPostFormat)) {
            mSettings.defaultPostFormat = STANDARD_POST_FORMAT_KEY;
        }
        return mSettings.defaultPostFormat;
    }

    public @NonNull String getDefaultPostFormatDisplay() {
        String defaultFormat = getFormats().get(getDefaultPostFormat());
        if (TextUtils.isEmpty(defaultFormat)) defaultFormat = STANDARD_POST_FORMAT;
        return defaultFormat;
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

    public @NonNull String getRelatedPostsDescription() {
        if (mActivity == null) return "";
        String desc = mActivity.getString(getShowRelatedPosts() ? R.string.on : R.string.off);
        return StringUtils.capitalize(desc);
    }

    public boolean getAllowComments() {
        return mSettings.allowComments;
    }

    public boolean getSendPingbacks() {
        return mSettings.sendPingbacks;
    }

    public boolean getReceivePingbacks() {
        return mSettings.receivePingbacks;
    }

    public boolean getShouldCloseAfter() {
        return mSettings.shouldCloseAfter;
    }

    public int getCloseAfter() {
        return mSettings.closeCommentAfter;
    }

    public @NonNull String getCloseAfterDescriptionForPeriod() {
        return getCloseAfterDescriptionForPeriod(getCloseAfter());
    }

    public int getCloseAfterPeriodForDescription() {
        return !getShouldCloseAfter() ? 0 : getCloseAfter();
    }

    public @NonNull String getCloseAfterDescription() {
        return getCloseAfterDescriptionForPeriod(getCloseAfterPeriodForDescription());
    }

    public @NonNull String getCloseAfterDescriptionForPeriod(int period) {
        if (mActivity == null) return "";

        if (!getShouldCloseAfter()) return mActivity.getString(R.string.never);

        return StringUtils.getQuantityString(mActivity, R.string.never, R.string.days_quantity_one,
                R.string.days_quantity_other, period);
    }

    public int getCommentSorting() {
        return mSettings.sortCommentsBy;
    }

    public @NonNull String getSortingDescription() {
        if (mActivity == null) return "";

        int order = getCommentSorting();
        switch (order) {
            case SiteSettingsInterface.ASCENDING_SORT:
                return mActivity.getString(R.string.oldest_first);
            case SiteSettingsInterface.DESCENDING_SORT:
                return mActivity.getString(R.string.newest_first);
            default:
                return mActivity.getString(R.string.unknown);
        }
    }

    public boolean getShouldThreadComments() {
        return mSettings.shouldThreadComments;
    }

    public int getThreadingLevels() {
        return mSettings.threadingLevels;
    }

    public int getThreadingLevelsForDescription() {
        return !getShouldThreadComments() ? 1 : getThreadingLevels();
    }

    public @NonNull String getThreadingDescription() {
        return getThreadingDescriptionForLevel(getThreadingLevelsForDescription());
    }

    public @NonNull String getThreadingDescriptionForLevel(int level) {
        if (mActivity == null) return "";

        if (level <= 1) return mActivity.getString(R.string.none);
        return String.format(mActivity.getString(R.string.site_settings_threading_summary), level);
    }

    public boolean getShouldPageComments() {
        return mSettings.shouldPageComments;
    }

    public int getPagingCount() {
        return mSettings.commentsPerPage;
    }

    public int getPagingCountForDescription() {
        return !getShouldPageComments() ? 0 : getPagingCount();
    }

    public @NonNull String getPagingDescription() {
        if (mActivity == null) return "";

        if (!getShouldPageComments()) {
            return mActivity.getString(R.string.disabled);
        }

        int count = getPagingCountForDescription();
        return StringUtils.getQuantityString(mActivity, R.string.none, R.string.site_settings_paging_summary_one,
                R.string.site_settings_paging_summary_other, count);
    }

    public boolean getManualApproval() {
        return mSettings.commentApprovalRequired;
    }

    public boolean getIdentityRequired() {
        return mSettings.commentsRequireIdentity;
    }

    public boolean getUserAccountRequired() {
        return mSettings.commentsRequireUserAccount;
    }

    public boolean getUseCommentWhitelist() {
        return mSettings.commentAutoApprovalKnownUsers;
    }

    public int getMultipleLinks() {
        return mSettings.maxLinks;
    }

    public @NonNull List<String> getModerationKeys() {
        if (mSettings.holdForModeration == null) mSettings.holdForModeration = new ArrayList<>();
        return mSettings.holdForModeration;
    }

    public @NonNull String getModerationHoldDescription() {
        return getKeysDescription(getModerationKeys().size());
    }

    public @NonNull List<String> getBlacklistKeys() {
        if (mSettings.blacklist == null) mSettings.blacklist = new ArrayList<>();
        return mSettings.blacklist;
    }

    public @NonNull String getBlacklistDescription() {
        return getKeysDescription(getBlacklistKeys().size());
    }

    public @NonNull String getKeysDescription(int count) {
        if (mActivity == null) return "";

        return StringUtils.getQuantityString(mActivity, R.string.site_settings_list_editor_no_items_text,
                R.string.site_settings_list_editor_summary_one,
                R.string.site_settings_list_editor_summary_other, count);

    }

    public void setTitle(String title) {
        mSettings.title = title;
    }

    public void setTagline(String tagline) {
        mSettings.tagline = tagline;
    }

    public void setAddress(String address) {
        mSettings.address = address;
    }

    public void setPrivacy(int privacy) {
        mSettings.privacy = privacy;
    }

    public boolean setLanguageCode(String languageCode) {
        if (!mLanguageCodes.containsKey(languageCode) ||
            TextUtils.isEmpty(mLanguageCodes.get(languageCode))) return false;
        mSettings.language = languageCode;
        mSettings.languageId = Integer.valueOf(mLanguageCodes.get(languageCode));
        return true;
    }

    public void setLanguageId(int languageId) {
        // want to prevent O(n) language code lookup if there is no change
        if (mSettings.languageId != languageId) {
            mSettings.languageId = languageId;
            mSettings.language = languageIdToLanguageCode(Integer.toString(languageId));
        }
    }

    public void setUsername(String username) {
        mSettings.username = username;
    }

    public void setPassword(String password) {
        mSettings.password = password;
    }

    public void setLocation(boolean location) {
        mSettings.location = location;
    }

    public void setAllowComments(boolean allowComments) {
        mSettings.allowComments = allowComments;
    }

    public void setSendPingbacks(boolean sendPingbacks) {
        mSettings.sendPingbacks = sendPingbacks;
    }

    public void setReceivePingbacks(boolean receivePingbacks) {
        mSettings.receivePingbacks = receivePingbacks;
    }

    public void setShouldCloseAfter(boolean shouldCloseAfter) {
        mSettings.shouldCloseAfter = shouldCloseAfter;
    }

    public void setCloseAfter(int period) {
        mSettings.closeCommentAfter = period;
    }

    public void setCommentSorting(int method) {
        mSettings.sortCommentsBy = method;
    }

    public void setShouldThreadComments(boolean shouldThread) {
        mSettings.shouldThreadComments = shouldThread;
    }

    public void setThreadingLevels(int levels) {
        mSettings.threadingLevels = levels;
    }

    public void setShouldPageComments(boolean shouldPage) {
        mSettings.shouldPageComments= shouldPage;
    }

    public void setPagingCount(int count) {
        mSettings.commentsPerPage = count;
    }

    public void setManualApproval(boolean required) {
        mSettings.commentApprovalRequired = required;
    }

    public void setIdentityRequired(boolean required) {
        mSettings.commentsRequireIdentity = required;
    }

    public void setUserAccountRequired(boolean required) {
        mSettings.commentsRequireUserAccount = required;
    }

    public void setUseCommentWhitelist(boolean useWhitelist) {
        mSettings.commentAutoApprovalKnownUsers = useWhitelist;
    }

    public void setMultipleLinks(int count) {
        mSettings.maxLinks = count;
    }

    public void setModerationKeys(List<String> keys) {
        mSettings.holdForModeration = keys;
    }

    public void setBlacklistKeys(List<String> keys) {
        mSettings.blacklist = keys;
    }

    public void setDefaultCategory(int category) {
        mSettings.defaultCategory = category;
    }

    /**
     * Sets the default post format.
     *
     * @param format
     * if null or empty default format is set to {@link SiteSettingsInterface#STANDARD_POST_FORMAT_KEY}
     */
    public void setDefaultFormat(String format) {
        if (TextUtils.isEmpty(format)) {
            mSettings.defaultPostFormat = STANDARD_POST_FORMAT_KEY;
        } else {
            mSettings.defaultPostFormat = format.toLowerCase();
        }
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
     * Determines if the current Moderation Hold list contains a given value.
     */
    public boolean moderationHoldListContains(String value) {
        return getModerationKeys().contains(value);
    }

    /**
     * Determines if the current Blacklist list contains a given value.
     */
    public boolean blacklistListContains(String value) {
        return getBlacklistKeys().contains(value);
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
        loadCachedSettings();

        if (fetchRemote) {
            fetchRemoteData();
            fetchPostFormats();
        }

        return this;
    }

    /**
     * If there is a change in verification status the listener is notified.
     */
    protected void credentialsVerified(boolean valid) {
        Exception e = valid ? null : new AuthenticationError();
        if (mSettings.hasVerifiedCredentials != valid) notifyCredentialsVerifiedOnUiThread(e);
        mRemoteSettings.hasVerifiedCredentials = mSettings.hasVerifiedCredentials = valid;
    }

    /**
     * Helper method to create an XML-RPC interface for the current blog.
     */
    protected XMLRPCClientInterface instantiateInterface() {
        if (mBlog == null) return null;
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
                setLanguageCode(LanguageUtils.getPatchedCurrentDeviceLanguage(null));
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
        args.put(Param.SHOW_SUPPORTED_POST_FORMATS, "true");
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
                    SiteSettingsTable.saveSettings(mSettings);

                    notifyUpdatedOnUiThread(null);
                }
            }

            @Override
            public void onFailure(long id, Exception error) {
            }
        }, Method.GET_POST_FORMATS, params);
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
     * Notifies listener that settings have been updated with the latest remote data.
     */
    protected void notifyUpdatedOnUiThread(final Exception error) {
        if (mActivity == null || mActivity.isFinishing() || mListener == null) return;

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
}
