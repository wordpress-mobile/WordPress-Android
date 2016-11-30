package org.wordpress.android.fluxc.model;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Table
public class PostModel extends Payload implements Cloneable, Identifiable, Serializable {
    private static final long FEATURED_IMAGE_INIT_VALUE = -2;
    private static final long LATITUDE_REMOVED_VALUE = 8888;
    private static final long LONGITUDE_REMOVED_VALUE = 8888;

    @PrimaryKey
    @Column private int mId;
    @Column private int mLocalSiteId;
    @Column private long mRemoteSiteId; // .COM REST API
    @Column private long mRemotePostId;
    @Column private String mTitle;
    @Column private String mContent;
    @Column private String mDateCreated; // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column private String mCategoryIds;
    @Column private String mCustomFields;
    @Column private String mLink;
    @Column private String mExcerpt;
    @Column private String mTagIds;
    @Column private String mStatus;
    @Column private String mPassword;
    @Column private long mFeaturedImageId = FEATURED_IMAGE_INIT_VALUE;
    @Column private String mPostFormat;
    @Column private String mSlug;
    @Column private double mLatitude = PostLocation.INVALID_LATITUDE;
    @Column private double mLongitude = PostLocation.INVALID_LONGITUDE;

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

    public String getCategoryIds() {
        return StringUtils.notNullStr(mCategoryIds);
    }

    public void setCategoryIds(String categoryIds) {
        mCategoryIds = categoryIds;
    }

    @NonNull
    public List<Long> getCategoryIdList() {
        return taxonomyIdStringToList(mCategoryIds);
    }

    public void setCategoryIdList(List<Long> categories) {
        mCategoryIds = taxonomyIdListToString(categories);
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

    public String getTagIds() {
        return StringUtils.notNullStr(mTagIds);
    }

    public void setTagIds(String tagIds) {
        mTagIds = tagIds;
    }

    @NonNull
    public List<Long> getTagIdList() {
        return taxonomyIdStringToList(mTagIds);
    }

    public void setTagIdList(List<Long> tags) {
        mTagIds = taxonomyIdListToString(tags);
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

    public boolean hasFeaturedImage() {
        return mFeaturedImageId > 0;
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

    public PostLocation getLocation() {
        return new PostLocation(mLatitude, mLongitude);
    }

    public void setLocation(@NonNull PostLocation postLocation) {
        mLatitude = postLocation.getLatitude();
        mLongitude = postLocation.getLongitude();
    }

    public void setLocation(double latitude, double longitude) {
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
        if (this == other) return true;
        if (other == null || !(other instanceof PostModel)) return false;

        PostModel otherPost = (PostModel) other;

        return getId() == otherPost.getId() && getLocalSiteId() == otherPost.getLocalSiteId()
               && getRemoteSiteId() == otherPost.getRemoteSiteId() && getRemotePostId() == otherPost.getRemotePostId()
               && getFeaturedImageId() == otherPost.getFeaturedImageId()
               && Double.compare(otherPost.getLatitude(), getLatitude()) == 0
               && Double.compare(otherPost.getLongitude(), getLongitude()) == 0
               && isPage() == otherPost.isPage()
               && isLocalDraft() == otherPost.isLocalDraft() && isLocallyChanged() == otherPost.isLocallyChanged()
               && getLastKnownRemoteFeaturedImageId() == otherPost.getLastKnownRemoteFeaturedImageId()
               && getHasCapabilityPublishPost() == otherPost.getHasCapabilityPublishPost()
               && getHasCapabilityEditPost() == otherPost.getHasCapabilityEditPost()
               && getHasCapabilityDeletePost() == otherPost.getHasCapabilityDeletePost()
               && getParentId() == otherPost.getParentId()
               && StringUtils.equals(getTitle(), otherPost.getTitle())
               && StringUtils.equals(getContent(), otherPost.getContent())
               && StringUtils.equals(getDateCreated(), otherPost.getDateCreated())
               && StringUtils.equals(getCategoryIds(), otherPost.getCategoryIds())
               && StringUtils.equals(getCustomFields(), otherPost.getCustomFields())
               && StringUtils.equals(getLink(), otherPost.getLink())
               && StringUtils.equals(getExcerpt(), otherPost.getExcerpt())
               && StringUtils.equals(getTagIds(), otherPost.getTagIds())
               && StringUtils.equals(getStatus(), otherPost.getStatus())
               && StringUtils.equals(getPassword(), otherPost.getPassword())
               && StringUtils.equals(getPostFormat(), otherPost.getPostFormat())
               && StringUtils.equals(getSlug(), otherPost.getSlug())
               && StringUtils.equals(getParentTitle(), otherPost.getParentTitle());
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
        return getLocation() != null && getLocation().isValid();
    }

    public boolean shouldDeleteLatitude() {
        return mLatitude == LATITUDE_REMOVED_VALUE;
    }

    public boolean shouldDeleteLongitude() {
        return mLongitude == LONGITUDE_REMOVED_VALUE;
    }

    public void clearLocation() {
        mLatitude = LATITUDE_REMOVED_VALUE;
        mLongitude = LONGITUDE_REMOVED_VALUE;
    }

    public boolean featuredImageHasChanged() {
        return (mLastKnownRemoteFeaturedImageId != mFeaturedImageId);
    }

    private static List<Long> taxonomyIdStringToList(String ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String[] stringArray = ids.split(",");
        List<Long> longList = new ArrayList<>();
        for (String categoryString : stringArray) {
            longList.add(Long.parseLong(categoryString));
        }
        return longList;
    }

    private static String taxonomyIdListToString(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return TextUtils.join(",", ids);
    }

    @Override
    public PostModel clone() {
        try {
            return (PostModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Can't happen
        }
    }
}
