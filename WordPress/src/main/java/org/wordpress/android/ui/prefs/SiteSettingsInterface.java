package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnAllSitesMobileEditorChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnPostFormatsChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteEditorsChanged;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.JetpackSettingsModel;
import org.wordpress.android.models.SiteSettingsModel;
import org.wordpress.android.ui.plans.PlansConstants;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

/**
 * Interface for WordPress (.com and .org) Site Settings. The {@link SiteSettingsModel} class is
 * used to store the following settings:
 * <p>
 * - Title
 * - Tagline
 * - Address
 * - Privacy
 * - Language
 * - Site icon
 * - Username (.org only)
 * - Password (.org only)
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
 * <p>
 * This class is marked abstract. This is due to the fact that .org (self-hosted) and .com sites
 * expose different API's to query and edit their respective settings (even though the options
 * offered by each is roughly the same). To get an instance of this interface class use the
 * {@link SiteSettingsInterface#getInterface(Context, SiteModel, SiteSettingsListener)} method.
 */

public abstract class SiteSettingsInterface {
    /**
     * Identifies an Ascending (oldest to newest) sort order.
     */
    static final int ASCENDING_SORT = 0;

    /**
     * Identifies an Descending (newest to oldest) sort order.
     */
    static final int DESCENDING_SORT = 1;

    /**
     * Used to prefix keys in an analytics property list.
     */
    static final String SAVED_ITEM_PREFIX = "item_saved_";

    /**
     * Key for the Standard post format. Used as default if post format is not set/known.
     */
    static final String STANDARD_POST_FORMAT_KEY = "standard";

    /**
     * Standard sharing button style value. Used as default value if button style is unknown.
     */
    private static final String STANDARD_SHARING_BUTTON_STYLE = "icon-text";

    /**
     * Standard post format value. Used as default display value if post format is unknown.
     */
    private static final String STANDARD_POST_FORMAT = "Standard";

    /**
     * Instantiates the appropriate (self-hosted or .com) SiteSettingsInterface.
     */
    @Nullable
    public static SiteSettingsInterface getInterface(Context host, SiteModel site, SiteSettingsListener listener) {
        if (host == null || site == null) {
            return null;
        }

        if (SiteUtils.isAccessedViaWPComRest(site)) {
            return new WPComSiteSettings(host, site, listener);
        }

        return new SelfHostedSiteSettings(host, site, listener);
    }

    /**
     * Thrown when provided credentials are not valid.
     */
    public class AuthenticationError extends Exception {
    }

    /**
     * Interface callbacks for settings events.
     */
    public interface SiteSettingsListener {
        void onSaveError(Exception error);

        void onFetchError(Exception error);

        /**
         * Called when settings have been updated with remote changes.
         */
        void onSettingsUpdated();

        /**
         * Called when attempt to update remote settings is finished.
         */
        void onSettingsSaved();

        /**
         * Called when a request to validate current credentials has completed.
         *
         * @param error null if successful
         */
        void onCredentialsValidated(Exception error);
    }

    /**
     * {@link SiteSettingsInterface} implementations should use this method to start a background
     * task to load settings data from a remote source.
     */
    protected abstract void fetchRemoteData();

    protected Context mContext;
    protected final SiteModel mSite;
    protected final SiteSettingsListener mListener;
    protected final SiteSettingsModel mSettings;
    protected final SiteSettingsModel mRemoteSettings;
    protected final JetpackSettingsModel mJpSettings;
    protected final JetpackSettingsModel mRemoteJpSettings;
    private final Map<String, String> mLanguageCodes;

    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

    protected SiteSettingsInterface(Context host, SiteModel site, SiteSettingsListener listener) {
        ((WordPress) host.getApplicationContext()).component().inject(this);
        mDispatcher.register(this);
        mContext = host;
        mSite = site;
        mListener = listener;
        mSettings = new SiteSettingsModel();
        mRemoteSettings = new SiteSettingsModel();
        mJpSettings = new JetpackSettingsModel();
        mRemoteJpSettings = new JetpackSettingsModel();
        mLanguageCodes = LocaleManager.generateLanguageMap(host);
    }

    @Override
    protected void finalize() throws Throwable {
        mDispatcher.unregister(this);
        super.finalize();
    }

    public void clear() {
        mDispatcher.unregister(this);
        mContext = null;
    }

    public void saveSettings() {
        SiteSettingsTable.saveSettings(mSettings);
    }

    public int getLocalSiteId() {
        return mSite.getId();
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

    public @NonNull String getQuotaDiskSpace() {
        return mSettings.quotaDiskSpace == null ? "" : mSettings.quotaDiskSpace;
    }

    public int getPrivacy() {
        return mSettings.privacy;
    }

    public @NonNull String getPrivacyDescription() {
        if (mContext != null) {
            switch (getPrivacy()) {
                case -1:
                    return mContext.getString(R.string.site_settings_privacy_private_summary);
                case 0:
                    return mContext.getString(R.string.site_settings_privacy_hidden_summary);
                case 1:
                    return mContext.getString(R.string.site_settings_privacy_public_summary);
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

    public @NonNull Map<String, String> getFormats() {
        mSettings.postFormats = new HashMap<>();
        String[] postFormatDisplayNames = mContext.getResources().getStringArray(R.array.post_format_display_names);
        String[] postFormatKeys = mContext.getResources().getStringArray(R.array.post_format_keys);
        // Add standard post format (only for .com)
        mSettings.postFormats.put(STANDARD_POST_FORMAT_KEY, STANDARD_POST_FORMAT);
        // Add default post formats
        for (int i = 0; i < postFormatKeys.length && i < postFormatDisplayNames.length; ++i) {
            mSettings.postFormats.put(postFormatKeys[i], postFormatDisplayNames[i]);
        }
        if (mSite == null) {
            return mSettings.postFormats;
        }
        // Add (or replace) site-specific post formats
        List<PostFormatModel> postFormats = mSiteStore.getPostFormats(mSite);
        for (PostFormatModel postFormat : postFormats) {
            mSettings.postFormats.put(postFormat.getSlug(), postFormat.getDisplayName());
        }
        return mSettings.postFormats;
    }

    public @NonNull CategoryModel[] getCategories() {
        if (mSettings.categories == null) {
            mSettings.categories = new CategoryModel[0];
        }
        return mSettings.categories;
    }

    public @NonNull SparseArrayCompat<String> getCategoryNames() {
        SparseArrayCompat<String> categoryNames = new SparseArrayCompat<>();
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
        if (TextUtils.isEmpty(defaultFormat)) {
            defaultFormat = STANDARD_POST_FORMAT;
        }
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
        if (mContext == null) {
            return "";
        }
        String desc = mContext.getString(getShowRelatedPosts() ? R.string.on : R.string.off);
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
        if (mContext == null) {
            return "";
        }

        if (!getShouldCloseAfter()) {
            return mContext.getString(R.string.never);
        }

        return StringUtils.getQuantityString(mContext, R.string.never, R.string.days_quantity_one,
                                             R.string.days_quantity_other, period);
    }

    public int getCommentSorting() {
        return mSettings.sortCommentsBy;
    }

    public @NonNull String getSortingDescription() {
        if (mContext == null) {
            return "";
        }

        int order = getCommentSorting();
        switch (order) {
            case SiteSettingsInterface.ASCENDING_SORT:
                return mContext.getString(R.string.oldest_first);
            case SiteSettingsInterface.DESCENDING_SORT:
                return mContext.getString(R.string.newest_first);
            default:
                return mContext.getString(R.string.unknown);
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
        if (mContext == null) {
            return "";
        }

        if (level <= 1) {
            return mContext.getString(R.string.none);
        }
        return String.format(mContext.getString(R.string.site_settings_threading_summary), level);
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
        if (mContext == null) {
            return "";
        }

        if (!getShouldPageComments()) {
            return mContext.getString(R.string.disabled);
        }

        int count = getPagingCountForDescription();
        return StringUtils.getQuantityString(mContext, R.string.none, R.string.site_settings_paging_summary_one,
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
        if (mSettings.holdForModeration == null) {
            mSettings.holdForModeration = new ArrayList<>();
        }
        return mSettings.holdForModeration;
    }

    public @NonNull String getModerationHoldDescription() {
        return getKeysDescription(getModerationKeys().size());
    }

    public @NonNull List<String> getBlacklistKeys() {
        if (mSettings.blacklist == null) {
            mSettings.blacklist = new ArrayList<>();
        }
        return mSettings.blacklist;
    }

    public @NonNull String getBlacklistDescription() {
        return getKeysDescription(getBlacklistKeys().size());
    }

    public @NonNull String getJetpackProtectWhitelistSummary() {
        return getKeysDescription(getJetpackWhitelistKeys().size());
    }

    public String getSharingLabel() {
        return mSettings.sharingLabel;
    }

    public @NonNull String getSharingButtonStyle(Context context) {
        if (TextUtils.isEmpty(mSettings.sharingButtonStyle)) {
            mSettings.sharingButtonStyle = context.getResources().getStringArray(R.array.sharing_button_style_array)[0];
        }

        return mSettings.sharingButtonStyle;
    }

    public @NonNull String getSharingButtonStyleDisplayText(Context context) {
        String sharingButtonStyle = getSharingButtonStyle(context);
        String[] styleArray = context.getResources().getStringArray(R.array.sharing_button_style_array);
        String[] styleDisplayArray = context.getResources().getStringArray(R.array.sharing_button_style_display_array);
        for (int i = 0; i < styleArray.length; i++) {
            if (sharingButtonStyle.equals(styleArray[i])) {
                return styleDisplayArray[i];
            }
        }

        return styleDisplayArray[0];
    }

    public boolean getAllowReblogButton() {
        return mSettings.allowReblogButton;
    }

    public boolean getAllowLikeButton() {
        return mSettings.allowLikeButton;
    }

    public boolean getAllowCommentLikes() {
        // We have different settings for comment likes for wpcom and Jetpack sites
        return mSite.isJetpackConnected() ? mJpSettings.commentLikes : mSettings.allowCommentLikes;
    }

    public @NonNull String getTwitterUsername() {
        if (mSettings.twitterUsername == null) {
            mSettings.twitterUsername = "";
        }
        return mSettings.twitterUsername;
    }

    public @NonNull String getKeysDescription(int count) {
        if (mContext == null) {
            return "";
        }

        return StringUtils.getQuantityString(mContext, R.string.site_settings_list_editor_no_items_text,
                                             R.string.site_settings_list_editor_summary_one,
                                             R.string.site_settings_list_editor_summary_other, count);
    }

    public String getStartOfWeek() {
        return mSettings.startOfWeek;
    }

    public void setStartOfWeek(String startOfWeek) {
        mSettings.startOfWeek = startOfWeek;
    }

    public String getDateFormat() {
        return mSettings.dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        mSettings.dateFormat = dateFormat;
    }

    public String getTimeFormat() {
        return mSettings.timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        mSettings.timeFormat = timeFormat;
    }

    public String getTimezone() {
        return mSettings.timezone;
    }

    public void setTimezone(String timezone) {
        mSettings.timezone = timezone;
    }

    public int getPostsPerPage() {
        return mSettings.postsPerPage;
    }

    public void setPostsPerPage(int postsPerPage) {
        mSettings.postsPerPage = postsPerPage;
    }

    public boolean getAmpSupported() {
        return mSettings.ampSupported;
    }

    public void setAmpSupported(boolean supported) {
        mSettings.ampSupported = supported;
    }

    public boolean getAmpEnabled() {
        return mSettings.ampEnabled;
    }

    public void setAmpEnabled(boolean enabled) {
        mSettings.ampEnabled = enabled;
    }

    public boolean getJetpackSearchSupported() {
        return mSettings.jetpackSearchSupported;
    }

    public void setJetpackSearchSupported(boolean supported) {
        mSettings.jetpackSearchSupported = supported;
    }

    public boolean getJetpackSearchEnabled() {
        return mSettings.jetpackSearchEnabled;
    }

    public void setJetpackSearchEnabled(boolean enabled) {
        mSettings.jetpackSearchEnabled = enabled;
    }

    public boolean isJetpackMonitorEnabled() {
        return mJpSettings.monitorActive;
    }

    public boolean shouldSendJetpackMonitorEmailNotifications() {
        return mJpSettings.emailNotifications;
    }

    public boolean shouldSendJetpackMonitorWpNotifications() {
        return mJpSettings.wpNotifications;
    }

    public void enableJetpackMonitor(boolean monitorActive) {
        mJpSettings.monitorActive = monitorActive;
    }

    public void enableJetpackMonitorEmailNotifications(boolean emailNotifications) {
        mJpSettings.emailNotifications = emailNotifications;
    }

    public void enableJetpackMonitorWpNotifications(boolean wpNotifications) {
        mJpSettings.wpNotifications = wpNotifications;
    }

    public boolean isJetpackProtectEnabled() {
        return mJpSettings.jetpackProtectEnabled;
    }

    public void enableJetpackProtect(boolean enabled) {
        mJpSettings.jetpackProtectEnabled = enabled;
    }

    public @NonNull List<String> getJetpackWhitelistKeys() {
        return mJpSettings.jetpackProtectWhitelist;
    }

    public void setJetpackWhitelistKeys(@NonNull List<String> whitelistKeys) {
        mJpSettings.jetpackProtectWhitelist.clear();
        mJpSettings.jetpackProtectWhitelist.addAll(whitelistKeys);
    }

    public void enableJetpackSsoMatchEmail(boolean enabled) {
        mJpSettings.ssoMatchEmail = enabled;
    }

    public void enableJetpackSso(boolean enabled) {
        mJpSettings.ssoActive = enabled;
    }

    public boolean isJetpackSsoEnabled() {
        return mJpSettings.ssoActive;
    }

    public boolean isJetpackSsoMatchEmailEnabled() {
        return mJpSettings.ssoMatchEmail;
    }

    public void enableJetpackSsoTwoFactor(boolean enabled) {
        mJpSettings.ssoRequireTwoFactor = enabled;
    }

    public boolean isJetpackSsoTwoFactorEnabled() {
        return mJpSettings.ssoRequireTwoFactor;
    }

    void enableServeImagesFromOurServers(boolean enabled) {
        mJpSettings.serveImagesFromOurServers = enabled;
    }

    boolean isServeImagesFromOurServersEnabled() {
        return mJpSettings.serveImagesFromOurServers;
    }

    void enableServeStaticFilesFromOurServers(boolean enabled) {
        mJpSettings.serveStaticFilesFromOurServers = enabled;
    }

    boolean isServeStaticFilesFromOurServersEnabled() {
        return mJpSettings.serveStaticFilesFromOurServers;
    }

    void enableLazyLoadImages(boolean enabled) {
        mJpSettings.lazyLoadImages = enabled;
    }

    boolean isLazyLoadImagesEnabled() {
        return mJpSettings.lazyLoadImages;
    }

    void enableAdFreeHosting(boolean enabled) {
        mJpSettings.adFreeVideoHosting = enabled;
    }

    boolean isAdFreeHostingEnabled() {
        return mJpSettings.adFreeVideoHosting;
    }

    void enableImprovedSearch(boolean enabled) {
        mJpSettings.improvedSearch = enabled;
    }

    boolean isImprovedSearchEnabled() {
        return mJpSettings.improvedSearch;
    }

    public boolean isSharingModuleEnabled() {
        return mJpSettings.sharingEnabled;
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

    public void setQuotaDiskSpace(String quotaDiskSpace) {
        mSettings.quotaDiskSpace = quotaDiskSpace;
    }

    public void setPrivacy(int privacy) {
        mSettings.privacy = privacy;
    }

    public boolean setLanguageCode(String languageCode) {
        if (!mLanguageCodes.containsKey(languageCode)
            || TextUtils.isEmpty(mLanguageCodes.get(languageCode))) {
            return false;
        }
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

    public void setSiteIconMediaId(int siteIconMediaId) {
        mSettings.siteIconMediaId = siteIconMediaId;
    }


    public void setUsername(String username) {
        mSettings.username = username;
    }

    public void setPassword(String password) {
        mSettings.password = password;
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
        mSettings.shouldPageComments = shouldPage;
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

    public void setSharingLabel(String sharingLabel) {
        mSettings.sharingLabel = sharingLabel;
    }

    public void setSharingButtonStyle(String sharingButtonStyle) {
        if (TextUtils.isEmpty(sharingButtonStyle)) {
            mSettings.sharingButtonStyle = STANDARD_SHARING_BUTTON_STYLE;
        } else {
            mSettings.sharingButtonStyle = sharingButtonStyle.toLowerCase(Locale.ROOT);
        }
    }

    public void setAllowReblogButton(boolean allowReblogButton) {
        mSettings.allowReblogButton = allowReblogButton;
    }

    public void setAllowLikeButton(boolean allowLikeButton) {
        mSettings.allowLikeButton = allowLikeButton;
    }

    public void setAllowCommentLikes(boolean allowCommentLikes) {
        // We have different settings for comment likes for wpcom and Jetpack sites
        if (mSite.isJetpackConnected()) {
            mJpSettings.commentLikes = allowCommentLikes;
        } else {
            mSettings.allowCommentLikes = allowCommentLikes;
        }
    }

    public void setTwitterUsername(String twitterUsername) {
        mSettings.twitterUsername = twitterUsername;
    }

    public void setDefaultCategory(int category) {
        mSettings.defaultCategory = category;
    }

    /**
     * Sets the default post format.
     *
     * @param format if null or empty default format is set to {@link SiteSettingsInterface#STANDARD_POST_FORMAT_KEY}
     */
    public void setDefaultFormat(String format) {
        if (TextUtils.isEmpty(format)) {
            mSettings.defaultPostFormat = STANDARD_POST_FORMAT_KEY;
        } else {
            mSettings.defaultPostFormat = format.toLowerCase(Locale.ROOT);
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
     * @param ids an array of post format IDs
     * @return true unless the provided IDs are different from the current IDs or in a different order
     */
    public boolean isSameFormatList(CharSequence[] ids) {
        if (ids == null) {
            return mSettings.postFormats == null;
        }
        if (mSettings.postFormats == null || ids.length != mSettings.postFormats.size()) {
            return false;
        }

        String[] keys = mSettings.postFormats.keySet().toArray(new String[mSettings.postFormats.size()]);
        for (int i = 0; i < ids.length; ++i) {
            if (!keys[i].equals(ids[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the provided list of category IDs is the same (order dependent) as the current
     * list of Categories in the local settings object.
     *
     * @param ids an array of integers stored as Strings (for convenience)
     * @return true unless the provided IDs are different from the current IDs or in a different order
     */
    public boolean isSameCategoryList(CharSequence[] ids) {
        if (ids == null) {
            return mSettings.categories == null;
        }
        if (mSettings.categories == null || ids.length != mSettings.categories.length) {
            return false;
        }

        for (int i = 0; i < ids.length; ++i) {
            if (Integer.valueOf(ids[i].toString()) != mSettings.categories[i].id) {
                return false;
            }
        }

        return true;
    }

    /**
     * Needed so that subclasses can be created before initializing. The final member variables
     * are null until object has been created so XML-RPC callbacks will not run.
     *
     * @return itself
     */
    public SiteSettingsInterface init(boolean fetchRemote) {
        loadCachedSettings();

        if (fetchRemote) {
            fetchRemoteData();
            mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(mSite));
            if (!AppPrefs.isDefaultAppWideEditorPreferenceSet()) {
                // Check if the migration from app-wide to per-site setting has already happened - v12.9->13.0
                // before fetching site editors from the remote
                mDispatcher.dispatch(SiteActionBuilder.newFetchSiteEditorsAction(mSite));
            }
        }

        return this;
    }

    /**
     * If there is a change in verification status the listener is notified.
     */
    protected void credentialsVerified(boolean valid) {
        Exception e = valid ? null : new AuthenticationError();
        if (mSettings.hasVerifiedCredentials != valid) {
            notifyCredentialsVerifiedOnUiThread(e);
        }
        mRemoteSettings.hasVerifiedCredentials = mSettings.hasVerifiedCredentials = valid;
    }

    /**
     * Language IDs, used only by WordPress, are integer values that map to a language code.
     * http://bit.ly/2H7gksN
     * <p>
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
        Cursor localSettings = SiteSettingsTable.getSettings(mSite.getId());

        if (localSettings != null && localSettings.getCount() > 0) {
            mSettings.isInLocalTable = true;
            SparseArrayCompat<CategoryModel> cachedModels = SiteSettingsTable.getAllCategories();
            mSettings.deserializeOptionsDatabaseCursor(localSettings, cachedModels);
            mSettings.language = languageIdToLanguageCode(Integer.toString(mSettings.languageId));
            if (mSettings.language == null) {
                setLanguageCode(LanguageUtils.getPatchedCurrentDeviceLanguage(null));
            }
            mRemoteSettings.language = mSettings.language;
            mRemoteSettings.languageId = mSettings.languageId;
            mRemoteSettings.siteIconMediaId = mSettings.siteIconMediaId;
        } else {
            mSettings.isInLocalTable = false;
            mSettings.localTableId = mSite.getId();
            setAddress(mSite.getUrl());
            setUsername(mSite.getUsername());
            setPassword(mSite.getPassword());
            setTitle(mSite.getName());
        }
        // Quota information always comes from the main site table
        if (mSite.hasDiskSpaceQuotaInformation()) {
            String percentage = FormatUtils.formatPercentage(mSite.getSpacePercentUsed() / 100);
            final String[] units = new String[] {mContext.getString(R.string.file_size_in_bytes),
                    mContext.getString(R.string.file_size_in_kilobytes),
                    mContext.getString(R.string.file_size_in_megabytes),
                    mContext.getString(R.string.file_size_in_gigabytes),
                    mContext.getString(R.string.file_size_in_terabytes) };

            String quotaAvailableSentence;
            if (mSite.getPlanId() == PlansConstants.BUSINESS_PLAN_ID) {
                String usedSpace = FormatUtils.formatFileSize(mSite.getSpaceUsed(), units);
                quotaAvailableSentence = String.format(mContext.getString(R.string.site_settings_quota_space_unlimited),
                        usedSpace);
            } else {
                String spaceAllowed = FormatUtils.formatFileSize(mSite.getSpaceAllowed(), units);
                quotaAvailableSentence = String.format(mContext.getString(R.string.site_settings_quota_space_value),
                        percentage, spaceAllowed);
            }
            setQuotaDiskSpace(quotaAvailableSentence);
        }
        // Self hosted always read account data from the main table
        if (!SiteUtils.isAccessedViaWPComRest(mSite)) {
            setUsername(mSite.getUsername());
            setPassword(mSite.getPassword());
        }

        if (localSettings != null) {
            localSettings.close();
        }
        notifyUpdatedOnUiThread();
    }

    /**
     * Notifies listener that credentials have been validated or are incorrect.
     */
    private void notifyCredentialsVerifiedOnUiThread(final Exception error) {
        if (mContext == null || mListener == null) {
            return;
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mListener.onCredentialsValidated(error);
            }
        });
    }

    protected void notifyFetchErrorOnUiThread(final Exception error) {
        if (mListener == null) {
            return;
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mListener.onFetchError(error);
            }
        });
    }

    protected void notifySaveErrorOnUiThread(final Exception error) {
        if (mListener == null) {
            return;
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mListener.onSaveError(error);
            }
        });
    }

    /**
     * Notifies listener that settings have been updated with the latest remote data.
     */
    protected void notifyUpdatedOnUiThread() {
        if (mListener == null) {
            return;
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mListener.onSettingsUpdated();
            }
        });
    }

    /**
     * Notifies listener that settings have been saved or an error occurred while saving.
     */
    protected void notifySavedOnUiThread() {
        if (mListener == null) {
            return;
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mListener.onSettingsSaved();
            }
        });
    }

    // FluxC OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostFormatsChanged(OnPostFormatsChanged event) {
        if (event.isError()) {
            AppLog.e(T.SETTINGS, "An error occurred while updating the post formats with type: " + event.error.type);
            return;
        }
        AppLog.v(T.SETTINGS, "Post formats successfully fetched!");
        notifyUpdatedOnUiThread();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteEditorsChanged(OnSiteEditorsChanged event) {
        // When the site editor details are loaded from the remote backend, make sure to set a default if empty
        if (event.isError()) {
            return;
        }
        updateAnalyticsAndUI();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAllSitesMobileEditorChanged(OnAllSitesMobileEditorChanged event) {
        if (event.isError()) {
            return;
        }
        if (event.isNetworkResponse) {
            // We can remove the global app setting now, since we're sure the migration ended with success.
            AppPrefs.removeAppWideEditorPreference();
        }
        updateAnalyticsAndUI();
    }

    private void updateAnalyticsAndUI() {
        // Need to update the user property about GB enabled on any of the sites
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);

        notifyUpdatedOnUiThread();
    }
}
