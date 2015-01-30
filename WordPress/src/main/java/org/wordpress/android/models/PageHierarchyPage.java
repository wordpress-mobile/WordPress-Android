package org.wordpress.android.models;

import org.wordpress.android.util.StringUtils;

public class PageHierarchyPage {
    private int mLocalTableBlogId;
    private long mLocalTablePostId;
    private String mRemotePostId;
    private String mTitle;
    private String mPageParentId;
    private boolean mHasLocalChanges;

    public PageHierarchyPage(int blogId) {
        mLocalTableBlogId = blogId;
    }

    public int getLocalTableBlogId() {
        return mLocalTableBlogId;
    }

    public void setLocalTableBlogId(int localTableBlogId) {
        mLocalTableBlogId = localTableBlogId;
    }

    public long getLocalTablePostId() {
        return mLocalTablePostId;
    }

    public void setLocalTablePostId(long id) {
        mLocalTablePostId = id;
    }

    public String getRemotePostId() {
        return StringUtils.notNullStr(mRemotePostId);
    }

    public void setRemotePostId(String postId) {
        mRemotePostId = postId;
    }

    public String getTitle() {
        return StringUtils.notNullStr(mTitle);
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getPageParentId() {
        return StringUtils.notNullStr(mPageParentId);
    }

    public void setPageParentId(String parentId) {
        this.mPageParentId = parentId;
    }

    public boolean isLocalChange() {
        return mHasLocalChanges;
    }

    public void setLocalChange(boolean hasLocalChanges) {
        mHasLocalChanges = hasLocalChanges;
    }
}