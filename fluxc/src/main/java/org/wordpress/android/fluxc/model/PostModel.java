package org.wordpress.android.fluxc.model;

import android.text.TextUtils;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

@Table
public class PostModel implements Identifiable, Payload {
    private static long FEATURED_IMAGE_INIT_VALUE = -2;

    @PrimaryKey
    @Column private int mId;
    @Column private int mLocalSiteId;
    @Column private long mRemoteSiteId; // .COM REST API
    @Column private long mRemotePostId;
    @Column private String mTitle;
    @Column private String mContent;
    @Column private String mDateCreated; // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column private String mCategories;
    @Column private String mCustomFields;
    @Column private String mLink;
    @Column private String mExcerpt;
    @Column private String mKeywords;
    @Column private String mStatus;
    @Column private String mPassword;
    @Column private long mFeaturedImageId = FEATURED_IMAGE_INIT_VALUE;
    @Column private String mPostFormat;
    @Column private String mSlug;
    @Column private double mLatitude;
    @Column private double mLongitude;

    // Page specific
    @Column private boolean mIsPage;
    @Column private long mParentId;
    @Column private String mParentTitle;

    @Column private boolean mIsLocalDraft;
    @Column private boolean mIsLocallyChanged;

    // XML-RPC only, needed to work around a bug with the API:
    // https://github.com/wordpress-mobile/WordPress-Android/pull/3425
    // We may be able to drop this if we switch to wp.editPost (and it doesn't have the same bug as metaWeblog.editPost)
    @Column private long mLastKnownRemoteFeaturedImageId = FEATURED_IMAGE_INIT_VALUE;

    // WPCom capabilities
    @Column private boolean mHasCapabilityPublishPost;
    @Column private boolean mHasCapabilityEditPost;
    @Column private boolean mHasCapabilityDeletePost;

    public PostModel() {}

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public int getId() {
        return mId;
    }


    public int getLocalSiteId() {
        return mLocalSiteId;
    }

    public void setLocalSiteId(int localTableSiteId) {
        mLocalSiteId = localTableSiteId;
    }

    public long getRemoteSiteId() {
        return mRemoteSiteId;
    }

    public void setRemoteSiteId(long siteId) {
        mRemoteSiteId = siteId;
    }

    public long getRemotePostId() {
        return mRemotePostId;
    }

    public void setRemotePostId(long postId) {
        mRemotePostId = postId;
    }

    public String getTitle() {
        return StringUtils.notNullStr(mTitle);
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getContent() {
        return StringUtils.notNullStr(mContent);
    }

    public void setContent(String content) {
        mContent = content;
    }

    public String getDateCreated() {
        return mDateCreated;
    }

    public void setDateCreated(String dateCreated) {
        mDateCreated = dateCreated;
    }

    public String getCategories() {
        return StringUtils.notNullStr(mCategories);
    }

    public void setCategories(String categories) {
        mCategories = categories;
    }

    public String getCustomFields() {
        return StringUtils.notNullStr(mCustomFields);
    }

    public void setCustomFields(String customFields) {
        mCustomFields = customFields;
    }

    public String getLink() {
        return StringUtils.notNullStr(mLink);
    }

    public void setLink(String link) {
        mLink = link;
    }

    public String getExcerpt() {
        return StringUtils.notNullStr(mExcerpt);
    }

    public void setExcerpt(String excerpt) {
        mExcerpt = excerpt;
    }

    public String getKeywords() {
        return StringUtils.notNullStr(mKeywords);
    }

    public void setKeywords(String keywords) {
        mKeywords = keywords;
    }

    public String getStatus() {
        return StringUtils.notNullStr(mStatus);
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public String getPassword() {
        return StringUtils.notNullStr(mPassword);
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public long getFeaturedImageId() {
        if (mFeaturedImageId == FEATURED_IMAGE_INIT_VALUE) {
            return 0;
        }

        return mFeaturedImageId;
    }

    public void setFeaturedImageId(long featuredImageId) {
        if (mFeaturedImageId == FEATURED_IMAGE_INIT_VALUE) {
            mLastKnownRemoteFeaturedImageId = mFeaturedImageId;
        }

        mFeaturedImageId = featuredImageId;
    }

    public String getPostFormat() {
        return StringUtils.notNullStr(mPostFormat);
    }

    public void setPostFormat(String postFormat) {
        mPostFormat = postFormat;
    }

    public String getSlug() {
        return StringUtils.notNullStr(mSlug);
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public PostLocation getPostLocation() {
        return new PostLocation(mLatitude, mLongitude);
    }

    public void setPostLocation(PostLocation postLocation) {
        mLatitude = postLocation.getLatitude();
        mLongitude = postLocation.getLongitude();
    }

    public void setPostLocation(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public boolean isPage() {
        return mIsPage;
    }

    public void setIsPage(boolean isPage) {
        mIsPage = isPage;
    }

    public long getParentId() {
        return mParentId;
    }

    public void setParentId(long parentId) {
        mParentId = parentId;
    }

    public String getParentTitle() {
        return StringUtils.notNullStr(mParentTitle);
    }

    public void setParentTitle(String parentTitle) {
        mParentTitle = parentTitle;
    }

    public boolean isLocalDraft() {
        return mIsLocalDraft;
    }

    public void setIsLocalDraft(boolean isLocalDraft) {
        mIsLocalDraft = isLocalDraft;
    }

    public boolean isLocallyChanged() {
        return mIsLocallyChanged;
    }

    public void setIsLocallyChanged(boolean isLocallyChanged) {
        mIsLocallyChanged = isLocallyChanged;
    }

    public long getLastKnownRemoteFeaturedImageId() {
        return mLastKnownRemoteFeaturedImageId;
    }

    public void setLastKnownRemoteFeaturedImageId(long lastKnownRemoteFeaturedImageId) {
        mLastKnownRemoteFeaturedImageId = lastKnownRemoteFeaturedImageId;
    }

    public boolean getHasCapabilityPublishPost() {
        return mHasCapabilityPublishPost;
    }

    public void setHasCapabilityPublishPost(boolean hasCapabilityPublishPost) {
        mHasCapabilityPublishPost = hasCapabilityPublishPost;
    }

    public boolean getHasCapabilityEditPost() {
        return mHasCapabilityEditPost;
    }

    public void setHasCapabilityEditPost(boolean hasCapabilityEditPost) {
        mHasCapabilityEditPost = hasCapabilityEditPost;
    }

    public boolean getHasCapabilityDeletePost() {
        return mHasCapabilityDeletePost;
    }

    public void setHasCapabilityDeletePost(boolean hasCapabilityDeletePost) {
        mHasCapabilityDeletePost = hasCapabilityDeletePost;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        PostModel otherPost = (PostModel) other;

        return (getId() == otherPost.getId() &&
                getLocalSiteId() == otherPost.getLocalSiteId() &&
                getRemoteSiteId() == otherPost.getRemoteSiteId() &&
                getRemotePostId() == otherPost.getRemotePostId() &&
                getFeaturedImageId() == otherPost.getFeaturedImageId() &&
                Double.compare(otherPost.getLatitude(), getLatitude()) == 0 &&
                Double.compare(otherPost.getLongitude(), getLongitude()) == 0 &&
                isPage() == otherPost.isPage() &&
                isLocalDraft() == otherPost.isLocalDraft() &&
                isLocallyChanged() == otherPost.isLocallyChanged() &&
                getLastKnownRemoteFeaturedImageId() == otherPost.getLastKnownRemoteFeaturedImageId() &&
                getHasCapabilityPublishPost() == otherPost.getHasCapabilityPublishPost() &&
                getHasCapabilityEditPost() == otherPost.getHasCapabilityEditPost() &&
                getHasCapabilityDeletePost() == otherPost.getHasCapabilityDeletePost() &&
                getParentId() == otherPost.getParentId() &&
                getTitle() != null ? getTitle().equals(otherPost.getTitle()) : otherPost.getTitle() == null &&
                getContent() != null ? getContent().equals(otherPost.getContent()) : otherPost.getContent() == null &&
                getDateCreated() != null ? getDateCreated().equals(otherPost.getDateCreated()) :
                        otherPost.getDateCreated() == null &&
                getCategories() != null ? getCategories().equals(otherPost.getCategories()) :
                        otherPost.getCategories() == null &&
                getCustomFields() != null ? getCustomFields().equals(otherPost.getCustomFields()) :
                        otherPost.getCustomFields() == null &&
                getLink() != null ? getLink().equals(otherPost.getLink()) : otherPost.getLink() == null &&
                getExcerpt() != null ? getExcerpt().equals(otherPost.getExcerpt()) :
                        otherPost.getExcerpt() == null &&
                getKeywords() != null ? getKeywords().equals(otherPost.getKeywords()) :
                        otherPost.getKeywords() == null &&
                getStatus() != null ? getStatus().equals(otherPost.getStatus()) : otherPost.getStatus() == null &&
                getPassword() != null ? getPassword().equals(otherPost.getPassword()) :
                        otherPost.getPassword() == null &&
                getPostFormat() != null ? getPostFormat().equals(otherPost.getPostFormat()) :
                        otherPost.getPostFormat() == null &&
                getSlug() != null ? getSlug().equals(otherPost.getSlug()) : otherPost.getSlug() == null &&
                getParentTitle() != null ? getParentTitle().equals(otherPost.getParentTitle()) :
                        otherPost.getParentTitle() == null);
    }

    public JSONArray getJSONCategories() {
        JSONArray jArray = null;
        if (mCategories == null) {
            mCategories = "[]";
        }
        try {
            mCategories = StringUtils.unescapeHTML(mCategories);
            if (TextUtils.isEmpty(mCategories)) {
                jArray = new JSONArray();
            } else {
                jArray = new JSONArray(mCategories);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.POSTS, e);
        }
        return jArray;
    }

    public void setJSONCategories(JSONArray categories) {
        this.mCategories = categories.toString();
    }

    public JSONArray getJSONCustomFields() {
        if (mCustomFields == null) {
            return null;
        }
        JSONArray jArray = null;
        try {
            jArray = new JSONArray(mCustomFields);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.POSTS, "No custom fields found for post.");
        }
        return jArray;
    }

    public JSONObject getCustomField(String key) {
        JSONArray customFieldsJson = getJSONCustomFields();
        if (customFieldsJson == null) {
            return null;
        }

        for (int i = 0; i < customFieldsJson.length(); i++) {
            try {
                JSONObject jsonObject = new JSONObject(customFieldsJson.getString(i));
                String curentKey = jsonObject.getString("key");
                if (key.equals(curentKey)) {
                    return jsonObject;
                }
            } catch (JSONException e) {
                AppLog.e(AppLog.T.POSTS, e);
            }
        }
        return null;
    }

    public boolean supportsLocation() {
        // Right now, we only disable for pages.
        return !isPage();
    }

    public boolean hasLocation() {
        return getPostLocation() != null && getPostLocation().isValid();
    }

    public boolean featuredImageHasChanged() {
        return (mLastKnownRemoteFeaturedImageId != mFeaturedImageId);
    }
}
