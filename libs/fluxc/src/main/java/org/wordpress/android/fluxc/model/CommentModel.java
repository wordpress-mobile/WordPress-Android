package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

import java.io.Serializable;

@Table
@SuppressWarnings("NotNullFieldNotInitialized")
public class CommentModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = 3454722213760369852L;

    // Ids
    @PrimaryKey
    @Column private int mId;
    @Column private long mRemoteCommentId;
    @Column private long mRemotePostId;
    @Column private int mLocalSiteId;
    @Column private long mRemoteSiteId;

    // Comment author
    @Nullable @Column private String mAuthorUrl;
    @Nullable @Column private String mAuthorName;
    @Nullable @Column private String mAuthorEmail;
    @Column private long mAuthorId;
    @Nullable @Column private String mAuthorProfileImageUrl;

    // Comment data
    @Nullable @Column private String mPostTitle;
    @NonNull @Column private String mStatus;
    @NonNull @Column private String mDatePublished;
    @Column private long mPublishedTimestamp;
    @NonNull @Column private String mContent;
    @NonNull @Column private String mUrl;

    // Parent Comment Data
    @Column private boolean mHasParent;
    @Column private long mParentId;

    // WPCOM only
    @Column private boolean mILike; // current user likes this comment

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    public long getRemoteCommentId() {
        return mRemoteCommentId;
    }

    public void setRemoteCommentId(long remoteCommentId) {
        mRemoteCommentId = remoteCommentId;
    }

    public long getRemotePostId() {
        return mRemotePostId;
    }

    public void setRemotePostId(long remotePostId) {
        mRemotePostId = remotePostId;
    }

    @Nullable
    public String getAuthorUrl() {
        return mAuthorUrl;
    }

    public void setAuthorUrl(@Nullable String authorUrl) {
        this.mAuthorUrl = authorUrl;
    }

    @Nullable
    public String getAuthorName() {
        return mAuthorName;
    }

    public void setAuthorName(@Nullable String authorName) {
        this.mAuthorName = authorName;
    }

    @Nullable
    public String getAuthorEmail() {
        return mAuthorEmail;
    }

    public void setAuthorEmail(@Nullable String authorEmail) {
        this.mAuthorEmail = authorEmail;
    }

    @Nullable
    public String getAuthorProfileImageUrl() {
        return mAuthorProfileImageUrl;
    }

    public void setAuthorProfileImageUrl(@Nullable String authorProfileImageUrl) {
        this.mAuthorProfileImageUrl = authorProfileImageUrl;
    }

    @Nullable
    public String getPostTitle() {
        return mPostTitle;
    }

    public void setPostTitle(@Nullable String postTitle) {
        this.mPostTitle = postTitle;
    }

    @NonNull
    public String getStatus() {
        return mStatus;
    }

    public void setStatus(@NonNull String status) {
        this.mStatus = status;
    }

    @NonNull
    public String getDatePublished() {
        return mDatePublished;
    }

    public void setDatePublished(@NonNull String datePublished) {
        this.mDatePublished = datePublished;
    }

    @NonNull
    public String getContent() {
        return mContent;
    }

    public void setContent(@NonNull String content) {
        this.mContent = content;
    }

    public int getLocalSiteId() {
        return mLocalSiteId;
    }

    public void setLocalSiteId(int localSiteId) {
        mLocalSiteId = localSiteId;
    }

    public long getRemoteSiteId() {
        return mRemoteSiteId;
    }

    public void setRemoteSiteId(long remoteSiteId) {
        mRemoteSiteId = remoteSiteId;
    }

    public long getAuthorId() {
        return mAuthorId;
    }

    public void setAuthorId(long authorId) {
        mAuthorId = authorId;
    }

    public boolean getILike() {
        return mILike;
    }

    public void setILike(boolean iLike) {
        mILike = iLike;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    public void setUrl(@NonNull String url) {
        mUrl = url;
    }

    public long getPublishedTimestamp() {
        return mPublishedTimestamp;
    }

    public void setPublishedTimestamp(long publishedTimestamp) {
        mPublishedTimestamp = publishedTimestamp;
    }

    public boolean getHasParent() {
        return mHasParent;
    }

    public void setHasParent(boolean hasParent) {
        mHasParent = hasParent;
    }

    public long getParentId() {
        return mParentId;
    }

    public void setParentId(long parentId) {
        mParentId = parentId;
    }
}
