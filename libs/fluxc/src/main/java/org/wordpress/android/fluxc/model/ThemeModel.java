package org.wordpress.android.fluxc.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

@Table
public class ThemeModel implements Identifiable, Serializable {
    @PrimaryKey @Column private int mId;

    @Column private long mLocalSiteId;
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
    @Column private float mPrice;
    @Column private boolean mActive;
    @Column private boolean mAutoUpdate;
    @Column private boolean mAutoUpdateTranslation;

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
            return  false;
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
                && getPrice() == otherTheme.getPrice()
                && getActive() == otherTheme.getActive()
                && getAutoUpdate() == otherTheme.getAutoUpdate()
                && getAutoUpdateTranslation() == otherTheme.getAutoUpdateTranslation();
    }

    public long getLocalSiteId() {
        return mLocalSiteId;
    }

    public void setLocalSiteId(long localSiteId) {
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

    public float getPrice() {
        return mPrice;
    }

    public void setPrice(float price) {
        mPrice = price;
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
}
