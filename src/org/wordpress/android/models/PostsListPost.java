package org.wordpress.android.models;

import android.text.format.DateUtils;

import java.util.Date;

/**
 * Barebones post/page as listed in PostsListFragment
 * Created by dan on 11/5/13.
 */
public class PostsListPost {

    int postId;
    int blogId;
    String title;
    long dateCreatedGmt;
    String status;
    String formattedDate;

    public PostsListPost(int postId, int blogId, String title, long dateCreatedGmt, String status) {
        setPostId(postId);
        setBlogId(blogId);
        setTitle(title);
        setDateCreatedGmt(dateCreatedGmt);
        setStatus(status);
        setFormattedDate();
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
        return title;
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
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormattedDate() {

        return formattedDate;
    }

    public void setFormattedDate() {
        formattedDate = DateUtils.getRelativeTimeSpanString(getDateCreatedGmt(), new Date().getTime(), DateUtils.SECOND_IN_MILLIS).toString();
    }

}
