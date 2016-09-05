package org.wordpress.android.fluxc.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;

import java.io.Serializable;

@Table
public class CommentModel extends Payload implements Identifiable, Serializable {
    // Ids
    @PrimaryKey
    @Column private int mId;
    @Column private long mRemoteCommentId;
    @Column private int mLocalPostId;
    @Column private long mRemotePostId;

    // Author
    @Column private String authorUrl;
    @Column private String authorName;
    @Column private String authorEmail;
    @Column private String authorProfileImageUrl;

    //
    @Column private String postTitle;
    @Column private String status; // FIXME: Replace with enum
    @Column private String datePublished;
    @Column private String comment;

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

    public int getLocalPostId() {
        return mLocalPostId;
    }

    public void setLocalPostId(int localPostId) {
        mLocalPostId = localPostId;
    }

    public long getRemotePostId() {
        return mRemotePostId;
    }

    public void setRemotePostId(long remotePostId) {
        mRemotePostId = remotePostId;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getAuthorProfileImageUrl() {
        return authorProfileImageUrl;
    }

    public void setAuthorProfileImageUrl(String authorProfileImageUrl) {
        this.authorProfileImageUrl = authorProfileImageUrl;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDatePublished() {
        return datePublished;
    }

    public void setDatePublished(String datePublished) {
        this.datePublished = datePublished;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
