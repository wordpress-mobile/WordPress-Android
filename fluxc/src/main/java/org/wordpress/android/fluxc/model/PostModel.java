package org.wordpress.android.fluxc.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    @Column private String mTagNames;
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

    // Local only
    @Column private boolean mIsLocalDraft;
    @Column private boolean mIsLocallyChanged;
    @Column private String mDateLocallyChanged; // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z

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

    public @NonNull String getTitle() {
        return StringUtils.notNullStr(mTitle);
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public @NonNull String getContent() {
        return StringUtils.notNullStr(mContent);
    }

    public void setContent(String content) {
        mContent = content;
    }

    public @NonNull String getDateCreated() {
        return StringUtils.notNullStr(mDateCreated);
    }

    public void setDateCreated(String dateCreated) {
        mDateCreated = dateCreated;
    }

    public @NonNull String getCategoryIds() {
        return StringUtils.notNullStr(mCategoryIds);
    }

    public void setCategoryIds(String categoryIds) {
        mCategoryIds = categoryIds;
    }

    public @NonNull List<Long> getCategoryIdList() {
        return termIdStringToList(mCategoryIds);
    }

    public void setCategoryIdList(List<Long> categories) {
        mCategoryIds = termIdListToString(categories);
    }

    public @NonNull String getCustomFields() {
        return StringUtils.notNullStr(mCustomFields);
    }

    public void setCustomFields(String customFields) {
        mCustomFields = customFields;
    }

    public @NonNull String getLink() {
        return StringUtils.notNullStr(mLink);
    }

    public void setLink(String link) {
        mLink = link;
    }

    public @NonNull String getExcerpt() {
        return StringUtils.notNullStr(mExcerpt);
    }

    public void setExcerpt(String excerpt) {
        mExcerpt = excerpt;
    }

    public @NonNull String getTagNames() {
        return StringUtils.notNullStr(mTagNames);
    }

    public void setTagNames(String tags) {
        mTagNames = tags;
    }

    public @NonNull List<String> getTagNameList() {
        return termNameStringToList(mTagNames);
    }

    public void setTagNameList(List<String> tags) {
        mTagNames = termNameListToString(tags);
    }

    public @NonNull String getStatus() {
        return StringUtils.notNullStr(mStatus);
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public @NonNull String getPassword() {
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

    public @NonNull String getPostFormat() {
        return StringUtils.notNullStr(mPostFormat);
    }

    public void setPostFormat(String postFormat) {
        mPostFormat = postFormat;
    }

    public @NonNull String getSlug() {
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

    public @NonNull PostLocation getLocation() {
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

    public @NonNull String getParentTitle() {
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

    public @NonNull String getDateLocallyChanged() {
        return StringUtils.notNullStr(mDateLocallyChanged);
    }

    public void setDateLocallyChanged(String dateLocallyChanged) {
        mDateLocallyChanged = dateLocallyChanged;
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
                && StringUtils.equals(getTagNames(), otherPost.getTagNames())
                && StringUtils.equals(getStatus(), otherPost.getStatus())
                && StringUtils.equals(getPassword(), otherPost.getPassword())
                && StringUtils.equals(getPostFormat(), otherPost.getPostFormat())
                && StringUtils.equals(getSlug(), otherPost.getSlug())
                && StringUtils.equals(getParentTitle(), otherPost.getParentTitle())
                && StringUtils.equals(getDateLocallyChanged(), otherPost.getDateLocallyChanged());
    }

    public @Nullable JSONArray getJSONCustomFields() {
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

    public @Nullable JSONObject getCustomField(String key) {
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
        return getLocation().isValid();
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

    public void clearFeaturedImage() {
        setFeaturedImageId(-1);
    }

    public boolean featuredImageHasChanged() {
        return (mLastKnownRemoteFeaturedImageId != mFeaturedImageId);
    }

    private static List<Long> termIdStringToList(String ids) {
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

    private static String termIdListToString(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return TextUtils.join(",", ids);
    }

    private static List<String> termNameStringToList(String terms) {
        if (terms == null || terms.isEmpty()) {
            return Collections.emptyList();
        }
        String[] stringArray = terms.split(",");
        List<String> stringList = new ArrayList<>();
        Collections.addAll(stringList, stringArray);
        return stringList;
    }

    private static String termNameListToString(List<String> termNames) {
        if (termNames == null || termNames.isEmpty()) {
            return "";
        }
        return TextUtils.join(",", termNames);
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
