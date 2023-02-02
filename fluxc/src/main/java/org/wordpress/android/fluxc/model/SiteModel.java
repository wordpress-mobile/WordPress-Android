package org.wordpress.android.fluxc.model;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Table
@RawConstraints({"UNIQUE (SITE_ID, URL)"})
public class SiteModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = -7641813766771796252L;

    @Retention(SOURCE)
    @IntDef({ORIGIN_UNKNOWN, ORIGIN_WPCOM_REST, ORIGIN_XMLRPC, ORIGIN_WPAPI})
    public @interface SiteOrigin {}
    public static final int ORIGIN_UNKNOWN = 0;
    public static final int ORIGIN_WPCOM_REST = 1;
    public static final int ORIGIN_XMLRPC = 2;
    public static final int ORIGIN_WPAPI = 3;

    public static final long VIP_PLAN_ID = 31337;

    public static final String ACTIVE_MODULES_KEY_PUBLICIZE = "publicize";
    public static final String ACTIVE_MODULES_KEY_SHARING_BUTTONS = "sharedaddy";

    @PrimaryKey
    @Column private int mId;
    // Only given a value for wpcom and Jetpack sites - self-hosted sites use mSelfHostedSiteId
    @Column private long mSiteId;
    @Column private String mUrl;
    @Column private String mAdminUrl;
    @Column private String mLoginUrl;
    @Column private String mName;
    @Column private String mDescription;
    @Column private boolean mIsWPCom;
    @Column private boolean mIsWPComAtomic;
    @Column private boolean mIsFeaturedImageSupported;
    @Column private boolean mIsWpForTeamsSite;
    @Column private String mDefaultCommentStatus = "open";
    @Column private String mTimezone; // Expressed as an offset relative to GMT (e.g. '-8')
    @Column private String mFrameNonce; // only wpcom and Jetpack sites
    @Column private long mMaxUploadSize; // only set for Jetpack sites
    @Column private long mMemoryLimit; // only set for Jetpack sites
    @Column private int mOrigin = ORIGIN_UNKNOWN; // Does this site come from a WPCOM REST or XMLRPC fetch_sites call?
    @Column private int mOrganizationId = -1;

    @Column private String mShowOnFront;
    @Column private long mPageOnFront = -1;
    @Column private long mPageForPosts = -1;

    // Self hosted specifics
    // The siteId for self hosted sites. Jetpack sites will also have a mSiteId, which is their id on wpcom
    @Column private long mSelfHostedSiteId;
    @Column private String mUsername;
    @Column private String mPassword;
    @Column(name = "XMLRPC_URL") private String mXmlRpcUrl;
    @Column private String mWpApiRestUrl;
    @Column private String mSoftwareVersion;
    @Column private boolean mIsSelfHostedAdmin;

    // Self hosted user's profile data
    @Column private String mEmail;
    @Column private String mDisplayName;

    // mIsJetpackInstalled is true if Jetpack is installed and activated on the self hosted site, but Jetpack can
    // be disconnected.
    @Column private boolean mIsJetpackInstalled;
    // mIsJetpackConnected is true if Jetpack is installed, activated and connected to a WordPress.com account.
    @Column private boolean mIsJetpackConnected;
    // mIsJetpackCPConnected is true for self hosted sites that use Jetpack Connection Package,
    // but don't have full jetpack plugin
    @Column(name = "IS_JETPACK_CP_CONNECTED") private boolean mIsJetpackCPConnected;
    @Column private String mJetpackVersion;
    @Column private String mJetpackUserEmail;
    @Column private boolean mIsAutomatedTransfer;
    @Column private boolean mIsWpComStore;
    @Column private boolean mHasWooCommerce;

    // WPCom specifics
    @Column private boolean mIsVisible = true;
    @Column private boolean mIsPrivate;
    @Column private boolean mIsComingSoon;
    @Column private boolean mIsVideoPressSupported;
    @Column private long mPlanId;
    @Column private String mPlanShortName;
    @Column private String mIconUrl;
    @Column private boolean mHasFreePlan;
    @Column private String mUnmappedUrl;
    @Column private String mWebEditor;
    @Column private String mMobileEditor;

    // WPCom capabilities
    @Column private boolean mHasCapabilityEditPages;
    @Column private boolean mHasCapabilityEditPosts;
    @Column private boolean mHasCapabilityEditOthersPosts;
    @Column private boolean mHasCapabilityEditOthersPages;
    @Column private boolean mHasCapabilityDeletePosts;
    @Column private boolean mHasCapabilityDeleteOthersPosts;
    @Column private boolean mHasCapabilityEditThemeOptions;
    @Column private boolean mHasCapabilityEditUsers;
    @Column private boolean mHasCapabilityListUsers;
    @Column private boolean mHasCapabilityManageCategories;
    @Column private boolean mHasCapabilityManageOptions;
    @Column private boolean mHasCapabilityActivateWordads;
    @Column private boolean mHasCapabilityPromoteUsers;
    @Column private boolean mHasCapabilityPublishPosts;
    @Column private boolean mHasCapabilityUploadFiles;
    @Column private boolean mHasCapabilityDeleteUser;
    @Column private boolean mHasCapabilityRemoveUsers;
    @Column private boolean mHasCapabilityViewStats;

    // WPCOM and Jetpack Disk Quota information
    @Column private long mSpaceAvailable;
    @Column private long mSpaceAllowed;
    @Column private long mSpaceUsed;
    @Column private double mSpacePercentUsed;

    @Column private String mActiveModules;
    @Column private boolean mIsPublicizePermanentlyDisabled;
    @Column private String mActiveJetpackConnectionPlugins;

    // Zendesk meta
    @Column private String mZendeskPlan;
    @Column private String mZendeskAddOns;

    // Blogging Reminder options
    @Column private boolean mIsBloggingPromptsOptedIn;
    @Column private boolean mIsBloggingPromptsCardOptedIn;
    @Column private boolean mIsPotentialBloggingSite;
    @Column private boolean mIsBloggingReminderOnMonday;
    @Column private boolean mIsBloggingReminderOnTuesday;
    @Column private boolean mIsBloggingReminderOnWednesday;
    @Column private boolean mIsBloggingReminderOnThursday;
    @Column private boolean mIsBloggingReminderOnFriday;
    @Column private boolean mIsBloggingReminderOnSaturday;
    @Column private boolean mIsBloggingReminderOnSunday;
    @Column private int mBloggingReminderHour;
    @Column private int mBloggingReminderMinute;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    public LocalId localId() {
        return new LocalOrRemoteId.LocalId(mId);
    }

    public RemoteId remoteId() {
        if (mSiteId != 0L) {
            return new RemoteId(mSiteId);
        } else {
            return new RemoteId(mSelfHostedSiteId);
        }
    }

    public SiteModel() {
    }

    public long getSiteId() {
        return mSiteId;
    }

    public void setSiteId(long siteId) {
        mSiteId = siteId;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(@NonNull String url) {
        try {
            // Normalize the URL, because it can be used as an identifier.
            mUrl = (new URI(url)).normalize().toString();
        } catch (URISyntaxException e) {
            // Don't set the URL
            AppLog.e(T.API, "Trying to set an invalid url: " + url);
        }
    }

    public String getLoginUrl() {
        return mLoginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        mLoginUrl = loginUrl;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public boolean isWPCom() {
        return mIsWPCom;
    }

    public void setIsWPCom(boolean wpCom) {
        mIsWPCom = wpCom;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String getXmlRpcUrl() {
        return mXmlRpcUrl;
    }

    public void setXmlRpcUrl(String xmlRpcUrl) {
        mXmlRpcUrl = xmlRpcUrl;
    }

    public String getWpApiRestUrl() {
        return mWpApiRestUrl;
    }

    public void setWpApiRestUrl(String wpApiRestEndpoint) {
        mWpApiRestUrl = wpApiRestEndpoint;
    }

    public long getSelfHostedSiteId() {
        return mSelfHostedSiteId;
    }

    public void setSelfHostedSiteId(long selfHostedSiteId) {
        mSelfHostedSiteId = selfHostedSiteId;
    }

    public boolean isSelfHostedAdmin() {
        return mIsSelfHostedAdmin;
    }

    public void setIsSelfHostedAdmin(boolean selfHostedAdmin) {
        mIsSelfHostedAdmin = selfHostedAdmin;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public void setIsVisible(boolean visible) {
        mIsVisible = visible;
    }

    public boolean isPrivate() {
        return mIsPrivate;
    }

    public void setIsPrivate(boolean isPrivate) {
        mIsPrivate = isPrivate;
    }

    public boolean isFeaturedImageSupported() {
        return mIsFeaturedImageSupported;
    }

    public void setIsFeaturedImageSupported(boolean featuredImageSupported) {
        mIsFeaturedImageSupported = featuredImageSupported;
    }

    public String getDefaultCommentStatus() {
        return mDefaultCommentStatus;
    }

    public void setDefaultCommentStatus(String defaultCommentStatus) {
        mDefaultCommentStatus = defaultCommentStatus;
    }

    public String getSoftwareVersion() {
        return mSoftwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        mSoftwareVersion = softwareVersion;
    }

    public String getAdminUrl() {
        return mAdminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        mAdminUrl = adminUrl;
    }

    public boolean isVideoPressSupported() {
        return mIsVideoPressSupported;
    }

    public void setIsVideoPressSupported(boolean videoPressSupported) {
        mIsVideoPressSupported = videoPressSupported;
    }

    public boolean getHasCapabilityEditPages() {
        return mHasCapabilityEditPages;
    }

    public void setHasCapabilityEditPages(boolean hasCapabilityEditPages) {
        mHasCapabilityEditPages = hasCapabilityEditPages;
    }

    public boolean getHasCapabilityEditPosts() {
        return mHasCapabilityEditPosts;
    }

    public void setHasCapabilityEditPosts(boolean capabilityEditPosts) {
        mHasCapabilityEditPosts = capabilityEditPosts;
    }

    public boolean getHasCapabilityEditOthersPosts() {
        return mHasCapabilityEditOthersPosts;
    }

    public void setHasCapabilityEditOthersPosts(boolean capabilityEditOthersPosts) {
        mHasCapabilityEditOthersPosts = capabilityEditOthersPosts;
    }

    public boolean getHasCapabilityEditOthersPages() {
        return mHasCapabilityEditOthersPages;
    }

    public void setHasCapabilityEditOthersPages(boolean capabilityEditOthersPages) {
        mHasCapabilityEditOthersPages = capabilityEditOthersPages;
    }

    public boolean getHasCapabilityDeletePosts() {
        return mHasCapabilityDeletePosts;
    }

    public void setHasCapabilityDeletePosts(boolean capabilityDeletePosts) {
        mHasCapabilityDeletePosts = capabilityDeletePosts;
    }

    public boolean getHasCapabilityDeleteOthersPosts() {
        return mHasCapabilityDeleteOthersPosts;
    }

    public void setHasCapabilityDeleteOthersPosts(boolean capabilityDeleteOthersPosts) {
        mHasCapabilityDeleteOthersPosts = capabilityDeleteOthersPosts;
    }

    public boolean getHasCapabilityEditThemeOptions() {
        return mHasCapabilityEditThemeOptions;
    }

    public void setHasCapabilityEditThemeOptions(boolean capabilityEditThemeOptions) {
        mHasCapabilityEditThemeOptions = capabilityEditThemeOptions;
    }

    public boolean getHasCapabilityEditUsers() {
        return mHasCapabilityEditUsers;
    }

    public void setHasCapabilityEditUsers(boolean capabilityEditUsers) {
        mHasCapabilityEditUsers = capabilityEditUsers;
    }

    public boolean getHasCapabilityListUsers() {
        return mHasCapabilityListUsers;
    }

    public void setHasCapabilityListUsers(boolean capabilityListUsers) {
        mHasCapabilityListUsers = capabilityListUsers;
    }

    public boolean getHasCapabilityManageCategories() {
        return mHasCapabilityManageCategories;
    }

    public void setHasCapabilityManageCategories(boolean capabilityManageCategories) {
        mHasCapabilityManageCategories = capabilityManageCategories;
    }

    public boolean getHasCapabilityManageOptions() {
        return mHasCapabilityManageOptions;
    }

    public void setHasCapabilityManageOptions(boolean capabilityManageOptions) {
        mHasCapabilityManageOptions = capabilityManageOptions;
    }

    public boolean getHasCapabilityActivateWordads() {
        return mHasCapabilityActivateWordads;
    }

    public void setHasCapabilityActivateWordads(boolean capabilityActivateWordads) {
        mHasCapabilityActivateWordads = capabilityActivateWordads;
    }

    public boolean getHasCapabilityPromoteUsers() {
        return mHasCapabilityPromoteUsers;
    }

    public void setHasCapabilityPromoteUsers(boolean capabilityPromoteUsers) {
        mHasCapabilityPromoteUsers = capabilityPromoteUsers;
    }

    public boolean getHasCapabilityPublishPosts() {
        return mHasCapabilityPublishPosts;
    }

    public void setHasCapabilityPublishPosts(boolean capabilityPublishPosts) {
        mHasCapabilityPublishPosts = capabilityPublishPosts;
    }

    public boolean getHasCapabilityUploadFiles() {
        return mHasCapabilityUploadFiles;
    }

    public void setHasCapabilityUploadFiles(boolean capabilityUploadFiles) {
        mHasCapabilityUploadFiles = capabilityUploadFiles;
    }

    public boolean getHasCapabilityDeleteUser() {
        return mHasCapabilityDeleteUser;
    }

    public void setHasCapabilityDeleteUser(boolean capabilityDeleteUser) {
        mHasCapabilityDeleteUser = capabilityDeleteUser;
    }

    public boolean getHasCapabilityRemoveUsers() {
        return mHasCapabilityRemoveUsers;
    }

    public void setHasCapabilityRemoveUsers(boolean capabilityRemoveUsers) {
        mHasCapabilityRemoveUsers = capabilityRemoveUsers;
    }

    public boolean getHasCapabilityViewStats() {
        return mHasCapabilityViewStats;
    }

    public void setHasCapabilityViewStats(boolean capabilityViewStats) {
        mHasCapabilityViewStats = capabilityViewStats;
    }

    public String getTimezone() {
        return mTimezone;
    }

    public void setTimezone(String timezone) {
        mTimezone = timezone;
    }

    public String getFrameNonce() {
        return mFrameNonce;
    }

    public void setFrameNonce(String frameNonce) {
        mFrameNonce = frameNonce;
    }

    public long getMaxUploadSize() {
        return mMaxUploadSize;
    }

    public void setMaxUploadSize(long maxUploadSize) {
        mMaxUploadSize = maxUploadSize;
    }

    public boolean hasMaxUploadSize() {
        return mMaxUploadSize > 0;
    }

    public long getMemoryLimit() {
        return mMemoryLimit;
    }

    public void setMemoryLimit(long memoryLimit) {
        mMemoryLimit = memoryLimit;
    }

    public boolean hasMemoryLimit() {
        return mMemoryLimit > 0;
    }

    public String getPlanShortName() {
        return mPlanShortName;
    }

    public void setPlanShortName(String planShortName) {
        mPlanShortName = planShortName;
    }

    public long getPlanId() {
        return mPlanId;
    }

    public void setPlanId(long planId) {
        mPlanId = planId;
    }

    public String getIconUrl() {
        return mIconUrl;
    }

    public void setIconUrl(String iconUrl) {
        mIconUrl = iconUrl;
    }

    public boolean getHasFreePlan() {
        return mHasFreePlan;
    }

    public void setHasFreePlan(boolean hasFreePlan) {
        mHasFreePlan = hasFreePlan;
    }

    public String getUnmappedUrl() {
        return mUnmappedUrl;
    }

    public void setUnmappedUrl(String unMappedUrl) {
        mUnmappedUrl = unMappedUrl;
    }

    public String getWebEditor() {
        return mWebEditor;
    }

    public void setWebEditor(String webEditor) {
        mWebEditor = webEditor;
    }

    public String getMobileEditor() {
        return mMobileEditor;
    }

    public void setMobileEditor(String mobileEditor) {
        mMobileEditor = mobileEditor;
    }

    public boolean isJetpackInstalled() {
        return mIsJetpackInstalled;
    }

    public void setIsJetpackInstalled(boolean jetpackInstalled) {
        mIsJetpackInstalled = jetpackInstalled;
    }

    public boolean isJetpackConnected() {
        return mIsJetpackConnected;
    }

    public void setIsJetpackConnected(boolean jetpackConnected) {
        mIsJetpackConnected = jetpackConnected;
    }

    public boolean isJetpackCPConnected() {
        return mIsJetpackCPConnected;
    }

    public void setIsJetpackCPConnected(boolean isJetpackCPConnected) {
        this.mIsJetpackCPConnected = isJetpackCPConnected;
    }

    public String getJetpackVersion() {
        return mJetpackVersion;
    }

    public void setJetpackVersion(String jetpackVersion) {
        mJetpackVersion = jetpackVersion;
    }

    public String getJetpackUserEmail() {
        return mJetpackUserEmail;
    }

    public void setJetpackUserEmail(String jetpackUserEmail) {
        mJetpackUserEmail = jetpackUserEmail;
    }

    public boolean isAutomatedTransfer() {
        return mIsAutomatedTransfer;
    }

    public void setIsAutomatedTransfer(boolean automatedTransfer) {
        mIsAutomatedTransfer = automatedTransfer;
    }

    public boolean isWpComStore() {
        return mIsWpComStore;
    }

    public void setIsWpComStore(boolean isWpComStore) {
        mIsWpComStore = isWpComStore;
    }

    public boolean getHasWooCommerce() {
        return mHasWooCommerce;
    }

    public void setHasWooCommerce(boolean hasWooCommerce) {
        mHasWooCommerce = hasWooCommerce;
    }

    @SiteOrigin
    public int getOrigin() {
        return mOrigin;
    }

    public void setOrigin(@SiteOrigin int origin) {
        mOrigin = origin;
    }

    public boolean isUsingWpComRestApi() {
        return isWPCom() || (isJetpackConnected() && getOrigin() == ORIGIN_WPCOM_REST);
    }

    public void setSpaceAvailable(long spaceAvailable) {
        mSpaceAvailable = spaceAvailable;
    }

    public long getSpaceAvailable() {
        return mSpaceAvailable;
    }

    public void setSpaceAllowed(long spaceAllowed) {
        mSpaceAllowed = spaceAllowed;
    }

    public long getSpaceAllowed() {
        return mSpaceAllowed;
    }

    public void setSpaceUsed(long spaceUsed) {
        mSpaceUsed = spaceUsed;
    }

    public long getSpaceUsed() {
        return mSpaceUsed;
    }

    public void setSpacePercentUsed(double spacePercentUsed) {
        mSpacePercentUsed = spacePercentUsed;
    }

    public double getSpacePercentUsed() {
        return mSpacePercentUsed;
    }

    public boolean hasDiskSpaceQuotaInformation() {
        return mSpaceAllowed > 0;
    }

    public boolean isWPComAtomic() {
        return mIsWPComAtomic;
    }

    public void setIsWPComAtomic(boolean isWPComAtomic) {
        mIsWPComAtomic = isWPComAtomic;
    }

    public boolean isWpForTeamsSite() {
        return mIsWpForTeamsSite;
    }

    public void setIsWpForTeamsSite(boolean wpForTeamsSite) {
        mIsWpForTeamsSite = wpForTeamsSite;
    }

    public boolean isComingSoon() {
        return mIsComingSoon;
    }

    public void setIsComingSoon(boolean isComingSoon) {
        mIsComingSoon = isComingSoon;
    }

    public boolean isPrivateWPComAtomic() {
        return isWPComAtomic() && (isPrivate() || isComingSoon());
    }

    public String getShowOnFront() {
        return mShowOnFront;
    }

    public void setShowOnFront(String showOnFront) {
        mShowOnFront = showOnFront;
    }

    public long getPageOnFront() {
        return mPageOnFront;
    }

    public void setPageOnFront(long pageOnFront) {
        mPageOnFront = pageOnFront;
    }

    public long getPageForPosts() {
        return mPageForPosts;
    }

    public void setPageForPosts(long pageForPosts) {
        mPageForPosts = pageForPosts;
    }

    public boolean isPublicizePermanentlyDisabled() {
        return mIsPublicizePermanentlyDisabled;
    }

    public void setIsPublicizePermanentlyDisabled(boolean publicizePermanentlyDisabled) {
        mIsPublicizePermanentlyDisabled = publicizePermanentlyDisabled;
    }

    public String getActiveModules() {
        return mActiveModules;
    }

    public void setActiveModules(String activeModules) {
        mActiveModules = activeModules;
    }

    public String getActiveJetpackConnectionPlugins() {
        return mActiveJetpackConnectionPlugins;
    }

    public void setActiveJetpackConnectionPlugins(String activeJetpackConnectionPlugins) {
        mActiveJetpackConnectionPlugins = activeJetpackConnectionPlugins;
    }

    public boolean isActiveModuleEnabled(String moduleName) {
        if (mActiveModules != null) {
            String[] activeModules = mActiveModules.split(",");
            return Arrays.asList(activeModules).contains(moduleName);
        }
        return false;
    }

    public boolean isAdmin() {
        return mHasCapabilityManageOptions;
    }

    public boolean supportsSharing() {
        return supportsPublicize() || supportsShareButtons();
    }

    public boolean supportsPublicize() {
        // Publicize is only supported via REST
        if (getOrigin() != ORIGIN_WPCOM_REST) return false;

        if (!getHasCapabilityPublishPosts()) return false;

        if (isJetpackConnected()) {
            // For Jetpack, check if the module is enabled
            return isActiveModuleEnabled(ACTIVE_MODULES_KEY_PUBLICIZE);
        } else {
            // For WordPress.com, check if it is not disabled
            return !isPublicizePermanentlyDisabled();
        }
    }

    public boolean supportsShareButtons() {
        // Share Button settings are only supported via REST, and for admins
        if (!isAdmin() || getOrigin() != ORIGIN_WPCOM_REST) {
            return false;
        }

        if (isJetpackConnected()) {
            // For Jetpack, check if the module is enabled
            return isActiveModuleEnabled(ACTIVE_MODULES_KEY_SHARING_BUTTONS);
        } else {
            return true;
        }
    }

    public String getZendeskPlan() {
        return mZendeskPlan;
    }

    public void setZendeskPlan(String zendeskPlan) {
        mZendeskPlan = zendeskPlan;
    }

    public String getZendeskAddOns() {
        return mZendeskAddOns;
    }

    public void setZendeskAddOns(String zendeskAddOns) {
        mZendeskAddOns = zendeskAddOns;
    }

    public int getOrganizationId() {
        return mOrganizationId;
    }

    public void setOrganizationId(int organizationId) {
        mOrganizationId = organizationId;
    }

    public boolean isBloggingPromptsOptedIn() {
        return mIsBloggingPromptsOptedIn;
    }

    public void setIsBloggingPromptsOptedIn(boolean bloggingPromptsOptedIn) {
        mIsBloggingPromptsOptedIn = bloggingPromptsOptedIn;
    }

    public boolean isBloggingPromptsCardOptedIn() {
        return mIsBloggingPromptsCardOptedIn;
    }

    public void setIsBloggingPromptsCardOptedIn(boolean bloggingPromptsCardOptedIn) {
        mIsBloggingPromptsCardOptedIn = bloggingPromptsCardOptedIn;
    }

    public boolean isPotentialBloggingSite() {
        return mIsPotentialBloggingSite;
    }

    public void setIsPotentialBloggingSite(boolean potentialBloggingSite) {
        mIsPotentialBloggingSite = potentialBloggingSite;
    }

    public boolean isBloggingReminderOnMonday() {
        return mIsBloggingReminderOnMonday;
    }

    public void setIsBloggingReminderOnMonday(boolean bloggingReminderOnMonday) {
        mIsBloggingReminderOnMonday = bloggingReminderOnMonday;
    }

    public boolean isBloggingReminderOnTuesday() {
        return mIsBloggingReminderOnTuesday;
    }

    public void setIsBloggingReminderOnTuesday(boolean bloggingReminderOnTuesday) {
        mIsBloggingReminderOnTuesday = bloggingReminderOnTuesday;
    }

    public boolean isBloggingReminderOnWednesday() {
        return mIsBloggingReminderOnWednesday;
    }

    public void setIsBloggingReminderOnWednesday(boolean bloggingReminderOnWednesday) {
        mIsBloggingReminderOnWednesday = bloggingReminderOnWednesday;
    }

    public boolean isBloggingReminderOnThursday() {
        return mIsBloggingReminderOnThursday;
    }

    public void setIsBloggingReminderOnThursday(boolean bloggingReminderOnThursday) {
        mIsBloggingReminderOnThursday = bloggingReminderOnThursday;
    }

    public boolean isBloggingReminderOnFriday() {
        return mIsBloggingReminderOnFriday;
    }

    public void setIsBloggingReminderOnFriday(boolean bloggingReminderOnFriday) {
        mIsBloggingReminderOnFriday = bloggingReminderOnFriday;
    }

    public boolean isBloggingReminderOnSaturday() {
        return mIsBloggingReminderOnSaturday;
    }

    public void setIsBloggingReminderOnSaturday(boolean bloggingReminderOnSaturday) {
        mIsBloggingReminderOnSaturday = bloggingReminderOnSaturday;
    }

    public boolean isBloggingReminderOnSunday() {
        return mIsBloggingReminderOnSunday;
    }

    public void setIsBloggingReminderOnSunday(boolean bloggingReminderOnSunday) {
        mIsBloggingReminderOnSunday = bloggingReminderOnSunday;
    }

    public int getBloggingReminderHour() {
        return mBloggingReminderHour;
    }

    public void setBloggingReminderHour(int bloggingReminderHour) {
        mBloggingReminderHour = bloggingReminderHour;
    }

    public int getBloggingReminderMinute() {
        return mBloggingReminderMinute;
    }

    public void setBloggingReminderMinute(int bloggingReminderMinute) {
        mBloggingReminderMinute = bloggingReminderMinute;
    }
}
