package org.wordpress.android.stores.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.stores.Payload;

@Table
@RawConstraints({"UNIQUE (SITE_ID, URL)"})
public class SiteModel implements Identifiable, Payload {

    @PrimaryKey
    @Column private int mId;
    @Column private long mSiteId;
    @Column private String mUrl;
    @Column private String mAdminUrl;
    @Column private String mName;
    @Column private String mDescription;
    @Column private boolean mIsWPCom;
    @Column private boolean mIsAdmin;
    @Column private boolean mIsFeaturedImageSupported;

    // Self hosted specifics
    @Column private String mUsername;
    @Column private String mPassword;
    @Column private String mXMLRpcUrl;
    @Column private String mSoftwareVersion;

    // WPCom specifics
    @Column private boolean mIsJetpack;
    @Column private boolean mIsVisible;
    @Column private boolean mIsVideoPressSupported;

    // Jetpack specifics
    // The .com site ID for Jetpack sites (for Jetpack sites, the siteId is local to their network,
    // just as for normal self-hosted sites)
    @Column private long mDotComIdForJetpack;

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

    public String getXMLRpcUrl() {
        return mXMLRpcUrl;
    }

    public void setXMLRpcUrl(String XMLRpcUrl) {
        mXMLRpcUrl = XMLRpcUrl;
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

    public long getDotComIdForJetpack() {
        return mDotComIdForJetpack;
    }

    public void setDotComIdForJetpack(long dotComIdForJetpack) {
        mDotComIdForJetpack = dotComIdForJetpack;
    }
}
