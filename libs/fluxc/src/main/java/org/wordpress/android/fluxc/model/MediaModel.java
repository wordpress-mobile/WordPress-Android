package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

@Table
public class MediaModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = -1396457338496002846L;

    public enum MediaUploadState {
        QUEUED, UPLOADING, DELETING, DELETED, FAILED, UPLOADED;

        @NonNull
        public static MediaUploadState fromString(@Nullable String stringState) {
            if (stringState != null) {
                for (MediaUploadState state : MediaUploadState.values()) {
                    if (stringState.equalsIgnoreCase(state.toString())) {
                        return state;
                    }
                }
            }
            return UPLOADED;
        }
    }

    @PrimaryKey
    @Column private int mId;

    // Associated IDs
    @Column private int mLocalSiteId;
    @Column private int mLocalPostId; // The local post the media was uploaded from, for lookup after media uploads
    @Column private long mMediaId; // The remote ID of the media
    @Column private long mPostId; // The remote post ID ('parent') of the media
    @Column private long mAuthorId;
    @NonNull @Column private String mGuid;

    // Upload date, ISO 8601-formatted date in UTC
    @Nullable @Column private String mUploadDate;

    // Remote Url's
    @NonNull @Column private String mUrl;
    @Nullable @Column private String mThumbnailUrl;

    // File descriptors
    @Nullable @Column private String mFileName;
    @Nullable @Column private String mFilePath;
    @Nullable @Column private String mFileExtension;
    @Nullable @Column private String mMimeType;

    // Descriptive strings
    @Nullable @Column private String mTitle;
    @NonNull @Column private String mCaption;
    @NonNull @Column private String mDescription;
    @NonNull @Column private String mAlt;

    // Image and Video files only
    @Column private int mWidth;
    @Column private int mHeight;

    // Video and Audio files only
    @Column private int mLength;

    // Video only
    @Nullable @Column private String mVideoPressGuid;
    @Column private boolean mVideoPressProcessingDone;

    // Local only
    @Nullable @Column private String mUploadState;
    @Column private boolean mMarkedLocallyAsFeatured;

    // Other Sizes. Only available for images on self-hosted (xmlrpc layer) and Rest WPCOM sites
    @Nullable @Column private String mFileUrlMediumSize; // Self-hosted and wpcom
    @Nullable @Column private String mFileUrlMediumLargeSize; // Self-hosted only
    @Nullable @Column private String mFileUrlLargeSize; // Self-hosted and wpcom

    //
    // Legacy
    //
    @Column private int mHorizontalAlignment;
    @Column private boolean mVerticalAlignment;
    @Column private boolean mFeatured;
    @Column private boolean mFeaturedInPost;

    // Set to true on a successful response to delete via WP.com REST API, not stored locally
    private boolean mDeleted;

    /**
     * Enum representing various media fields with their default field names.
     * The default values can be changed by modifying the string parameter
     * passed to the enum constructor.
     */
    public enum MediaFields {
        PARENT_ID("parent_id"),
        TITLE("title"),
        DESCRIPTION("description"),
        CAPTION("caption"),
        ALT("alt");

        @NonNull private final String mFieldName;

        // Constructor
        MediaFields(@NonNull String fieldName) {
            this.mFieldName = fieldName;
        }

        // Getter
        @NonNull
        public String getFieldName() {
            return this.mFieldName;
        }
    }

    @NonNull private MediaFields[] mFieldsToUpdate = MediaFields.values();

    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public MediaModel() {
        this.mId = 0;
        this.mLocalSiteId = 0;
        this.mLocalPostId = 0;
        this.mMediaId = 0;
        this.mPostId = 0;
        this.mAuthorId = 0;
        this.mGuid = "";
        this.mUploadDate = null;
        this.mUrl = "";
        this.mThumbnailUrl = null;
        this.mFileName = null;
        this.mFilePath = null;
        this.mFileExtension = null;
        this.mMimeType = null;
        this.mTitle = null;
        this.mCaption = "";
        this.mDescription = "";
        this.mAlt = "";
        this.mWidth = 0;
        this.mHeight = 0;
        this.mLength = 0;
        this.mVideoPressGuid = null;
        this.mVideoPressProcessingDone = false;
        this.mUploadState = null;
        this.mMarkedLocallyAsFeatured = false;
        this.mFileUrlMediumSize = null;
        this.mFileUrlMediumLargeSize = null;
        this.mFileUrlLargeSize = null;
        this.mHorizontalAlignment = 0;
        this.mVerticalAlignment = false;
        this.mFeatured = false;
        this.mFeaturedInPost = false;
        this.mDeleted = false;
    }

    /**
     * Use when getting an existing media.
     */
    public MediaModel(
            int localSiteId,
            long mediaId) {
        this.mLocalSiteId = localSiteId;
        this.mMediaId = mediaId;
        this.mGuid = "";
        this.mUrl = "";
        this.mCaption = "";
        this.mDescription = "";
        this.mAlt = "";
    }

    /**
     * Use when converting local uri into a media, and then, to upload a new or update an existing media.
     */
    public MediaModel(
            int localSiteId,
            @Nullable String uploadDate,
            @Nullable String fileName,
            @Nullable String filePath,
            @Nullable String fileExtension,
            @Nullable String mimeType,
            @Nullable String title,
            @Nullable MediaUploadState uploadState) {
        this.mLocalSiteId = localSiteId;
        this.mGuid = "";
        this.mUploadDate = uploadDate;
        this.mUrl = "";
        this.mFileName = fileName;
        this.mFilePath = filePath;
        this.mFileExtension = fileExtension;
        this.mMimeType = mimeType;
        this.mTitle = title;
        this.mCaption = "";
        this.mDescription = "";
        this.mAlt = "";
        this.mUploadState = uploadState != null ? uploadState.toString() : null;
    }

    /**
     * Use when converting editor image metadata into a media.
     */
    public MediaModel(
            @NonNull String url,
            @Nullable String fileName,
            @Nullable String fileExtension,
            @Nullable String title,
            @NonNull String caption,
            @NonNull String alt,
            int width,
            int height) {
        this.mGuid = "";
        this.mUrl = url;
        this.mFileName = fileName;
        this.mFileExtension = fileExtension;
        this.mTitle = title;
        this.mCaption = caption;
        this.mDescription = "";
        this.mAlt = alt;
        this.mWidth = width;
        this.mHeight = height;
    }

    /**
     * Use when converting a media file into a media.
     */
    public MediaModel(
            int id,
            int localSiteId,
            long mediaId,
            @NonNull String url,
            @Nullable String thumbnailUrl,
            @Nullable String fileName,
            @Nullable String filePath,
            @Nullable String fileExtension,
            @Nullable String mimeType,
            @Nullable String title,
            @NonNull String caption,
            @NonNull String description,
            @NonNull String alt,
            @Nullable String videoPressGuid,
            @NonNull MediaUploadState uploadState) {
        this.mId = id;
        this.mLocalSiteId = localSiteId;
        this.mMediaId = mediaId;
        this.mGuid = "";
        this.mUrl = url;
        this.mThumbnailUrl = thumbnailUrl;
        this.mFileName = fileName;
        this.mFilePath = filePath;
        this.mFileExtension = fileExtension;
        this.mMimeType = mimeType;
        this.mTitle = title;
        this.mCaption = caption;
        this.mDescription = description;
        this.mAlt = alt;
        this.mVideoPressGuid = videoPressGuid;
        this.mUploadState = uploadState.toString();
    }

    public MediaModel(
            int localSiteId,
            long mediaId,
            long postId,
            long authorId,
            @NonNull String guid,
            @Nullable String uploadDate,
            @NonNull String url,
            @Nullable String thumbnailUrl,
            @Nullable String fileName,
            @Nullable String fileExtension,
            @Nullable String mimeType,
            @Nullable String title,
            @NonNull String caption,
            @NonNull String description,
            @NonNull String alt,
            int width,
            int height,
            int length,
            @Nullable String videoPressGuid,
            boolean videoPressProcessingDone,
            @NonNull MediaUploadState uploadState,
            @Nullable String fileUrlMediumSize,
            @Nullable String fileUrlMediumLargeSize,
            @Nullable String fileUrlLargeSize,
            boolean deleted) {
        this.mLocalSiteId = localSiteId;
        this.mMediaId = mediaId;
        this.mPostId = postId;
        this.mAuthorId = authorId;
        this.mGuid = guid;
        this.mUploadDate = uploadDate;
        this.mUrl = url;
        this.mThumbnailUrl = thumbnailUrl;
        this.mFileName = fileName;
        this.mFileExtension = fileExtension;
        this.mMimeType = mimeType;
        this.mTitle = title;
        this.mCaption = caption;
        this.mDescription = description;
        this.mAlt = alt;
        this.mWidth = width;
        this.mHeight = height;
        this.mLength = length;
        this.mVideoPressGuid = videoPressGuid;
        this.mVideoPressProcessingDone = videoPressProcessingDone;
        this.mUploadState = uploadState.toString();
        this.mFileUrlMediumSize = fileUrlMediumSize;
        this.mFileUrlMediumLargeSize = fileUrlMediumLargeSize;
        this.mFileUrlLargeSize = fileUrlLargeSize;
        this.mDeleted = deleted;
    }

    @Override
    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof MediaModel)) return false;

        MediaModel otherMedia = (MediaModel) other;

        return getId() == otherMedia.getId()
                && getLocalSiteId() == otherMedia.getLocalSiteId() && getLocalPostId() == otherMedia.getLocalPostId()
                && getMediaId() == otherMedia.getMediaId() && getPostId() == otherMedia.getPostId()
                && getAuthorId() == otherMedia.getAuthorId() && getWidth() == otherMedia.getWidth()
                && getHeight() == otherMedia.getHeight() && getLength() == otherMedia.getLength()
                && getHorizontalAlignment() == otherMedia.getHorizontalAlignment()
                && getVerticalAlignment() == otherMedia.getVerticalAlignment()
                && getVideoPressProcessingDone() == otherMedia.getVideoPressProcessingDone()
                && getFeatured() == otherMedia.getFeatured()
                && getFeaturedInPost() == otherMedia.getFeaturedInPost()
                && getMarkedLocallyAsFeatured() == otherMedia.getMarkedLocallyAsFeatured()
                && StringUtils.equals(getGuid(), otherMedia.getGuid())
                && StringUtils.equals(getUploadDate(), otherMedia.getUploadDate())
                && StringUtils.equals(getUrl(), otherMedia.getUrl())
                && StringUtils.equals(getThumbnailUrl(), otherMedia.getThumbnailUrl())
                && StringUtils.equals(getFileName(), otherMedia.getFileName())
                && StringUtils.equals(getFilePath(), otherMedia.getFilePath())
                && StringUtils.equals(getFileExtension(), otherMedia.getFileExtension())
                && StringUtils.equals(getMimeType(), otherMedia.getMimeType())
                && StringUtils.equals(getTitle(), otherMedia.getTitle())
                && StringUtils.equals(getDescription(), otherMedia.getDescription())
                && StringUtils.equals(getCaption(), otherMedia.getCaption())
                && StringUtils.equals(getAlt(), otherMedia.getAlt())
                && StringUtils.equals(getVideoPressGuid(), otherMedia.getVideoPressGuid())
                && StringUtils.equals(getUploadState(), otherMedia.getUploadState())
                && StringUtils.equals(getFileUrlMediumSize(), otherMedia.getFileUrlMediumSize())
                && StringUtils.equals(getFileUrlMediumLargeSize(), otherMedia.getFileUrlMediumLargeSize())
                && StringUtils.equals(getFileUrlLargeSize(), otherMedia.getFileUrlLargeSize());
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public int getId() {
        return mId;
    }

    public void setLocalSiteId(int localSiteId) {
        mLocalSiteId = localSiteId;
    }

    public int getLocalSiteId() {
        return mLocalSiteId;
    }

    public void setLocalPostId(int localPostId) {
        mLocalPostId = localPostId;
    }

    public int getLocalPostId() {
        return mLocalPostId;
    }

    public void setMediaId(long mediaId) {
        mMediaId = mediaId;
    }

    public long getMediaId() {
        return mMediaId;
    }

    public void setPostId(long postId) {
        mPostId = postId;
    }

    public long getPostId() {
        return mPostId;
    }

    public void setAuthorId(long authorId) {
        mAuthorId = authorId;
    }

    public long getAuthorId() {
        return mAuthorId;
    }

    public void setGuid(@NonNull String guid) {
        mGuid = guid;
    }

    @NonNull
    public String getGuid() {
        return mGuid;
    }

    public void setUploadDate(@Nullable String uploadDate) {
        mUploadDate = uploadDate;
    }

    @Nullable
    public String getUploadDate() {
        return mUploadDate;
    }

    public void setUrl(@NonNull String url) {
        mUrl = url;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    public void setThumbnailUrl(@Nullable String thumbnailUrl) {
        mThumbnailUrl = thumbnailUrl;
    }

    @Nullable
    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    public void setFileName(@Nullable String fileName) {
        mFileName = fileName;
    }

    @Nullable
    public String getFileName() {
        return mFileName;
    }

    public void setFilePath(@Nullable String filePath) {
        mFilePath = filePath;
    }

    @Nullable
    public String getFilePath() {
        return mFilePath;
    }

    public void setFileExtension(@Nullable String fileExtension) {
        mFileExtension = fileExtension;
    }

    @Nullable
    public String getFileExtension() {
        return mFileExtension;
    }

    public void setMimeType(@Nullable String mimeType) {
        mMimeType = mimeType;
    }

    @Nullable
    public String getMimeType() {
        return mMimeType;
    }

    public void setTitle(@Nullable String title) {
        mTitle = title;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    public void setCaption(@NonNull String caption) {
        mCaption = caption;
    }

    @NonNull
    public String getCaption() {
        return mCaption;
    }

    public void setDescription(@NonNull String description) {
        mDescription = description;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public void setAlt(@NonNull String alt) {
        mAlt = alt;
    }

    @NonNull
    public String getAlt() {
        return mAlt;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setLength(int length) {
        mLength = length;
    }

    public int getLength() {
        return mLength;
    }

    public void setVideoPressGuid(@Nullable String videoPressGuid) {
        mVideoPressGuid = videoPressGuid;
    }

    @Nullable
    public String getVideoPressGuid() {
        return mVideoPressGuid;
    }

    public void setVideoPressProcessingDone(boolean videoPressProcessingDone) {
        mVideoPressProcessingDone = videoPressProcessingDone;
    }

    public boolean getVideoPressProcessingDone() {
        return mVideoPressProcessingDone;
    }

    public void setUploadState(@Nullable String uploadState) {
        mUploadState = uploadState;
    }

    public void setUploadState(@NonNull MediaUploadState uploadState) {
        mUploadState = uploadState.toString();
    }

    @Nullable
    public String getUploadState() {
        return mUploadState;
    }

    @NonNull
    public MediaFields[] getFieldsToUpdate() {
        return mFieldsToUpdate;
    }

    @SuppressWarnings("unused")
    public void setFieldsToUpdate(@NonNull MediaFields[] fieldsToUpdate) {
        this.mFieldsToUpdate = fieldsToUpdate;
    }

    //
    // Legacy methods
    //

    public boolean isVideo() {
        return MediaUtils.isVideoMimeType(getMimeType());
    }

    public void setHorizontalAlignment(int horizontalAlignment) {
        mHorizontalAlignment = horizontalAlignment;
    }

    public int getHorizontalAlignment() {
        return mHorizontalAlignment;
    }

    public void setVerticalAlignment(boolean verticalAlignment) {
        mVerticalAlignment = verticalAlignment;
    }

    public boolean getVerticalAlignment() {
        return mVerticalAlignment;
    }

    public void setFeatured(boolean featured) {
        mFeatured = featured;
    }

    public boolean getFeatured() {
        return mFeatured;
    }

    public void setFeaturedInPost(boolean featuredInPost) {
        mFeaturedInPost = featuredInPost;
    }

    public boolean getMarkedLocallyAsFeatured() {
        return mMarkedLocallyAsFeatured;
    }

    public void setMarkedLocallyAsFeatured(boolean markedLocallyAsFeatured) {
        mMarkedLocallyAsFeatured = markedLocallyAsFeatured;
    }

    public boolean getFeaturedInPost() {
        return mFeaturedInPost;
    }

    public void setDeleted(boolean deleted) {
        mDeleted = deleted;
    }

    public boolean getDeleted() {
        return mDeleted;
    }

    public void setFileUrlMediumSize(@Nullable String file) {
        mFileUrlMediumSize = file;
    }

    @Nullable
    public String getFileUrlMediumSize() {
        return mFileUrlMediumSize;
    }

    public void setFileUrlMediumLargeSize(@Nullable String file) {
        mFileUrlMediumLargeSize = file;
    }

    @Nullable
    public String getFileUrlMediumLargeSize() {
        return mFileUrlMediumLargeSize;
    }

    public void setFileUrlLargeSize(@Nullable String file) {
        mFileUrlLargeSize = file;
    }

    @Nullable
    public String getFileUrlLargeSize() {
        return mFileUrlLargeSize;
    }
}
