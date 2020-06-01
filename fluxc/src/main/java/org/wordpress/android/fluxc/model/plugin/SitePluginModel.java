package org.wordpress.android.fluxc.model.plugin;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import java.io.Serializable;

@Table
@RawConstraints({"UNIQUE (SLUG, LOCAL_SITE_ID)"})
public class SitePluginModel implements Identifiable, Serializable {
    private static final long serialVersionUID = -7687371389928982877L;

    @PrimaryKey @Column private int mId;
    @Column private int mLocalSiteId;
    @Column private String mName;
    @Column private String mDisplayName;
    @Column private String mPluginUrl;
    @Column private String mVersion;
    @Column private String mSlug;
    @Column private String mDescription;
    @Column private String mAuthorName;
    @Column private String mAuthorUrl;
    @Column private String mSettingsUrl;
    @Column private boolean mIsActive;
    @Column private boolean mIsAutoUpdateEnabled;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    public int getLocalSiteId() {
        return mLocalSiteId;
    }

    public void setLocalSiteId(int localSiteId) {
        mLocalSiteId = localSiteId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getPluginUrl() {
        return mPluginUrl;
    }

    public void setPluginUrl(String pluginUrl) {
        mPluginUrl = pluginUrl;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getSlug() {
        return mSlug;
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
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

    public String getSettingsUrl() {
        return mSettingsUrl;
    }

    public void setSettingsUrl(String settingsUrl) {
        mSettingsUrl = settingsUrl;
    }

    public boolean isActive() {
        return mIsActive;
    }

    public void setIsActive(boolean isActive) {
        mIsActive = isActive;
    }

    public boolean isAutoUpdateEnabled() {
        return mIsAutoUpdateEnabled;
    }

    public void setIsAutoUpdateEnabled(boolean isAutoUpdateEnabled) {
        mIsAutoUpdateEnabled = isAutoUpdateEnabled;
    }
}
