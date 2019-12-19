package org.wordpress.android.fluxc.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

@Table
public class ThemeModel implements Identifiable, Serializable {
    private static final long serialVersionUID = 5966516212440517166L;

    @PrimaryKey @Column private int mId;

    @Column private int mLocalSiteId;
    @Column private String mThemeId;
    @Column private String mName;
    @Column private String mDescription;
    @Column private String mSlug;
    @Column private String mVersion;
    @Column private String mAuthorName;
    @Column private String mAuthorUrl;
    @Column private String mThemeUrl;
    @Column private String mScreenshotUrl;
    @Column private String mDemoUrl;
    @Column private String mDownloadUrl;
    @Column private String mStylesheet;
    @Column private String mPriceText;
    @Column private boolean mFree = true;
    @Column private String mMobileFriendlyCategorySlug;
    @Column private boolean mActive;
    @Column private boolean mAutoUpdate;
    @Column private boolean mAutoUpdateTranslation;

    // local use only
    @Column private boolean mIsWpComTheme;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof ThemeModel)) {
            return false;
        }
        ThemeModel otherTheme = (ThemeModel) other;
        return getId() == otherTheme.getId()
                && getLocalSiteId() == otherTheme.getLocalSiteId()
                && StringUtils.equals(getThemeId(), otherTheme.getThemeId())
                && StringUtils.equals(getName(), otherTheme.getName())
                && StringUtils.equals(getDescription(), otherTheme.getDescription())
                && StringUtils.equals(getVersion(), otherTheme.getVersion())
                && StringUtils.equals(getAuthorName(), otherTheme.getAuthorName())
                && StringUtils.equals(getAuthorUrl(), otherTheme.getAuthorUrl())
                && StringUtils.equals(getThemeUrl(), otherTheme.getThemeUrl())
                && StringUtils.equals(getScreenshotUrl(), otherTheme.getScreenshotUrl())
                && StringUtils.equals(getDemoUrl(), otherTheme.getDemoUrl())
                && StringUtils.equals(getSlug(), otherTheme.getSlug())
                && StringUtils.equals(getDownloadUrl(), otherTheme.getDownloadUrl())
                && StringUtils.equals(getStylesheet(), otherTheme.getStylesheet())
                && StringUtils.equals(getPriceText(), otherTheme.getPriceText())
                && getFree() == otherTheme.getFree()
                && StringUtils.equals(getMobileFriendlyCategorySlug(), otherTheme.getMobileFriendlyCategorySlug())
                && getActive() == otherTheme.getActive()
                && getAutoUpdate() == otherTheme.getAutoUpdate()
                && getAutoUpdateTranslation() == otherTheme.getAutoUpdateTranslation()
                && isWpComTheme() == otherTheme.isWpComTheme();
    }

    public int getLocalSiteId() {
        return mLocalSiteId;
    }

    public void setLocalSiteId(int localSiteId) {
        this.mLocalSiteId = localSiteId;
    }

    public String getThemeId() {
        return mThemeId;
    }

    public void setThemeId(String themeId) {
        mThemeId = themeId;
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

    public String getSlug() {
        return mSlug;
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getAuthorName() {
        return mAuthorName;
    }

    public void setAuthorName(String authorName) {
        mAuthorName = authorName;
    }

    public String getAuthorUrl() {
        return mAuthorUrl;
    }

    public void setAuthorUrl(String authorUrl) {
        mAuthorUrl = authorUrl;
    }

    public String getThemeUrl() {
        return mThemeUrl;
    }

    public void setThemeUrl(String themeUrl) {
        mThemeUrl = themeUrl;
    }

    public String getScreenshotUrl() {
        return mScreenshotUrl;
    }

    public void setScreenshotUrl(String screenshotUrl) {
        mScreenshotUrl = screenshotUrl;
    }

    public String getDemoUrl() {
        return mDemoUrl;
    }

    public void setDemoUrl(String demoUrl) {
        mDemoUrl = demoUrl;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        mDownloadUrl = downloadUrl;
    }

    public String getStylesheet() {
        return mStylesheet;
    }

    public void setStylesheet(String stylesheet) {
        mStylesheet = stylesheet;
    }

    public String getPriceText() {
        return mPriceText;
    }

    public void setPriceText(String priceText) {
        mPriceText = priceText;
    }

    public boolean getFree() {
        return mFree;
    }

    public String getMobileFriendlyCategorySlug() {
        return mMobileFriendlyCategorySlug;
    }

    public void setMobileFriendlyCategorySlug(String mobileFriendlyCategorySlug) {
        mMobileFriendlyCategorySlug = mobileFriendlyCategorySlug;
    }

    public boolean isFree() {
        return getFree();
    }

    public void setFree(boolean free) {
        mFree = free;
    }

    public boolean getActive() {
        return mActive;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    public boolean getAutoUpdate() {
        return mAutoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        mAutoUpdate = autoUpdate;
    }

    public boolean getAutoUpdateTranslation() {
        return mAutoUpdateTranslation;
    }

    public void setAutoUpdateTranslation(boolean autoUpdateTranslation) {
        mAutoUpdateTranslation = autoUpdateTranslation;
    }

    public boolean isWpComTheme() {
        return mIsWpComTheme;
    }

    public void setIsWpComTheme(boolean isWpComTheme) {
        mIsWpComTheme = isWpComTheme;
    }
}
