package org.wordpress.android.fluxc.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

import java.io.Serializable;

@Table
public class CommentModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = 3454722213760369852L;

    // Ids
    @PrimaryKey
    @Column private int mId;
    @Column private long mRemoteCommentId;
    @Column private long mRemotePostId;
    @Column private long mRemoteParentCommentId;
    @Column private int mLocalSiteId;
    @Column private long mRemoteSiteId;

    // Comment author
    @Column private String mAuthorUrl;
    @Column private String mAuthorName;
    @Column private String mAuthorEmail;
    @Column private String mAuthorProfileImageUrl;

    // Comment data
    @Column private String mPostTitle;
    @Column private String mStatus;
    @Column private String mDatePublished;
    @Column private String mContent;
    @Column private String mUrl;

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

    public String getAuthorUrl() {
        return mAuthorUrl;
    }

    public void setAuthorUrl(String authorUrl) {
        this.mAuthorUrl = authorUrl;
    }

    public String getAuthorName() {
        return mAuthorName;
    }

    public void setAuthorName(String authorName) {
        this.mAuthorName = authorName;
    }

    public String getAuthorEmail() {
        return mAuthorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.mAuthorEmail = authorEmail;
    }

    public String getAuthorProfileImageUrl() {
        return mAuthorProfileImageUrl;
    }

    public void setAuthorProfileImageUrl(String authorProfileImageUrl) {
        this.mAuthorProfileImageUrl = authorProfileImageUrl;
    }

    public String getPostTitle() {
        return mPostTitle;
    }

    public void setPostTitle(String postTitle) {
        this.mPostTitle = postTitle;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStatus(String status) {
        this.mStatus = status;
    }

    public String getDatePublished() {
        return mDatePublished;
    }

    public void setDatePublished(String datePublished) {
        this.mDatePublished = datePublished;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
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

    public long getRemoteParentCommentId() {
        return mRemoteParentCommentId;
    }

    public void setRemoteParentCommentId(long remoteParentCommentId) {
        mRemoteParentCommentId = remoteParentCommentId;
    }

    public boolean getILike() {
        return mILike;
    }

    public void setILike(boolean iLike) {
        mILike = iLike;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }
}
