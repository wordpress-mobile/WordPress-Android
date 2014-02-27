package org.wordpress.android.models;

import android.text.format.DateUtils;

import org.wordpress.android.util.StringUtils;

import java.util.Date;

/**
 * Barebones post/page as listed in PostsListFragment
 * Created by @roundhill on 11/5/13.
 */
public class PostsListPost {

    private int postId;
    private int blogId;
    private String title;
    private long dateCreatedGmt;
    private String status;
    private String formattedDate;
    private boolean isLocalDraft;
    private boolean hasLocalChanges;

    public PostsListPost(int postId, int blogId, String title, long dateCreatedGmt, String status, boolean localDraft, boolean localChanges) {
        setPostId(postId);
        setBlogId(blogId);
        setTitle(title);
        setDateCreatedGmt(dateCreatedGmt);
        setStatus(status);
        setFormattedDate();
        setLocalDraft(localDraft);
        setHasLocalChanges(localChanges);
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getBlogId() {
        return blogId;
    }

    public void setBlogId(int blogId) {
        this.blogId = blogId;
    }

    public String getTitle() {
        return StringUtils.notNullStr(title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDateCreatedGmt() {
        return dateCreatedGmt;
    }

    public void setDateCreatedGmt(long dateCreatedGmt) {
        this.dateCreatedGmt = dateCreatedGmt;
    }

    public String getStatus() {
        return StringUtils.notNullStr(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormattedDate() {

        return StringUtils.notNullStr(formattedDate);
    }

    public void setFormattedDate() {
        formattedDate = DateUtils.getRelativeTimeSpanString(getDateCreatedGmt(), new Date().getTime(), DateUtils.SECOND_IN_MILLIS).toString();
    }

    public boolean isLocalDraft() {
        return isLocalDraft;
    }

    public void setLocalDraft(boolean isLocalDraft) {
        this.isLocalDraft = isLocalDraft;
    }

    public boolean hasLocalChanges() {
        return hasLocalChanges;
    }

    public void setHasLocalChanges(boolean localChanges) {
        this.hasLocalChanges = localChanges;
    }

}
