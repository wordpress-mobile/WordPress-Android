package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    @NonNull @Column private String mThemeId;
    @NonNull @Column private String mName;
    @NonNull @Column private String mDescription;
    @Nullable @Column private String mSlug;
    @Nullable @Column private String mVersion;
    @Nullable @Column private String mAuthorName;
    @Nullable @Column private String mAuthorUrl;
    @Nullable @Column private String mThemeUrl;
    @Nullable @Column private String mThemeType;
    @NonNull @Column private String mScreenshotUrl;
    @Nullable @Column private String mDemoUrl;
    @Nullable @Column private String mDownloadUrl;
    @Nullable @Column private String mStylesheet;
    @Nullable @Column private String mPriceText;
    @Column private boolean mFree;
    @Nullable @Column private String mMobileFriendlyCategorySlug;
    @Column private boolean mActive;
    @Column private boolean mAutoUpdate;
    @Column private boolean mAutoUpdateTranslation;

    // local use only
    @Column private boolean mIsExternalTheme;
    @Column private boolean mIsWpComTheme;

    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public ThemeModel() {
        this.mId = 0;
        this.mLocalSiteId = 0;
        this.mThemeId = "";
        this.mName = "";
        this.mDescription = "";
        this.mSlug = null;
        this.mVersion = null;
        this.mAuthorName = null;
        this.mAuthorUrl = null;
        this.mThemeUrl = null;
        this.mThemeType = null;
        this.mScreenshotUrl = "";
        this.mDemoUrl = null;
        this.mDownloadUrl = null;
        this.mStylesheet = null;
        this.mPriceText = null;
        this.mFree = true;
        this.mMobileFriendlyCategorySlug = null;
        this.mActive = false;
        this.mAutoUpdate = false;
        this.mAutoUpdateTranslation = false;
        this.mIsExternalTheme = false;
        this.mIsWpComTheme = false;
    }

    /**
     * Use when creating a WP.com theme.
     */
    public ThemeModel(
            @NonNull String themeId,
            @NonNull String name,
            @NonNull String description,
            @Nullable String slug,
            @Nullable String version,
            @Nullable String authorName,
            @Nullable String authorUrl,
            @Nullable String themeUrl,
            @Nullable String themeType,
            @NonNull String screenshotUrl,
            @Nullable String demoUrl,
            @Nullable String downloadUrl,
            @Nullable String stylesheet,
            @Nullable String priceText,
            boolean isExternalTheme,
            boolean free,
            @Nullable String mobileFriendlyCategorySlug) {
        this.mThemeId = themeId;
        this.mName = name;
        this.mDescription = description;
        this.mSlug = slug;
        this.mVersion = version;
        this.mAuthorName = authorName;
        this.mAuthorUrl = authorUrl;
        this.mThemeUrl = themeUrl;
        this.mThemeType = themeType;
        this.mScreenshotUrl = screenshotUrl;
        this.mDemoUrl = demoUrl;
        this.mDownloadUrl = downloadUrl;
        this.mStylesheet = stylesheet;
        this.mPriceText = priceText;
        this.mIsExternalTheme = isExternalTheme;
        this.mFree = free;
        this.mMobileFriendlyCategorySlug = mobileFriendlyCategorySlug;
    }

    /**
     * Use when creating a Jetpack theme.
     */
    public ThemeModel(
            @NonNull String themeId,
            @NonNull String name,
            @NonNull String description,
            @Nullable String version,
            @Nullable String authorName,
            @Nullable String authorUrl,
            @Nullable String themeUrl,
            @NonNull String screenshotUrl,
            boolean active,
            boolean autoUpdate,
            boolean autoUpdateTranslation) {
        this.mThemeId = themeId;
        this.mName = name;
        this.mDescription = description;
        this.mVersion = version;
        this.mAuthorName = authorName;
        this.mAuthorUrl = authorUrl;
        this.mThemeUrl = themeUrl;
        this.mScreenshotUrl = screenshotUrl;
        this.mActive = active;
        this.mAutoUpdate = autoUpdate;
        this.mAutoUpdateTranslation = autoUpdateTranslation;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    public boolean equals(@Nullable Object other) {
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

    @NonNull
    public String getThemeId() {
        return mThemeId;
    }

    public void setThemeId(@NonNull String themeId) {
        mThemeId = themeId;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String name) {
        mName = name;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(@NonNull String description) {
        mDescription = description;
    }

    @Nullable
    public String getSlug() {
        return mSlug;
    }

    public void setSlug(@Nullable String slug) {
        mSlug = slug;
    }

    @Nullable
    public String getVersion() {
        return mVersion;
    }

    public void setVersion(@Nullable String version) {
        mVersion = version;
    }

    @Nullable
    public String getAuthorName() {
        return mAuthorName;
    }

    public void setAuthorName(@Nullable String authorName) {
        mAuthorName = authorName;
    }

    @Nullable
    public String getAuthorUrl() {
        return mAuthorUrl;
    }

    public void setAuthorUrl(@Nullable String authorUrl) {
        mAuthorUrl = authorUrl;
    }

    @Nullable
    public String getThemeUrl() {
        return mThemeUrl;
    }

    public void setThemeUrl(@Nullable String themeUrl) {
        mThemeUrl = themeUrl;
    }

    @NonNull
    public String getScreenshotUrl() {
        return mScreenshotUrl;
    }

    public void setScreenshotUrl(@NonNull String screenshotUrl) {
        mScreenshotUrl = screenshotUrl;
    }

    @Nullable
    public String getThemeType() {
        return mThemeType;
    }

    public void setThemeType(@Nullable String themeType) {
        mThemeType = themeType;
    }

    @Nullable
    public String getDemoUrl() {
        return mDemoUrl;
    }

    public void setDemoUrl(@Nullable String demoUrl) {
        mDemoUrl = demoUrl;
    }

    @Nullable
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(@Nullable String downloadUrl) {
        mDownloadUrl = downloadUrl;
    }

    @Nullable
    public String getStylesheet() {
        return mStylesheet;
    }

    public void setStylesheet(@Nullable String stylesheet) {
        mStylesheet = stylesheet;
    }

    @Nullable
    public String getPriceText() {
        return mPriceText;
    }

    public void setPriceText(@Nullable String priceText) {
        mPriceText = priceText;
    }

    public boolean getFree() {
        return mFree;
    }

    @Nullable
    public String getMobileFriendlyCategorySlug() {
        return mMobileFriendlyCategorySlug;
    }

    public void setMobileFriendlyCategorySlug(@Nullable String mobileFriendlyCategorySlug) {
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

    public boolean isExternalTheme() {
        return mIsExternalTheme;
    }

    public void setIsExternalTheme(boolean isExternalTheme) {
        mIsExternalTheme = isExternalTheme;
    }

    public boolean isWpComTheme() {
        return mIsWpComTheme;
    }

    public void setIsWpComTheme(boolean isWpComTheme) {
        mIsWpComTheme = isWpComTheme;
    }
}
