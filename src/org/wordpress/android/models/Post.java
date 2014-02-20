package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

public class Post implements Serializable {

    // Increment this value if this model changes
    // See: http://www.javapractices.com/topic/TopicAction.do?Id=45
    static final long serialVersionUID  = 1L;

    public static String QUICK_MEDIA_TYPE_PHOTO = "QuickPhoto";
    public static String QUICK_MEDIA_TYPE_VIDEO = "QuickVideo";

    private long id;
    private int blogID;
    private String categories;
    private String custom_fields;
    private long dateCreated;
    private long date_created_gmt;
    private String description;
    private String link;
    private boolean mt_allow_comments;
    private boolean mt_allow_pings;
    private String mt_excerpt;
    private String mt_keywords;
    private String mt_text_more;
    private String permaLink;
    private String post_status;
    private String postid;
    private String title;
    private String userid;
    private String wp_author_display_name;
    private String wp_author_id;
    private String wp_password;
    private String wp_post_format;
    private String wp_slug;
    private boolean localDraft;
    private boolean uploaded;
    private double latitude;
    private double longitude;
    private boolean isPage;
    private boolean isLocalChange;

    private String mediaPaths;
    private String quickPostType;

    public List<String> imageUrl = new Vector<String>();

    public Post(int blog_id, long post_id, boolean isPage) {
        // load an existing post
        List<Object> postVals = WordPress.wpDB.loadPost(blog_id, isPage, post_id);
        if (postVals != null) {
            this.id = (Long) postVals.get(0);
            this.blogID = blog_id;
            if (postVals.get(2) != null)
                this.postid = postVals.get(2).toString();
            this.title = postVals.get(3).toString();
            this.dateCreated = (Long) postVals.get(4);
            this.date_created_gmt = (Long) postVals.get(5);
            this.categories = postVals.get(6).toString();
            this.custom_fields = postVals.get(7).toString();
            this.description = postVals.get(8).toString();
            this.link = postVals.get(9).toString();
            this.mt_allow_comments = (Integer) postVals.get(10) > 0;
            this.mt_allow_pings = (Integer) postVals.get(11) > 0;
            this.mt_excerpt = postVals.get(12).toString();
            this.mt_keywords = postVals.get(13).toString();
            if (postVals.get(14) != null)
                this.mt_text_more = postVals.get(14).toString();
            else
                this.mt_text_more = "";
            this.permaLink = postVals.get(15).toString();
            this.post_status = postVals.get(16).toString();
            this.userid = postVals.get(17).toString();
            this.wp_author_display_name = postVals.get(18).toString();
            this.wp_author_id = postVals.get(19).toString();
            this.wp_password = postVals.get(20).toString();
            this.wp_post_format = postVals.get(21).toString();
            this.wp_slug = postVals.get(22).toString();
            this.mediaPaths = postVals.get(23).toString();
            this.latitude = (Double) postVals.get(24);
            this.longitude = (Double) postVals.get(25);
            this.localDraft = (Integer) postVals.get(26) > 0;
            this.uploaded = (Integer) postVals.get(27) > 0;
            this.isPage = (Integer) postVals.get(28) > 0;
            this.isLocalChange = (Integer) postVals.get(29) > 0;
        } else {
            this.id = -1;
        }
    }

    public Post(int blogId, boolean isPage) {
        // creates a new, empty post for the passed in blogId
        this(blogId, "", "", "", "", 0, "", "", "","", 0, 0, isPage, "", false);
        this.localDraft = true;
        save();
    }

    public Post(int blog_id, String title, String content, String excerpt, String picturePaths, long date, String categories, String tags, String status,
            String password, double latitude, double longitude, boolean isPage, String postFormat, boolean isLocalChange) {
        this.blogID = blog_id;
        this.title = title;
        this.description = content;
        this.mt_excerpt = excerpt;
        this.mediaPaths = picturePaths;
        this.date_created_gmt = date;
        this.categories = categories;
        this.mt_keywords = tags;
        this.post_status = status;
        this.wp_password = password;
        this.isPage = isPage;
        this.wp_post_format = postFormat;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isLocalChange = isLocalChange;
    }

    public long getId() {
        return id;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getDate_created_gmt() {
        return date_created_gmt;
    }

    public void setDate_created_gmt(long dateCreatedGmt) {
        date_created_gmt = dateCreatedGmt;
    }

    public int getBlogID() {
        return blogID;
    }

    public void setBlogID(int blogID) {
        this.blogID = blogID;
    }

    public boolean isLocalDraft() {
        return localDraft;
    }

    public void setLocalDraft(boolean localDraft) {
        this.localDraft = localDraft;
    }

    public JSONArray getJSONCategories() {
        JSONArray jArray = null;
        if (categories == null)
            categories = "";
        try {
            categories = StringUtils.unescapeHTML(categories);
            jArray = new JSONArray(categories);
        } catch (JSONException e) {
            AppLog.e(T.POSTS, e);
        }
        return jArray;
    }

    public void setJSONCategories(JSONArray categories) {
        this.categories = categories.toString();
    }

    public JSONArray getCustom_fields() {
        JSONArray jArray = null;
        try {
            jArray = new JSONArray(custom_fields);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jArray;
    }

    public void setCustom_fields(JSONArray customFields) {
        custom_fields = customFields.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isMt_allow_comments() {
        return mt_allow_comments;
    }

    public void setMt_allow_comments(boolean mtAllowComments) {
        mt_allow_comments = mtAllowComments;
    }

    public boolean isMt_allow_pings() {
        return mt_allow_pings;
    }

    public void setMt_allow_pings(boolean mtAllowPings) {
        mt_allow_pings = mtAllowPings;
    }

    public String getMt_excerpt() {
        return mt_excerpt;
    }

    public void setMt_excerpt(String mtExcerpt) {
        mt_excerpt = mtExcerpt;
    }

    public String getMt_keywords() {
        if (mt_keywords == null)
            return "";
        else
            return mt_keywords;
    }

    public void setMt_keywords(String mtKeywords) {
        mt_keywords = mtKeywords;
    }

    public String getMt_text_more() {
        if (mt_text_more == null)
            return "";
        else
            return mt_text_more;
    }

    public void setMt_text_more(String mtTextMore) {
        mt_text_more = mtTextMore;
    }

    public String getPermaLink() {
        return permaLink;
    }

    public void setPermaLink(String permaLink) {
        this.permaLink = permaLink;
    }

    public String getPost_status() {
        return post_status;
    }

    public void setPost_status(String postStatus) {
        post_status = postStatus;
    }

    public String getPostid() {
        return postid;
    }

    public void setPostid(String postid) {
        this.postid = postid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getWP_author_display_name() {
        return wp_author_display_name;
    }

    public void setWP_author_display_name(String wpAuthorDisplayName) {
        wp_author_display_name = wpAuthorDisplayName;
    }

    public String getWP_author_id() {
        return wp_author_id;
    }

    public void setWP_author_id(String wpAuthorId) {
        wp_author_id = wpAuthorId;
    }

    public String getWP_password() {
        return wp_password;
    }

    public void setWP_password(String wpPassword) {
        wp_password = wpPassword;
    }

    public String getWP_post_format() {
        return wp_post_format;
    }

    public void setWP_post_form(String wpPostForm) {
        wp_post_format = wpPostForm;
    }

    public String getWP_slug() {
        return wp_slug;
    }

    public void setWP_slug(String wpSlug) {
        wp_slug = wpSlug;
    }

    public String getMediaPaths() {
        return mediaPaths;
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

    public boolean save() {
        long newPostID = WordPress.wpDB.savePost(this, this.blogID);

        if (newPostID >= 0 && this.isLocalDraft() && !this.isUploaded()) {
            this.id = newPostID;
            return true;
        }

        return false;
    }

    public boolean update() {
        int success = WordPress.wpDB.updatePost(this, this.blogID);

        return success > 0;
    }

    public void delete() {
        // deletes a post/page draft
        WordPress.wpDB.deletePost(this);
    }

    public void deleteMediaFiles() {
        WordPress.wpDB.deleteMediaFilesForPost(this);
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setQuickPostType(String type) {
        this.quickPostType = type;
    }

    public String getQuickPostType() {
        return quickPostType;
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
                                      StringUtils.equals(mt_excerpt, otherPost.mt_excerpt) &&
                                      StringUtils.equals(mt_keywords, otherPost.mt_keywords) &&
                                      StringUtils.equals(categories, otherPost.categories) &&
                                      StringUtils.equals(post_status, otherPost.post_status) &&
                                      StringUtils.equals(wp_password, otherPost.wp_password) &&
                                      StringUtils.equals(wp_post_format, otherPost.wp_post_format) &&
                                      this.date_created_gmt == otherPost.date_created_gmt &&
                                      this.latitude == otherPost.latitude &&
                                      this.longitude == otherPost.longitude);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + blogID;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + (isPage ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof Post) {
            Post otherPost = (Post) other;
            return (this.id == otherPost.id &&
                    this.isPage == otherPost.isPage &&
                    this.blogID == otherPost.blogID
            );
        } else {
            return false;
        }
    }

    /**
     * Get the entire post content
     * Joins description and mt_text_more fields if both are valid
     * @return post content as String
     */
    public String getContent() {
        String postContent;
        if (!getMt_text_more().equals("")) {
            if (isLocalDraft())
                postContent = getDescription() + "\n&lt;!--more--&gt;\n" + getMt_text_more();
            else
                postContent = getDescription() + "\n<!--more-->\n" + getMt_text_more();
        } else
            postContent = getDescription();

        return postContent;
    }

    public boolean isNew() {
        return getId() >= 0;
    }
}