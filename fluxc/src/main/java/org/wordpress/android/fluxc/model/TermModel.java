package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

@Table
@SuppressWarnings("NotNullFieldNotInitialized")
public class TermModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = -1484257248446576276L;

    @PrimaryKey
    @Column private int mId;
    @Column private int mLocalSiteId;
    @Column private long mRemoteTermId;
    @NonNull @Column private String mTaxonomy;
    @NonNull @Column private String mName;
    @Nullable @Column private String mSlug;
    @Nullable @Column private String mDescription;
    @Column private long mParentRemoteId;
    @Column private int mPostCount;

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

    public long getRemoteTermId() {
        return mRemoteTermId;
    }

    public void setRemoteTermId(long remoteTermId) {
        mRemoteTermId = remoteTermId;
    }

    @NonNull
    public String getTaxonomy() {
        return mTaxonomy;
    }

    public void setTaxonomy(@NonNull String taxonomy) {
        mTaxonomy = taxonomy;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String name) {
        mName = name;
    }

    @Nullable
    public String getSlug() {
        return mSlug;
    }

    public void setSlug(@Nullable String slug) {
        mSlug = slug;
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(@Nullable String description) {
        mDescription = description;
    }

    public long getParentRemoteId() {
        return mParentRemoteId;
    }

    public void setParentRemoteId(long parentRemoteId) {
        mParentRemoteId = parentRemoteId;
    }

    public int getPostCount() {
        return mPostCount;
    }

    public void setPostCount(int count) {
        mPostCount = count;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof TermModel)) return false;

        TermModel otherTerm = (TermModel) other;

        return getId() == otherTerm.getId()
                && getLocalSiteId() == otherTerm.getLocalSiteId()
                && getRemoteTermId() == otherTerm.getRemoteTermId()
                && getParentRemoteId() == otherTerm.getParentRemoteId()
                && getPostCount() == otherTerm.getPostCount()
                && StringUtils.equals(getSlug(), otherTerm.getSlug())
                && StringUtils.equals(getName(), otherTerm.getName())
                && StringUtils.equals(getTaxonomy(), otherTerm.getTaxonomy())
                && StringUtils.equals(getDescription(), otherTerm.getDescription());
    }
}
