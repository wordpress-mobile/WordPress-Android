package org.wordpress.android.fluxc.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

@Table
public class PostFormatModel implements Identifiable {
    @PrimaryKey
    @Column private int mId;

    // Site Id Foreign Key
    @Column private int mSiteId;

    // Post format attributes
    @Column private String mSlug;
    @Column private String mDisplayName;

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getSlug() {
        return mSlug;
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }

    public int getSiteId() {
        return mSiteId;
    }

    public void setSiteId(int siteId) {
        mSiteId = siteId;
    }
}
