package org.wordpress.android.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.util.StringUtils;

public class PublicizeService {
    public enum Status {
        OK("ok"),
        UNSUPPORTED("unsupported");

        @NonNull private final String mValue;

        Status(@NonNull final String value) {
            this.mValue = value;
        }

        @NonNull
        public static Status fromString(@Nullable final String value) {
            for (Status status : Status.values()) {
                if (status.mValue.equals(value)) {
                    return status;
                }
            }
            // default to OK
            return OK;
        }

        @NonNull
        public String getValue() {
            return mValue;
        }
    }

    public static final String FACEBOOK_SERVICE_ID = "facebook";
    public static final String TWITTER_SERVICE_ID = "twitter";

    private String mId;
    private String mLabel;
    private String mDescription;
    private String mGenericon;
    private String mIconUrl;
    private String mConnectUrl;
    private Status mStatus;
    private boolean mIsExternalUsersOnly;

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

    public void setGenericon(String genericon) {
        mGenericon = StringUtils.notNullStr(genericon);
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

    public boolean isExternalUsersOnly() {
        return mIsExternalUsersOnly;
    }

    public void setIsExternalUsersOnly(boolean isExternalUsersOnly) {
        mIsExternalUsersOnly = isExternalUsersOnly;
    }

    public Status getStatus() {
        return mStatus;
    }

    public void setStatus(final Status status) {
        this.mStatus = status;
    }

    public boolean isSameAs(PublicizeService other) {
        return other != null
               && other.getId().equals(this.getId())
               && other.getLabel().equals(this.getLabel())
               && other.getDescription().equals(this.getDescription())
               && other.getGenericon().equals(this.getGenericon())
               && other.getIconUrl().equals(this.getIconUrl())
               && other.getConnectUrl().equals(this.getConnectUrl())
               && other.isExternalUsersOnly() == this.isExternalUsersOnly()
               && other.isJetpackSupported() == this.isJetpackSupported();
    }
}
