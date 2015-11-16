package org.wordpress.android.models;

import org.wordpress.android.util.StringUtils;

public class PublicizeService {
    private String mId;
    private String mLabel;
    private String mDescription;
    private String mGenericon;
    private String mIconUrl;
    private String mConnectUrl;

    private boolean mIsJetpackSupported;
    private boolean mIsMultiExternalUserIdSupported;

    public String getId() {
        return StringUtils.notNullStr(mId);
    }
    public void setId(String id) {
        mId = StringUtils.notNullStr(id);
    }

    public String getLabel() {
        return StringUtils.notNullStr(mLabel);
    }
    public void setLabel(String label) {
        mLabel = StringUtils.notNullStr(label);
    }

    public String getDescription() {
        return StringUtils.notNullStr(mDescription);
    }
    public void setDescription(String description) {
        mDescription = StringUtils.notNullStr(description);
    }

    public String getGenericon() {
        return StringUtils.notNullStr(mGenericon);
    }
    public void setGenericon(String Genericon) {
        mGenericon = StringUtils.notNullStr(Genericon);
    }

    public String getIconUrl() {
        return StringUtils.notNullStr(mIconUrl);
    }
    public void setIconUrl(String url) {
        mIconUrl = StringUtils.notNullStr(url);
    }

    public String getConnectUrl() {
        return StringUtils.notNullStr(mConnectUrl);
    }
    public void setConnectUrl(String url) {
        mConnectUrl = StringUtils.notNullStr(url);
    }

    public boolean isJetpackSupported() {
        return mIsJetpackSupported;
    }
    public void setIsJetpackSupported(boolean supported) {
        mIsJetpackSupported = supported;
    }

    public boolean isMultiExternalUserIdSupported() {
        return mIsMultiExternalUserIdSupported;
    }
    public void setIsMultiExternalUserIdSupported(boolean supported) {
        mIsMultiExternalUserIdSupported = supported;
    }

    public boolean isSameAs(PublicizeService other) {
        return other != null
                && other.getId().equals(this.getId())
                && other.getLabel().equals(this.getLabel())
                && other.getDescription().equals(this.getDescription())
                && other.getGenericon().equals(this.getGenericon())
                && other.getIconUrl().equals(this.getIconUrl())
                && other.getConnectUrl().equals(this.getConnectUrl())
                && other.isJetpackSupported() == this.isJetpackSupported();
    }
}
