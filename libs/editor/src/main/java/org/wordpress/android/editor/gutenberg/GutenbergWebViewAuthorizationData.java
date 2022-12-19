package org.wordpress.android.editor.gutenberg;

import android.os.Parcel;
import android.os.Parcelable;

public class GutenbergWebViewAuthorizationData implements Parcelable {
    private String mSiteUrl;

    private boolean mIsWPCom;

    private long mWPComAccountUserId;
    private String mWPComAccountUsername;
    private String mWPComAccountPassword = "";
    private String mWPComAccountToken;

    private long mSelfHostedSiteId;
    private String mSiteUsername;
    private String mSitePassword;
    private String mSiteWebEditor;

    private boolean mIsSiteUsingWPComRestAPI;

    private String mWordPressUserAgent;

    private boolean mIsJetpackSsoEnabled;

    public GutenbergWebViewAuthorizationData(String siteUrl, boolean isWPCom, long wpComAccountUserId,
                                             String wpComAccountUsername, String wpComAccountToken,
                                             long selfHostedSiteId, String siteUsername, String sitePassword,
                                             boolean isSiteUsingWPComRestAPI, String siteWebEditor,
                                             String wordPressUserAgent, boolean isJetpackSsoEnabled) {
        mSiteUrl = siteUrl;
        mIsWPCom = isWPCom;
        mWPComAccountUserId = wpComAccountUserId;
        mWPComAccountUsername = wpComAccountUsername;
        mWPComAccountToken = wpComAccountToken;
        mSelfHostedSiteId = selfHostedSiteId;
        mSiteUsername = siteUsername;
        mSitePassword = sitePassword;
        mSiteWebEditor = siteWebEditor;
        mIsSiteUsingWPComRestAPI = isSiteUsingWPComRestAPI;
        mWordPressUserAgent = wordPressUserAgent;
        mIsJetpackSsoEnabled = isJetpackSsoEnabled;
    }

    public String getSiteUrl() {
        return mSiteUrl;
    }

    public boolean isWPCom() {
        return mIsWPCom;
    }

    public long getWPComAccountUserId() {
        return mWPComAccountUserId;
    }

    public String getWPComAccountUsername() {
        return mWPComAccountUsername;
    }

    public String getWPComAccountPassword() {
        return mWPComAccountPassword;
    }

    public String getWPComAccountToken() {
        return mWPComAccountToken;
    }

    public long getSelfHostedSiteId() {
        return mSelfHostedSiteId;
    }

    public String getSiteUsername() {
        return mSiteUsername;
    }

    public String getSitePassword() {
        return mSitePassword;
    }

    public boolean isSiteGutenbergWebEditor() {
        return "gutenberg".equals(mSiteWebEditor);
    }

    public boolean isSiteUsingWPComRestAPI() {
        return mIsSiteUsingWPComRestAPI;
    }

    public String getWordPressUserAgent() {
        return mWordPressUserAgent;
    }

    public boolean isJetpackSsoEnabled() {
        return mIsJetpackSsoEnabled;
    }

    public void setJetpackSsoEnabled(boolean jetpackSsoEnabled) {
        mIsJetpackSsoEnabled = jetpackSsoEnabled;
    }

    public static Creator<GutenbergWebViewAuthorizationData> getCREATOR() {
        return CREATOR;
    }

    protected GutenbergWebViewAuthorizationData(Parcel in) {
        mSiteUrl = in.readString();
        mIsWPCom = in.readByte() != 0;
        mWPComAccountUserId = in.readLong();
        mWPComAccountUsername = in.readString();
        mWPComAccountPassword = in.readString();
        mWPComAccountToken = in.readString();
        mSelfHostedSiteId = in.readLong();
        mSiteUsername = in.readString();
        mSitePassword = in.readString();
        mSiteWebEditor = in.readString();
        mIsSiteUsingWPComRestAPI = in.readByte() != 0;
        mWordPressUserAgent = in.readString();
        mIsJetpackSsoEnabled = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSiteUrl);
        dest.writeByte((byte) (mIsWPCom ? 1 : 0));
        dest.writeLong(mWPComAccountUserId);
        dest.writeString(mWPComAccountUsername);
        dest.writeString(mWPComAccountPassword);
        dest.writeString(mWPComAccountToken);
        dest.writeLong(mSelfHostedSiteId);
        dest.writeString(mSiteUsername);
        dest.writeString(mSitePassword);
        dest.writeString(mSiteWebEditor);
        dest.writeByte((byte) (mIsSiteUsingWPComRestAPI ? 1 : 0));
        dest.writeString(mWordPressUserAgent);
        dest.writeByte((byte) (mIsJetpackSsoEnabled ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GutenbergWebViewAuthorizationData> CREATOR =
            new Creator<GutenbergWebViewAuthorizationData>() {
                @Override
                public GutenbergWebViewAuthorizationData createFromParcel(Parcel in) {
                    return new GutenbergWebViewAuthorizationData(in);
                }

                @Override
                public GutenbergWebViewAuthorizationData[] newArray(int size) {
                    return new GutenbergWebViewAuthorizationData[size];
                }
            };
}
