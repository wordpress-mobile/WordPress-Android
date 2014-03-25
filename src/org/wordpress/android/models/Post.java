package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

public class Post implements Serializable {

    // Increment this value if this model changes
    // See: http://www.javapractices.com/topic/TopicAction.do?Id=45
    static final long serialVersionUID  = 2L;

    public static String QUICK_MEDIA_TYPE_PHOTO = "QuickPhoto";
    public static String QUICK_MEDIA_TYPE_VIDEO = "QuickVideo";

    private long localTablePostId;
    private int localTableBlogId;
    private String categories;
    private String customFields;
    private long dateCreated;
    private long dateCreatedGmt;
    private String description;
    private String link;
    private boolean allowComments;
    private boolean allowPings;
    private String excerpt;
    private String keywords;
    private String moreText;
    private String permaLink;
    private String status;
    private String remotePostId;
    private String title;
    private String userId;
    private String authorDisplayName;
    private String authorId;
    private String password;
    private String postFormat;
    private String slug;
    private boolean localDraft;
    private boolean uploaded;
    private double latitude;
    private double longitude;
    private boolean isPage;
    private String pageParentId;
    private String pageParentTitle;
    private boolean isLocalChange;
    private String mediaPaths;
    private String quickPostType;

    public Post() {

    }

    public Post(int blogId, boolean isPage) {
        // creates a new, empty post for the passed in blogId
        this.localTableBlogId = blogId;
        this.isPage = isPage;
        this.localDraft = true;
    }

    public long getLocalTablePostId() {
        return localTablePostId;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getDate_created_gmt() {
        return dateCreatedGmt;
    }

    public void setDate_created_gmt(long dateCreatedGmt) {
        this.dateCreatedGmt = dateCreatedGmt;
    }

    public void setCategories(String postCategories) {
        this.categories = postCategories;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    public int getLocalTableBlogId() {
        return localTableBlogId;
    }

    public void setLocalTableBlogId(int localTableBlogId) {
        this.localTableBlogId = localTableBlogId;
    }

    public boolean isLocalDraft() {
        return localDraft;
    }

    public void setLocalDraft(boolean localDraft) {
        this.localDraft = localDraft;
    }

    public JSONArray getJSONCategories() {
        JSONArray jArray = null;
        if (categories == null) {
            categories = "[]";
        }
        try {
            categories = StringUtils.unescapeHTML(categories);
            if (TextUtils.isEmpty(categories)) {
                jArray = new JSONArray();
            } else {
                jArray = new JSONArray(categories);
            }
        } catch (JSONException e) {
            AppLog.e(T.POSTS, e);
        }
        return jArray;
    }

    public void setJSONCategories(JSONArray categories) {
        this.categories = categories.toString();
    }

    public JSONArray getCustomFields() {
        JSONArray jArray = null;
        try {
            jArray = new JSONArray(customFields);
        } catch (JSONException e) {
            AppLog.e(T.POSTS, e);
        }
        return jArray;
    }

    public void setCustomFields(JSONArray customFields) {
        this.customFields = customFields.toString();
    }

    public String getDescription() {
        return StringUtils.notNullStr(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return StringUtils.notNullStr(link);
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isAllowComments() {
        return allowComments;
    }

    public void setAllowComments(boolean mtAllowComments) {
        allowComments = mtAllowComments;
    }

    public boolean isAllowPings() {
        return allowPings;
    }

    public void setAllowPings(boolean mtAllowPings) {
        allowPings = mtAllowPings;
    }

    public String getPostExcerpt() {
        return StringUtils.notNullStr(excerpt);
    }

    public void setPostExcerpt(String mtExcerpt) {
        excerpt = mtExcerpt;
    }

    public String getKeywords() {
            return StringUtils.notNullStr(keywords);
    }

    public void setKeywords(String mtKeywords) {
        keywords = mtKeywords;
    }

    public String getMoreText() {
        return StringUtils.notNullStr(moreText);
    }

    public void setMoreText(String mtTextMore) {
        moreText = mtTextMore;
    }

    public String getPermaLink() {
        return StringUtils.notNullStr(permaLink);
    }

    public void setPermaLink(String permaLink) {
        this.permaLink = permaLink;
    }

    public String getPostStatus() {
        return StringUtils.notNullStr(status);
    }

    public void setPostStatus(String postStatus) {
        status = postStatus;
    }

    public void setStatusEnum(PostStatus postStatus) {
        status = PostStatus.toString(postStatus);
    }

    public PostStatus getStatusEnum() {
        return PostStatus.fromPost(this);
    }

    public String getRemotePostId() {
        return StringUtils.notNullStr(remotePostId);
    }

    public void setRemotePostId(String postId) {
        this.remotePostId = postId;
    }

    public String getTitle() {
        return StringUtils.notNullStr(title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserId() {
        return StringUtils.notNullStr(userId);
    }

    public void setUserId(String userid) {
        this.userId = userid;
    }

    public String getAuthorDisplayName() {
        return StringUtils.notNullStr(authorDisplayName);
    }

    public void setAuthorDisplayName(String wpAuthorDisplayName) {
        authorDisplayName = wpAuthorDisplayName;
    }

    public String getAuthorId() {
        return StringUtils.notNullStr(authorId);
    }

    public void setAuthorId(String wpAuthorId) {
        authorId = wpAuthorId;
    }

    public String getPassword() {
        return StringUtils.notNullStr(password);
    }

    public void setPassword(String wpPassword) {
        password = wpPassword;
    }

    public String getPostFormat() {
        return StringUtils.notNullStr(postFormat);
    }

    public void setPostFormat(String wpPostForm) {
        postFormat = wpPostForm;
    }

    public String getSlug() {
        return StringUtils.notNullStr(slug);
    }

    public void setSlug(String wpSlug) {
        slug = wpSlug;
    }

    public String getMediaPaths() {
        return StringUtils.notNullStr(mediaPaths);
    }

    public void setMediaPaths(String mediaPaths) {
        this.mediaPaths = mediaPaths;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isPage() {
        return isPage;
    }

    public void setIsPage(boolean isPage) {
        this.isPage = isPage;
    }

    public String getPageParentId() {
        return StringUtils.notNullStr(pageParentId);
    }

    public void setPageParentId(String wp_page_parent_id) {
        this.pageParentId = wp_page_parent_id;
    }

    public String getPageParentTitle() {
        return StringUtils.notNullStr(pageParentTitle);
    }

    public void setPageParentTitle(String wp_page_parent_title) {
        this.pageParentTitle = wp_page_parent_title;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public boolean isLocalChange() {
        return isLocalChange;
    }

    public void setLocalChange(boolean isLocalChange) {
        this.isLocalChange = isLocalChange;
    }

    public void setLocalTablePostId(long id) {
        this.localTablePostId = id;
    }

    public void setQuickPostType(String type) {
        this.quickPostType = type;
    }

    public String getQuickPostType() {
        return StringUtils.notNullStr(quickPostType);
    }

    /**
     * Checks if this post currently has data differing from another post.
     *
     * @param otherPost The post to compare to this post's editable data.
     * @return True if this post's data differs from otherPost's data, False otherwise.
     */
    public boolean hasChanges(Post otherPost) {
        return otherPost == null || !(StringUtils.equals(title, otherPost.title) &&
                                      StringUtils.equals(description, otherPost.description) &&
                                      StringUtils.equals(excerpt, otherPost.excerpt) &&
                                      StringUtils.equals(keywords, otherPost.keywords) &&
                                      StringUtils.equals(categories, otherPost.categories) &&
                                      StringUtils.equals(status, otherPost.status) &&
                                      StringUtils.equals(password, otherPost.password) &&
                                      StringUtils.equals(postFormat, otherPost.postFormat) &&
                                      this.dateCreatedGmt == otherPost.dateCreatedGmt &&
                                      this.latitude == otherPost.latitude &&
                                      this.longitude == otherPost.longitude);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + localTableBlogId;
        result = prime * result + (int) (localTablePostId ^ (localTablePostId >>> 32));
        result = prime * result + (isPage ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof Post) {
            Post otherPost = (Post) other;
            return (this.localTablePostId == otherPost.localTablePostId &&
                    this.isPage == otherPost.isPage &&
                    this.localTableBlogId == otherPost.localTableBlogId
            );
        } else {
            return false;
        }
    }

    /**
     * Get the entire post content
     * Joins description and moreText fields if both are valid
     * @return post content as String
     */
    public String getContent() {
        String postContent;
        if (!TextUtils.isEmpty(getMoreText())) {
            if (isLocalDraft())
                postContent = getDescription() + "\n&lt;!--more--&gt;\n" + getMoreText();
            else
                postContent = getDescription() + "\n<!--more-->\n" + getMoreText();
        } else
            postContent = getDescription();

        return postContent;
    }

    public boolean isNew() {
        return getLocalTablePostId() >= 0;
    }

    public Post copy() {
        Post newPost = new Post();

        newPost.localTableBlogId = this.localTableBlogId;
        newPost.localTablePostId = this.localTablePostId;
        newPost.isPage = this.isPage;
        newPost.categories = this.categories;
        newPost.customFields = this.customFields;
        newPost.dateCreated = this.dateCreated;
        newPost.dateCreatedGmt = this.dateCreatedGmt;
        newPost.description = this.description;
        newPost.link = this.link;
        newPost.allowComments = this.allowComments;
        newPost.allowPings = this.allowPings;
        newPost.excerpt = this.excerpt;
        newPost.keywords = this.keywords;
        newPost.moreText = this.moreText;
        newPost.permaLink = this.permaLink;
        newPost.status = this.status;
        newPost.remotePostId = this.remotePostId;
        newPost.title = this.title;
        newPost.userId = this.userId;
        newPost.authorDisplayName = this.authorDisplayName;
        newPost.authorId = this.authorId;
        newPost.password = this.password;
        newPost.postFormat = this.postFormat;
        newPost.slug = this.slug;
        newPost.localDraft = this.localDraft;
        newPost.uploaded = this.uploaded;
        newPost.latitude = this.latitude;
        newPost.longitude = this.longitude;
        newPost.pageParentId = this.pageParentId;
        newPost.pageParentTitle = this.pageParentTitle;
        newPost.isLocalChange = this.isLocalChange;
        newPost.mediaPaths = this.mediaPaths;
        newPost.quickPostType = this.quickPostType;

        return newPost;
    }

}