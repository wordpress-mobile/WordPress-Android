package org.wordpress.android.fluxc.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;

import java.io.Serializable;

@Table
@RawConstraints({"UNIQUE (SITE_ID, URL)"})
public class SiteModel implements Identifiable, Payload, Serializable {

    @PrimaryKey
    @Column private int mId;
    // Only given a value for .COM and Jetpack sites - self-hosted sites use mDotOrgSiteId
    @Column private long mSiteId;
    @Column private String mUrl;
    @Column private String mAdminUrl;
    @Column private String mLoginUrl;
    @Column private String mName;
    @Column private String mDescription;
    @Column private boolean mIsWPCom;
    @Column private boolean mIsAdmin;
    @Column private boolean mIsFeaturedImageSupported;
    @Column private String mTimezone;


    // Self hosted specifics
    // The siteId for .org sites. Jetpack sites will also have a mSiteId, which is their id on .COM
    @Column private long mDotOrgSiteId;
    @Column private String mUsername;
    @Column private String mPassword;
    @Column(name = "XMLRPC_URL") private String mXmlRpcUrl;
    @Column private String mSoftwareVersion;

    // WPCom specifics
    @Column private boolean mIsJetpack;
    @Column private boolean mIsVisible;
    @Column private boolean mIsPrivate;
    @Column private boolean mIsVideoPressSupported;
    @Column private long mPlanId;
    @Column private String mPlanShortName;

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

    public void setUrl(String url) {
        mUrl = url;
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

    public void setIsWPCom(boolean WPCom) {
        mIsWPCom = WPCom;
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

    public long getDotOrgSiteId() {
        return mDotOrgSiteId;
    }

    public void setDotOrgSiteId(long dotOrgSiteId) {
        mDotOrgSiteId = dotOrgSiteId;
    }

    public boolean isAdmin() {
        return mIsAdmin;
    }

    public void setIsAdmin(boolean admin) {
        mIsAdmin = admin;
    }

    public boolean isJetpack() {
        return mIsJetpack;
    }

    public void setIsJetpack(boolean jetpack) {
        mIsJetpack = jetpack;
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
}
