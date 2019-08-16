package org.wordpress.android.fluxc.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

@Table
public class TaxonomyModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = 8855881690971305398L;

    @PrimaryKey
    @Column private int mId;
    @Column private int mLocalSiteId;
    @Column private String mName;
    @Column private String mLabel;
    @Column private String mDescription;
    @Column private boolean mIsHierarchical;
    @Column private boolean mIsPublic;

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

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public boolean isHierarchical() {
        return mIsHierarchical;
    }

    public void setIsHierarchical(boolean isHierarchical) {
        mIsHierarchical = isHierarchical;
    }

    public boolean isPublic() {
        return mIsPublic;
    }

    public void setIsPublic(boolean isPublic) {
        mIsPublic = isPublic;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof TaxonomyModel)) return false;

        TaxonomyModel otherTaxonomy = (TaxonomyModel) other;

        return getId() == otherTaxonomy.getId()
                && getLocalSiteId() == otherTaxonomy.getLocalSiteId()
                && isHierarchical() == otherTaxonomy.isHierarchical()
                && isPublic() == otherTaxonomy.isPublic()
                && StringUtils.equals(getName(), otherTaxonomy.getName())
                && StringUtils.equals(getLabel(), otherTaxonomy.getLabel())
                && StringUtils.equals(getDescription(), otherTaxonomy.getDescription());
    }
}
