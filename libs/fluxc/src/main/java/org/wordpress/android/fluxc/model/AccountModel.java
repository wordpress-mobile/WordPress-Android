package org.wordpress.android.fluxc.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.StringUtils;

@Table
public class AccountModel extends Payload<BaseNetworkError> implements Identifiable {
    @PrimaryKey(autoincrement = false)
    @Column private int mId;

    // Account attributes
    @Column private String mUserName;
    @Column private long mUserId;
    @Column private String mDisplayName;
    @Column private String mProfileUrl; // profile_URL
    @Column private String mAvatarUrl; // avatar_URL
    @Column private long mPrimarySiteId;
    @Column private boolean mEmailVerified;
    @Column private int mSiteCount;
    @Column private int mVisibleSiteCount;
    @Column private String mEmail;
    @Column private boolean mHasUnseenNotes;

    // Account Settings attributes
    @Column private String mFirstName;
    @Column private String mLastName;
    @Column private String mAboutMe;
    @Column private String mDate;
    @Column private String mNewEmail;
    @Column private boolean mPendingEmailChange;
    @Column private String mWebAddress; // WPCom rest API: user_URL
    @Column private boolean mTracksOptOut;
    @Column private boolean mUsernameCanBeChanged;

    public AccountModel() {
        init();
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
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof AccountModel)) return false;

        AccountModel otherAccount = (AccountModel) other;

        return getId() == otherAccount.getId()
               && StringUtils.equals(getUserName(), otherAccount.getUserName())
               && getUserId() == otherAccount.getUserId()
               && StringUtils.equals(getDisplayName(), otherAccount.getDisplayName())
               && StringUtils.equals(getProfileUrl(), otherAccount.getProfileUrl())
               && StringUtils.equals(getAvatarUrl(), otherAccount.getAvatarUrl())
               && getPrimarySiteId() == otherAccount.getPrimarySiteId()
               && getSiteCount() == otherAccount.getSiteCount()
               && getEmailVerified() == otherAccount.getEmailVerified()
               && getVisibleSiteCount() == otherAccount.getVisibleSiteCount()
               && StringUtils.equals(getFirstName(), otherAccount.getFirstName())
               && StringUtils.equals(getLastName(), otherAccount.getLastName())
               && StringUtils.equals(getAboutMe(), otherAccount.getAboutMe())
               && StringUtils.equals(getDate(), otherAccount.getDate())
               && StringUtils.equals(getNewEmail(), otherAccount.getNewEmail())
               && getPendingEmailChange() == otherAccount.getPendingEmailChange()
               && StringUtils.equals(getWebAddress(), otherAccount.getWebAddress())
               && getHasUnseenNotes() == otherAccount.getHasUnseenNotes()
               && getTracksOptOut() == otherAccount.getTracksOptOut()
               && getUsernameCanBeChanged() == otherAccount.getUsernameCanBeChanged();
    }

    public void init() {
        mUserName = "";
        mUserId = 0;
        mDisplayName = "";
        mProfileUrl = "";
        mAvatarUrl = "";
        mPrimarySiteId = 0;
        mSiteCount = 0;
        mEmailVerified = true;
        mVisibleSiteCount = 0;
        mEmail = "";
        mFirstName = "";
        mLastName = "";
        mAboutMe = "";
        mDate = "";
        mNewEmail = "";
        mPendingEmailChange = false;
        mWebAddress = "";
        mTracksOptOut = false;
        mUsernameCanBeChanged = false;
    }

    /**
     * Copies Account attributes from another {@link AccountModel} to this instance.
     */
    public void copyAccountAttributes(AccountModel other) {
        if (other == null) return;
        setUserName(other.getUserName());
        setUserId(other.getUserId());
        setDisplayName(other.getDisplayName());
        setProfileUrl(other.getProfileUrl());
        setAvatarUrl(other.getAvatarUrl());
        setPrimarySiteId(other.getPrimarySiteId());
        setSiteCount(other.getSiteCount());
        setVisibleSiteCount(other.getVisibleSiteCount());
        setEmail(other.getEmail());
        setHasUnseenNotes(other.getHasUnseenNotes());
        setEmailVerified(other.getEmailVerified());
    }

    /**
     * Copies Account Settings attributes from another {@link AccountModel} to this instance.
     */
    public void copyAccountSettingsAttributes(AccountModel other) {
        if (other == null) return;
        setUserName(other.getUserName());
        setPrimarySiteId(other.getPrimarySiteId());
        setFirstName(other.getFirstName());
        setLastName(other.getLastName());
        setAboutMe(other.getAboutMe());
        setDate(other.getDate());
        setNewEmail(other.getNewEmail());
        setPendingEmailChange(other.getPendingEmailChange());
        setTracksOptOut(other.getTracksOptOut());
        setWebAddress(other.getWebAddress());
        setDisplayName(other.getDisplayName());
        setUsernameCanBeChanged(other.getUsernameCanBeChanged());
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long userId) {
        mUserId = userId;
    }

    public void setPrimarySiteId(long primarySiteId) {
        mPrimarySiteId = primarySiteId;
    }

    public long getPrimarySiteId() {
        return mPrimarySiteId;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getProfileUrl() {
        return mProfileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        mProfileUrl = profileUrl;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        mAvatarUrl = avatarUrl;
    }

    public boolean getEmailVerified() {
        return mEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        mEmailVerified = emailVerified;
    }

    public int getSiteCount() {
        return mSiteCount;
    }

    public void setSiteCount(int siteCount) {
        mSiteCount = siteCount;
    }

    public int getVisibleSiteCount() {
        return mVisibleSiteCount;
    }

    public void setVisibleSiteCount(int visibleSiteCount) {
        mVisibleSiteCount = visibleSiteCount;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setFirstName(String firstName) {
        mFirstName = firstName;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public void setLastName(String lastName) {
        mLastName = lastName;
    }

    public String getLastName() {
        return mLastName;
    }

    public void setAboutMe(String aboutMe) {
        mAboutMe = aboutMe;
    }

    public String getAboutMe() {
        return mAboutMe;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public String getDate() {
        return mDate;
    }

    public void setNewEmail(String newEmail) {
        mNewEmail = newEmail;
    }

    public String getNewEmail() {
        return mNewEmail;
    }

    public void setPendingEmailChange(boolean pendingEmailChange) {
        mPendingEmailChange = pendingEmailChange;
    }

    public boolean getPendingEmailChange() {
        return mPendingEmailChange;
    }

    public void setWebAddress(String webAddress) {
        mWebAddress = webAddress;
    }

    public String getWebAddress() {
        return mWebAddress;
    }

    public boolean getHasUnseenNotes() {
        return mHasUnseenNotes;
    }

    public void setHasUnseenNotes(boolean hasUnseenNotes) {
        mHasUnseenNotes = hasUnseenNotes;
    }

    public boolean getTracksOptOut() {
        return mTracksOptOut;
    }

    public void setTracksOptOut(boolean tracksOptOut) {
        mTracksOptOut = tracksOptOut;
    }

    public boolean getUsernameCanBeChanged() {
        return mUsernameCanBeChanged;
    }

    public void setUsernameCanBeChanged(boolean usernameCanBeChanged) {
        mUsernameCanBeChanged = usernameCanBeChanged;
    }
}
