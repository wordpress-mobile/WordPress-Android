package org.wordpress.android.fluxc.model;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Table
@RawConstraints({"UNIQUE (SITE_ID, URL)"})
public class SiteModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    @Retention(SOURCE)
    @IntDef({ORIGIN_UNKNOWN, ORIGIN_WPCOM_REST, ORIGIN_XMLRPC})
    public @interface SiteOrigin {}
    public static final int ORIGIN_UNKNOWN = 0;
    public static final int ORIGIN_WPCOM_REST = 1;
    public static final int ORIGIN_XMLRPC = 2;

    public static final long VIP_PLAN_ID = 31337;

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
    @Column private boolean mIsFeaturedImageSupported;
    @Column private String mDefaultCommentStatus = "open";
    @Column private String mTimezone; // Expressed as an offset relative to GMT (e.g. '-8')
    @Column private String mFrameNonce; // only wpcom and Jetpack sites
    @Column private long mMaxUploadSize; // only set for Jetpack sites
    @Column private long mMemoryLimit; // only set for Jetpack sites
    @Column private int mOrigin = ORIGIN_UNKNOWN; // Does this site come from a WPCOM REST or XMLRPC fetch_sites call?

    // Self hosted specifics
    // The siteId for self hosted sites. Jetpack sites will also have a mSiteId, which is their id on wpcom
    @Column private long mSelfHostedSiteId;
    @Column private String mUsername;
    @Column private String mPassword;
    @Column(name = "XMLRPC_URL") private String mXmlRpcUrl;
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
    @Column private String mJetpackVersion;
    @Column private boolean mIsAutomatedTransfer;
    @Column private boolean mIsWpComStore;
    @Column private boolean mHasWooCommerce;

    // WPCom specifics
    @Column private boolean mIsVisible = true;
    @Column private boolean mIsPrivate;
    @Column private boolean mIsVideoPressSupported;
    @Column private long mPlanId;
    @Column private String mPlanShortName;
    @Column private String mIconUrl;
    @Column private boolean mHasFreePlan;
    @Column private String mUnmappedUrl;

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

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
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

    public String getJetpackVersion() {
        return mJetpackVersion;
    }

    public void setJetpackVersion(String jetpackVersion) {
        mJetpackVersion = jetpackVersion;
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
}
