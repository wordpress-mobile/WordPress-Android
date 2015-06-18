package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.StringUtils;

import java.text.BreakIterator;
import java.util.Date;

/**
 * Barebones post/page as listed in PostsListFragment
 */
public class PostsListPost {
    private static final int MAX_EXCERPT_LEN = 150;

    private long postId;
    private long blogId;
    private long dateCreatedGmt;

    private String title;
    private String excerpt;
    private String featuredImageUrl;
    private String status;

    private java.util.Date dtCreatedGmt;

    private boolean isLocalDraft;
    private boolean hasLocalChanges;
    private boolean isUploading;

    public PostsListPost(Post post) {
        setPostId(post.getLocalTablePostId());
        setBlogId(post.getLocalTableBlogId());

        setTitle(post.getTitle());
        setExcerpt(post.getPostExcerpt());

        setStatus(post.getPostStatus());
        setLocalDraft(post.isLocalDraft());
        setHasLocalChanges(post.hasChangedFromLocalDraftToPublished());
        setIsUploading(post.isUploading());
        setDateCreatedGmt(post.getDate_created_gmt());

        // generate the excerpt and featured image from the post's content if not set above
        if (!hasExcerpt()) {
            setExcerpt(makeExcerpt(post.getDescription()));
        }

        // TODO: our posts table doesn't store featured image so one is always generated here
        if (!hasFeaturedImage()) {
            ReaderImageScanner scanner = new ReaderImageScanner(post.getDescription(), false);
            this.featuredImageUrl = scanner.getLargestImage();
        }
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getBlogId() {
        return blogId;
    }

    public void setBlogId(long blogId) {
        this.blogId = blogId;
    }

    public String getTitle() {
        return StringUtils.notNullStr(title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean hasTitle() {
        return !TextUtils.isEmpty(title);
    }

    public String getExcerpt() {
        return StringUtils.notNullStr(excerpt);
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public boolean hasExcerpt() {
        return !TextUtils.isEmpty(excerpt);
    }

    private static String makeExcerpt(String description) {
        if (TextUtils.isEmpty(description)) {
            return null;
        }

        String s = HtmlUtils.fastStripHtml(description);
        if (s.length() < MAX_EXCERPT_LEN) {
            return s.trim();
        }

        StringBuilder result = new StringBuilder();
        BreakIterator wordIterator = BreakIterator.getWordInstance();
        wordIterator.setText(s);
        int start = wordIterator.first();
        int end = wordIterator.next();
        int totalLen = 0;
        while (end != BreakIterator.DONE) {
            String word = s.substring(start, end);
            result.append(word);
            totalLen += word.length();
            if (totalLen >= MAX_EXCERPT_LEN) {
                break;
            }
            start = end;
            end = wordIterator.next();
        }

        if (totalLen == 0) {
            return null;
        }
        return result.toString().trim() + "...";
    }

    public String getFeaturedImageUrl() {
        return StringUtils.notNullStr(featuredImageUrl);
    }

    public boolean hasFeaturedImage() {
        return !TextUtils.isEmpty(featuredImageUrl);
    }

    public long getDateCreatedGmt() {
        return dateCreatedGmt;
    }
    public void setDateCreatedGmt(long dateCreatedGmt) {
        this.dateCreatedGmt = dateCreatedGmt;
    }

    public String getOriginalStatus() {
        return StringUtils.notNullStr(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PostStatus getStatusEnum() {
        return PostStatus.fromPostsListPost(this);
    }

    public String getFormattedDate() {
        return DateTimeUtils.javaDateToTimeSpan(new Date(dateCreatedGmt));
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

    public boolean isUploading() {
        return isUploading;
    }

    public void setIsUploading(boolean uploading) {
        this.isUploading = uploading;
    }
}
