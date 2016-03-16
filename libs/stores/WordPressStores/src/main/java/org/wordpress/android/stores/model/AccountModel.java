package org.wordpress.android.stores.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.stores.Payload;
import org.wordpress.android.util.StringUtils;

@Table
public class AccountModel implements Identifiable, Payload {
    @PrimaryKey
    @Column private int mId;

    // Account attributes
    @Column private String mUserName;
    @Column private long mUserId;
    @Column private String mDisplayName;
    @Column private String mProfileUrl;
    @Column private String mAvatarUrl;
    @Column private long mPrimaryBlogId;
    @Column private int mSiteCount;
    @Column private int mVisibleSiteCount;
    @Column private String mEmail;

    // Account Settings attributes
    @Column private String mFirstName;
    @Column private String mLastName;
    @Column private String mAboutMe;
    @Column private String mDate;
    @Column private String mNewEmail;
    @Column private boolean mPendingEmailChange;
    @Column private String mWebAddress;

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
        if (other == null || !(other instanceof AccountModel)) return false;
        AccountModel otherAccount = (AccountModel) other;
        return getId() == otherAccount.getId() &&
                StringUtils.equals(getUserName(), otherAccount.getUserName()) &&
                getUserId() == otherAccount.getUserId() &&
                StringUtils.equals(getDisplayName(), otherAccount.getDisplayName()) &&
                StringUtils.equals(getProfileUrl(), otherAccount.getProfileUrl()) &&
                StringUtils.equals(getAvatarUrl(), otherAccount.getAvatarUrl()) &&
                getPrimaryBlogId() == otherAccount.getPrimaryBlogId() &&
                getSiteCount() == otherAccount.getSiteCount() &&
                getVisibleSiteCount() == otherAccount.getVisibleSiteCount() &&
                StringUtils.equals(getFirstName(), otherAccount.getFirstName()) &&
                StringUtils.equals(getLastName(), otherAccount.getLastName()) &&
                StringUtils.equals(getAboutMe(), otherAccount.getAboutMe()) &&
                StringUtils.equals(getDate(), otherAccount.getDate()) &&
                StringUtils.equals(getNewEmail(), otherAccount.getNewEmail()) &&
                getPendingEmailChange() == otherAccount.getPendingEmailChange() &&
                StringUtils.equals(getWebAddress(), otherAccount.getWebAddress());
    }

    private void init() {
        mUserName = "";
        mUserId = 0;
        mDisplayName = "";
        mProfileUrl = "";
        mAvatarUrl = "";
        mPrimaryBlogId = 0;
        mSiteCount = 0;
        mVisibleSiteCount = 0;
        mEmail = "";
        mFirstName = "";
        mLastName = "";
        mAboutMe = "";
        mDate = "";
        mNewEmail = "";
        mPendingEmailChange = false;
        mWebAddress = "";
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
        setPrimaryBlogId(other.getPrimaryBlogId());
        setSiteCount(other.getSiteCount());
        setVisibleSiteCount(other.getVisibleSiteCount());
        setEmail(other.getEmail());
    }

    /**
     * Copies Account Settings attributes from another {@link AccountModel} to this instance.
     */
    public void copyAccountSettingsAttributes(AccountModel other) {
        if (other == null) return;
        setUserName(other.getUserName());
        setPrimaryBlogId(other.getPrimaryBlogId());
        setFirstName(other.getFirstName());
        setLastName(other.getLastName());
        setAboutMe(other.getAboutMe());
        setDate(other.getDate());
        setNewEmail(other.getNewEmail());
        setPendingEmailChange(other.getPendingEmailChange());
        setWebAddress(other.getWebAddress());
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long userId) {
        mUserId = userId;
    }

    public void setPrimaryBlogId(long primaryBlogId) {
        mPrimaryBlogId = primaryBlogId;
    }

    public long getPrimaryBlogId() {
        return mPrimaryBlogId;
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
}
