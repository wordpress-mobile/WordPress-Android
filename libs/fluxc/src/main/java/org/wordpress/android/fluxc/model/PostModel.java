package org.wordpress.android.fluxc.model;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Table
public class PostModel extends Payload<BaseNetworkError> implements Cloneable, Identifiable, Serializable,
        PostImmutableModel {
    private static final long serialVersionUID = 4524418637508876144L;

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
    @Column private String mLastModified; // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column private String mRemoteLastModified; // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column private String mCategoryIds;
    @Column private String mCustomFields;
    @Column private String mLink;
    @Column private String mExcerpt;
    @Column private String mTagNames;
    @Column private String mStatus;
    @Column private String mPassword;
    @Column private long mFeaturedImageId;
    @Column private String mPostFormat;
    @Column private String mSlug;
    @Column private double mLatitude = PostLocation.INVALID_LATITUDE;
    @Column private double mLongitude = PostLocation.INVALID_LONGITUDE;

    @Column private long mAuthorId;
    @Column private String mAuthorDisplayName;

    /**
     * This field stores a hashcode value of the post content when the user confirmed making the changes visible to
     * the users (Publish/Submit/Update/Schedule/Sync).
     * <p>
     * It is used to determine if the user actually confirmed the changes and if the post was edited since then.
     */
    @Column private int mChangesConfirmedContentHashcode;

    // Page specific
    @Column private boolean mIsPage;
    @Column private long mParentId;
    @Column private String mParentTitle;

    // Unpublished revision data
    @Column private long mAutoSaveRevisionId;
    @Column private String mAutoSaveModified; // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column private String mRemoteAutoSaveModified; // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column private String mAutoSavePreviewUrl;
    @Column private String mAutoSaveTitle;
    @Column private String mAutoSaveContent;
    @Column private String mAutoSaveExcerpt;

    // Local only
    @Column private boolean mIsLocalDraft;
    @Column private boolean mIsLocallyChanged;
    @Column private String mDateLocallyChanged; // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z

    // XML-RPC only, needed to work around a bug with the API:
    // https://github.com/wordpress-mobile/WordPress-Android/pull/3425
    // We may be able to drop this if we switch to wp.editPost (and it doesn't have the same bug as metaWeblog.editPost)
    @Column private long mLastKnownRemoteFeaturedImageId;

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

    @Override
    public int getLocalSiteId() {
        return mLocalSiteId;
    }

    public void setLocalSiteId(int localTableSiteId) {
        mLocalSiteId = localTableSiteId;
    }

    @Override
    public long getRemoteSiteId() {
        return mRemoteSiteId;
    }

    public void setRemoteSiteId(long siteId) {
        mRemoteSiteId = siteId;
    }

    @Override
    public long getRemotePostId() {
        return mRemotePostId;
    }

    public void setRemotePostId(long postId) {
        mRemotePostId = postId;
    }

    @Override
    public @NonNull String getTitle() {
        return StringUtils.notNullStr(mTitle);
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    @Override
    public @NonNull String getContent() {
        return StringUtils.notNullStr(mContent);
    }

    public void setContent(String content) {
        mContent = content;
    }

    @Override
    public @NonNull String getDateCreated() {
        return StringUtils.notNullStr(mDateCreated);
    }

    public void setDateCreated(String dateCreated) {
        mDateCreated = dateCreated;
    }

    @Override
    public @NonNull String getLastModified() {
        return StringUtils.notNullStr(mLastModified);
    }

    public void setLastModified(String lastModified) {
        mLastModified = lastModified;
    }

    @Override
    public @NonNull String getRemoteLastModified() {
        return StringUtils.notNullStr(mRemoteLastModified);
    }

    public void setRemoteLastModified(String remoteLastModified) {
        mRemoteLastModified = remoteLastModified;
    }

    @Override
    public @NonNull String getCategoryIds() {
        return StringUtils.notNullStr(mCategoryIds);
    }

    public void setCategoryIds(String categoryIds) {
        mCategoryIds = categoryIds;
    }

    @Override
    public @NonNull List<Long> getCategoryIdList() {
        return termIdStringToList(mCategoryIds);
    }

    public void setCategoryIdList(List<Long> categories) {
        mCategoryIds = termIdListToString(categories);
    }

    @Override
    public @NonNull String getCustomFields() {
        return StringUtils.notNullStr(mCustomFields);
    }

    public void setCustomFields(String customFields) {
        mCustomFields = customFields;
    }

    @Override
    public @NonNull String getLink() {
        return StringUtils.notNullStr(mLink);
    }

    public void setLink(String link) {
        mLink = link;
    }

    @Override
    public @NonNull String getExcerpt() {
        return StringUtils.notNullStr(mExcerpt);
    }

    public void setExcerpt(String excerpt) {
        mExcerpt = excerpt;
    }

    @Override
    public @NonNull String getTagNames() {
        return StringUtils.notNullStr(mTagNames);
    }

    public void setTagNames(String tags) {
        mTagNames = tags;
    }

    @Override
    public @NonNull List<String> getTagNameList() {
        return termNameStringToList(mTagNames);
    }

    public void setTagNameList(List<String> tags) {
        mTagNames = termNameListToString(tags);
    }

    @Override
    public @NonNull String getStatus() {
        return StringUtils.notNullStr(mStatus);
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    @Override
    public @NonNull String getPassword() {
        return StringUtils.notNullStr(mPassword);
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    @Override
    public boolean hasFeaturedImage() {
        return mFeaturedImageId > 0;
    }

    @Override
    public long getFeaturedImageId() {
        return mFeaturedImageId;
    }

    public void setFeaturedImageId(long featuredImageId) {
        mFeaturedImageId = featuredImageId;
    }

    @Override
    public @NonNull String getPostFormat() {
        return StringUtils.notNullStr(mPostFormat);
    }

    public void setPostFormat(String postFormat) {
        mPostFormat = postFormat;
    }

    @Override
    public @NonNull String getSlug() {
        return StringUtils.notNullStr(mSlug);
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }

    @Override
    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    @Override
    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    @Override
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

    @Override
    public long getAuthorId() {
        return mAuthorId;
    }

    public void setAuthorId(long authorId) {
        this.mAuthorId = authorId;
    }

    @Override
    public String getAuthorDisplayName() {
        return mAuthorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        mAuthorDisplayName = authorDisplayName;
    }

    @Override
    public int getChangesConfirmedContentHashcode() {
        return mChangesConfirmedContentHashcode;
    }

    public void setChangesConfirmedContentHashcode(int changesConfirmedContentHashcode) {
        mChangesConfirmedContentHashcode = changesConfirmedContentHashcode;
    }

    @Override
    public boolean isPage() {
        return mIsPage;
    }

    public void setIsPage(boolean isPage) {
        mIsPage = isPage;
    }

    @Override
    public long getParentId() {
        return mParentId;
    }

    public void setParentId(long parentId) {
        mParentId = parentId;
    }

    @Override
    public @NonNull String getParentTitle() {
        return StringUtils.notNullStr(mParentTitle);
    }

    public void setParentTitle(String parentTitle) {
        mParentTitle = parentTitle;
    }

    @Override
    public boolean isLocalDraft() {
        return mIsLocalDraft;
    }

    public void setIsLocalDraft(boolean isLocalDraft) {
        mIsLocalDraft = isLocalDraft;
    }

    @Override
    public boolean isLocallyChanged() {
        return mIsLocallyChanged;
    }

    public void setIsLocallyChanged(boolean isLocallyChanged) {
        mIsLocallyChanged = isLocallyChanged;
    }

    @Override
    public boolean hasUnpublishedRevision() {
        return mAutoSaveRevisionId > 0;
    }

    @Override
    public long getAutoSaveRevisionId() {
        return mAutoSaveRevisionId;
    }

    public void setAutoSaveRevisionId(long autoSaveRevisionId) {
        mAutoSaveRevisionId = autoSaveRevisionId;
    }

    @Override
    public String getAutoSaveModified() {
        return mAutoSaveModified;
    }

    public void setAutoSaveModified(String autoSaveModified) {
        mAutoSaveModified = autoSaveModified;
    }

    @Override
    public String getRemoteAutoSaveModified() {
        return mRemoteAutoSaveModified;
    }

    public void setRemoteAutoSaveModified(String remoteAutoSaveModified) {
        mRemoteAutoSaveModified = remoteAutoSaveModified;
    }

    @Override
    public String getAutoSavePreviewUrl() {
        return mAutoSavePreviewUrl;
    }

    public void setAutoSavePreviewUrl(String autoSavePreviewUrl) {
        mAutoSavePreviewUrl = autoSavePreviewUrl;
    }

    @Override
    public String getAutoSaveTitle() {
        return mAutoSaveTitle;
    }

    public void setAutoSaveTitle(String autoSaveTitle) {
        mAutoSaveTitle = autoSaveTitle;
    }

    @Override
    public String getAutoSaveContent() {
        return mAutoSaveContent;
    }

    public void setAutoSaveContent(String autoSaveContent) {
        mAutoSaveContent = autoSaveContent;
    }

    @Override
    public String getAutoSaveExcerpt() {
        return mAutoSaveExcerpt;
    }

    public void setAutoSaveExcerpt(String autoSaveExcerpt) {
        mAutoSaveExcerpt = autoSaveExcerpt;
    }

    @Deprecated
    public long getLastKnownRemoteFeaturedImageId() {
        return mLastKnownRemoteFeaturedImageId;
    }

    @Deprecated
    public void setLastKnownRemoteFeaturedImageId(long lastKnownRemoteFeaturedImageId) {
        mLastKnownRemoteFeaturedImageId = lastKnownRemoteFeaturedImageId;
    }

    @Override
    public boolean getHasCapabilityPublishPost() {
        return mHasCapabilityPublishPost;
    }

    public void setHasCapabilityPublishPost(boolean hasCapabilityPublishPost) {
        mHasCapabilityPublishPost = hasCapabilityPublishPost;
    }

    @Override
    public boolean getHasCapabilityEditPost() {
        return mHasCapabilityEditPost;
    }

    public void setHasCapabilityEditPost(boolean hasCapabilityEditPost) {
        mHasCapabilityEditPost = hasCapabilityEditPost;
    }

    @Override
    public boolean getHasCapabilityDeletePost() {
        return mHasCapabilityDeletePost;
    }

    public void setHasCapabilityDeletePost(boolean hasCapabilityDeletePost) {
        mHasCapabilityDeletePost = hasCapabilityDeletePost;
    }

    @Override
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
                && getAuthorId() == otherPost.getAuthorId()
                && Double.compare(otherPost.getLatitude(), getLatitude()) == 0
                && Double.compare(otherPost.getLongitude(), getLongitude()) == 0
                && isPage() == otherPost.isPage()
                && isLocalDraft() == otherPost.isLocalDraft() && isLocallyChanged() == otherPost.isLocallyChanged()
                && getHasCapabilityPublishPost() == otherPost.getHasCapabilityPublishPost()
                && getHasCapabilityEditPost() == otherPost.getHasCapabilityEditPost()
                && getHasCapabilityDeletePost() == otherPost.getHasCapabilityDeletePost()
                && getParentId() == otherPost.getParentId()
                && getAutoSaveRevisionId() == otherPost.getAutoSaveRevisionId()
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
                && StringUtils.equals(getAuthorDisplayName(), otherPost.getAuthorDisplayName())
                && StringUtils.equals(getDateLocallyChanged(), otherPost.getDateLocallyChanged())
                && StringUtils.equals(getAutoSaveModified(), otherPost.getAutoSaveModified())
                && StringUtils.equals(getAutoSavePreviewUrl(), otherPost.getAutoSavePreviewUrl());
    }

    /**
     * This method is used along with `mChangesConfirmedContentHashcode`. We store the contentHashcode of
     * the post when the user explicitly confirms that the changes to the post can be published. Beware, that when
     * you modify this method all users will need to re-confirm all the local changes. The changes wouldn't get
     * published otherwise.
     *
     * This is a method generated using Android Studio. When you need to add a new field it's safer to use the
     * generator again. (We can't use Objects.hash() since the current minSdkVersion is lower than 19.
     */
    @Override
    public int contentHashcode() {
        int result;
        long temp;
        result = mId;
        result = 31 * result + mLocalSiteId;
        result = 31 * result + (int) (mRemoteSiteId ^ (mRemoteSiteId >>> 32));
        result = 31 * result + (int) (mRemotePostId ^ (mRemotePostId >>> 32));
        result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
        result = 31 * result + (mContent != null ? mContent.hashCode() : 0);
        result = 31 * result + (mDateCreated != null ? mDateCreated.hashCode() : 0);
        result = 31 * result + (mCategoryIds != null ? mCategoryIds.hashCode() : 0);
        result = 31 * result + (mCustomFields != null ? mCustomFields.hashCode() : 0);
        result = 31 * result + (mLink != null ? mLink.hashCode() : 0);
        result = 31 * result + (mExcerpt != null ? mExcerpt.hashCode() : 0);
        result = 31 * result + (mTagNames != null ? mTagNames.hashCode() : 0);
        result = 31 * result + (mStatus != null ? mStatus.hashCode() : 0);
        result = 31 * result + (mPassword != null ? mPassword.hashCode() : 0);
        result = 31 * result + (int) (mAuthorId ^ (mAuthorId >>> 32));
        result = 31 * result + (mAuthorDisplayName != null ? mAuthorDisplayName.hashCode() : 0);
        result = 31 * result + (int) (mFeaturedImageId ^ (mFeaturedImageId >>> 32));
        result = 31 * result + (mPostFormat != null ? mPostFormat.hashCode() : 0);
        result = 31 * result + (mSlug != null ? mSlug.hashCode() : 0);
        temp = Double.doubleToLongBits(mLatitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mLongitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (mIsPage ? 1 : 0);
        result = 31 * result + (int) (mParentId ^ (mParentId >>> 32));
        result = 31 * result + (mParentTitle != null ? mParentTitle.hashCode() : 0);
        return result;
    }

    public @Nullable JSONArray getJsonCustomFields() {
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

    @Override
    public @Nullable JSONObject getCustomField(String key) {
        JSONArray customFieldsJson = getJsonCustomFields();
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

    @Override
    public boolean supportsLocation() {
        // Right now, we only disable for pages.
        return !isPage();
    }

    @Override
    public boolean hasLocation() {
        return getLocation().isValid();
    }

    @Override
    public boolean shouldDeleteLatitude() {
        return mLatitude == LATITUDE_REMOVED_VALUE;
    }

    @Override
    public boolean shouldDeleteLongitude() {
        return mLongitude == LONGITUDE_REMOVED_VALUE;
    }

    public void clearLocation() {
        mLatitude = LATITUDE_REMOVED_VALUE;
        mLongitude = LONGITUDE_REMOVED_VALUE;
    }

    public void clearFeaturedImage() {
        setFeaturedImageId(0);
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
        for (String s : stringArray) {
            if (s != null && !s.trim().isEmpty()) {
                stringList.add(s.trim());
            }
        }
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
