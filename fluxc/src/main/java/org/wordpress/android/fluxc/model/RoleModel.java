package org.wordpress.android.fluxc.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

@Table
public class RoleModel implements Identifiable, Serializable {
    private static final long serialVersionUID = 5154356410357986144L;

    @PrimaryKey @Column private int mId;

    // Site Id Foreign Key
    @Column private int mSiteId;

    // Role attributes
    @Column private String mName;
    @Column private String mDisplayName;

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public int getSiteId() {
        return mSiteId;
    }

    public void setSiteId(int siteId) {
        mSiteId = siteId;
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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof RoleModel)) return false;

        RoleModel otherRole = (RoleModel) other;
        return getId() == otherRole.getId()
                && getSiteId() == otherRole.getSiteId()
                && StringUtils.equals(getName(), otherRole.getName())
                && StringUtils.equals(getDisplayName(), otherRole.getDisplayName());
    }
}
