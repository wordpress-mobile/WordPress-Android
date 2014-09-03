package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

import java.text.BreakIterator;
import java.util.Iterator;

public class ReaderPost {
    private String pseudoId;
    public long postId;
    public long blogId;
    public long authorId;

    private String title;
    private String text;
    private String excerpt;
    private String authorName;
    private String blogName;
    private String blogUrl;
    private String postAvatar;

    private String primaryTag;    // most popular tag on this post based on usage in blog
    private String secondaryTag;  // second most popular tag on this post based on usage in blog

    public long timestamp;        // used for sorting
    private String published;

    private String url;
    private String featuredImage;
    private String featuredVideo;

    public int numReplies;        // includes comments, trackbacks & pingbacks
    public int numLikes;

    public boolean isLikedByCurrentUser;
    public boolean isFollowedByCurrentUser;
    public boolean isRebloggedByCurrentUser;
    public boolean isCommentsOpen;
    public boolean isExternal;
    public boolean isPrivate;
    public boolean isVideoPress;

    public boolean isLikesEnabled;
    public boolean isSharingEnabled;    // currently unused

    private String attachmentsJson;

    public static ReaderPost fromJson(JSONObject json) {
        if (json == null) {
            throw new IllegalArgumentException("null json post");
        }

        ReaderPost post = new ReaderPost();

        post.postId = json.optLong("ID");
        post.blogId = json.optLong("site_ID");

        if (json.has("pseudo_ID")) {
            post.pseudoId = JSONUtil.getString(json, "pseudo_ID");  // read/ endpoint
        } else {
            post.pseudoId = JSONUtil.getString(json, "global_ID");  // sites/ endpoint
        }

        // remove HTML from the excerpt
        post.excerpt = HtmlUtils.fastStripHtml(JSONUtil.getString(json, "excerpt"));

        post.text = JSONUtil.getString(json, "content");
        post.title = JSONUtil.getStringDecoded(json, "title");
        post.url = JSONUtil.getString(json, "URL");
        post.setBlogUrl(JSONUtil.getString(json, "site_URL"));

        post.numReplies = json.optInt("comment_count");
        post.numLikes = json.optInt("like_count");
        post.isLikedByCurrentUser = JSONUtil.getBool(json, "i_like");
        post.isFollowedByCurrentUser = JSONUtil.getBool(json, "is_following");
        post.isRebloggedByCurrentUser = JSONUtil.getBool(json, "is_reblogged");
        post.isCommentsOpen = JSONUtil.getBool(json, "comments_open");
        post.isExternal = JSONUtil.getBool(json, "is_external");
        post.isPrivate = JSONUtil.getBool(json, "site_is_private");

        post.isLikesEnabled = JSONUtil.getBool(json, "likes_enabled");
        post.isSharingEnabled = JSONUtil.getBool(json, "sharing_enabled");

        // parse the author section
        assignAuthorFromJson(post, json.optJSONObject("author"));

        // only freshly-pressed posts have the "editorial" section
        JSONObject jsonEditorial = json.optJSONObject("editorial");
        if (jsonEditorial != null) {
            post.blogId = jsonEditorial.optLong("blog_id");
            post.blogName = JSONUtil.getStringDecoded(jsonEditorial, "blog_name");
            post.featuredImage = ReaderImageScanner.getImageUrlFromFPFeaturedImageUrl(JSONUtil.getString(jsonEditorial, "image"));
            post.setPrimaryTag(JSONUtil.getString(jsonEditorial, "highlight_topic_title")); //  highlight_topic?
            // we want freshly-pressed posts to show & store the date they were chosen rather than the day they were published
            post.published = JSONUtil.getString(jsonEditorial, "displayed_on");
        } else {
            post.featuredImage = JSONUtil.getString(json, "featured_image");
            post.blogName = JSONUtil.getStringDecoded(json, "site_name");
            post.published = JSONUtil.getString(json, "date");
        }

        // the date a post was liked is only returned by the read/liked/ endpoint - if this exists,
        // set it as the timestamp so posts are sorted by the date they were liked rather than the
        // date they were published (the timestamp is used to sort posts when querying)
        String likeDate = JSONUtil.getString(json, "date_liked");
        if (!TextUtils.isEmpty(likeDate)) {
            post.timestamp = DateTimeUtils.iso8601ToTimestamp(likeDate);
        } else {
            post.timestamp = DateTimeUtils.iso8601ToTimestamp(post.published);
        }

        // if there's no featured thumbnail, check if featured media has been set - this is sometimes
        // a YouTube or Vimeo video, in which case store it as the featured video so we can treat
        // it as a video
        if (!post.hasFeaturedImage()) {
            JSONObject jsonMedia = json.optJSONObject("featured_media");
            if (jsonMedia != null) {
                String mediaUrl = JSONUtil.getString(jsonMedia, "uri");
                if (!TextUtils.isEmpty(mediaUrl)) {
                    String type = JSONUtil.getString(jsonMedia, "type");
                    boolean isVideo = (type != null && type.equals("video"));
                    if (isVideo) {
                        post.featuredVideo = mediaUrl;
                    } else {
                        post.featuredImage = mediaUrl;
                    }
                }
            }

            // if we still don't have a featured image, parse the content for an image that's
            // suitable as a featured image
            if (!post.hasFeaturedImage()) {
                ReaderImageScanner scanner = new ReaderImageScanner(post.text, post.isPrivate);
                post.featuredImage = scanner.getBestFeaturedImage();
            }
        }

        // if the post is untitled, make up a title from the excerpt
        if (!post.hasTitle() && post.hasExcerpt()) {
            post.title = extractTitle(post.excerpt, 50);
        }

        // remove html from title (rare, but does happen)
        if (post.hasTitle() && post.title.contains("<") && post.title.contains(">")) {
            post.title = HtmlUtils.stripHtml(post.title);
        }

        // parse the tags section
        assignTagsFromJson(post, json.optJSONObject("tags"));

        // parse the attachments
        JSONObject jsonAttachments = json.optJSONObject("attachments");
        if (jsonAttachments != null) {
            post.attachmentsJson = jsonAttachments.toString();
        }

        // the single-post sites/$site/posts/$post endpoint returns all site metadata
        // under meta/data/site (assuming ?meta=site was added to the request)
        JSONObject jsonSite = JSONUtil.getJSONChild(json, "meta/data/site");
        if (jsonSite != null) {
            post.blogId = jsonSite.optInt("ID");
            post.blogName = JSONUtil.getString(jsonSite, "name");
            post.setBlogUrl(JSONUtil.getString(jsonSite, "URL"));
            post.isPrivate = JSONUtil.getBool(jsonSite, "is_private");
        }

        return post;
    }

     /*
      * assigns author-related info to the passed post from the passed JSON "author" object
      */
    private static void assignAuthorFromJson(ReaderPost post, JSONObject jsonAuthor) {
        if (jsonAuthor == null) {
            return;
        }

        post.authorName = JSONUtil.getString(jsonAuthor, "name");
        post.postAvatar = JSONUtil.getString(jsonAuthor, "avatar_URL");
        post.authorId = jsonAuthor.optLong("ID");

        // site_URL doesn't exist for /sites/ endpoints, so get it from the author
        if (TextUtils.isEmpty(post.blogUrl)) {
            post.setBlogUrl(JSONUtil.getString(jsonAuthor, "URL"));
        }
    }

    /*
     * assigns primary/secondary tags to the passed post from the passed JSON "tags" object
     */
    private static void assignTagsFromJson(ReaderPost post, JSONObject jsonTags) {
        if (jsonTags == null) {
            return;
        }

        Iterator<String> it = jsonTags.keys();
        if (!it.hasNext()) {
            return;
        }

        // most popular tag & second most popular tag, based on usage count on this blog
        String mostPopularTag = null;
        String nextMostPopularTag = null;
        int popularCount = 0;

        while (it.hasNext()) {
            JSONObject jsonThisTag = jsonTags.optJSONObject(it.next());

            // if the number of posts on this blog that use this tag is higher than previous,
            // set this as the most popular tag, and set the second most popular tag to
            // the current most popular tag
            int postCount = jsonThisTag.optInt("post_count");
            if (postCount > popularCount) {
                nextMostPopularTag = mostPopularTag;
                mostPopularTag = JSONUtil.getString(jsonThisTag, "name");
                popularCount = postCount;
            }
        }

        // don't set primary tag if one is already set (may have been set from the editorial
        // section if this is a Freshly Pressed post)
        if (!post.hasPrimaryTag()) {
            post.setPrimaryTag(mostPopularTag);
        }
        post.setSecondaryTag(nextMostPopularTag);
    }

    /*
     * extracts a title from a post's excerpt - used when the post has no title
     */
    private static String extractTitle(final String excerpt, int maxLen) {
        if (TextUtils.isEmpty(excerpt))
            return null;

        if (excerpt.length() < maxLen)
            return excerpt.trim();

        StringBuilder result = new StringBuilder();
        BreakIterator wordIterator = BreakIterator.getWordInstance();
        wordIterator.setText(excerpt);
        int start = wordIterator.first();
        int end = wordIterator.next();
        int totalLen = 0;
        while (end != BreakIterator.DONE) {
            String word = excerpt.substring(start, end);
            result.append(word);
            totalLen += word.length();
            if (totalLen >= maxLen)
                break;
            start = end;
            end = wordIterator.next();
        }

        if (totalLen==0)
            return null;
        return result.toString().trim() + "...";
    }

    // --------------------------------------------------------------------------------------------

    public String getAuthorName() {
        return StringUtils.notNullStr(authorName);
    }
    public void setAuthorName(String authorName) {
        this.authorName = StringUtils.notNullStr(authorName);
    }

    public String getTitle() {
        return StringUtils.notNullStr(title);
    }
    public void setTitle(String title) {
        this.title = StringUtils.notNullStr(title);
    }

    public String getText() {
        return StringUtils.notNullStr(text);
    }
    public void setText(String text) {
        this.text = StringUtils.notNullStr(text);
    }

    public String getExcerpt() {
        return StringUtils.notNullStr(excerpt);
    }
    public void setExcerpt(String excerpt) {
        this.excerpt = StringUtils.notNullStr(excerpt);
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
    }

    public String getFeaturedImage() {
        return StringUtils.notNullStr(featuredImage);
    }
    public void setFeaturedImage(String featuredImage) {
        this.featuredImage = StringUtils.notNullStr(featuredImage);
    }

    public String getFeaturedVideo() {
        return StringUtils.notNullStr(featuredVideo);
    }
    public void setFeaturedVideo(String featuredVideo) {
        this.featuredVideo = StringUtils.notNullStr(featuredVideo);
    }

    public String getBlogName() {
        return StringUtils.notNullStr(blogName);
    }
    public void setBlogName(String blogName) {
        this.blogName = StringUtils.notNullStr(blogName);
    }

    public String getBlogUrl() {
        return StringUtils.notNullStr(blogUrl);
    }
    public void setBlogUrl(String blogUrl) {
        this.blogUrl = StringUtils.notNullStr(blogUrl);
    }

    public String getPostAvatar() {
        return StringUtils.notNullStr(postAvatar);
    }
    public void setPostAvatar(String postAvatar) {
        this.postAvatar = StringUtils.notNullStr(postAvatar);
    }

    public String getPseudoId() {
        return StringUtils.notNullStr(pseudoId);
    }
    public void setPseudoId(String pseudoId) {
        this.pseudoId = StringUtils.notNullStr(pseudoId);
    }

    public String getPublished() {
        return StringUtils.notNullStr(published);
    }
    public void setPublished(String published) {
        this.published = StringUtils.notNullStr(published);
    }

    public String getPrimaryTag() {
        return StringUtils.notNullStr(primaryTag);
    }
    public void setPrimaryTag(String tagName) {
        // this is a bit of a hack to avoid setting the primary tag to one of the default
        // tag names ("Freshly Pressed", etc.)
        if (!ReaderTag.isDefaultTagName(tagName)) {
            this.primaryTag = StringUtils.notNullStr(tagName);
        }
    }
    boolean hasPrimaryTag() {
        return !TextUtils.isEmpty(primaryTag);
    }

    public String getSecondaryTag() {
        return StringUtils.notNullStr(secondaryTag);
    }
    public void setSecondaryTag(String tagName) {
        if (!ReaderTag.isDefaultTagName(tagName)) {
            this.secondaryTag = StringUtils.notNullStr(tagName);
        }
    }

    /*
     * attachments are stored as the actual JSON to avoid having a separate table for
     * them, may need to revisit this if/when attachments become more important
     */
    public String getAttachmentsJson() {
        return StringUtils.notNullStr(attachmentsJson);
    }
    public void setAttachmentsJson(String json) {
        attachmentsJson = StringUtils.notNullStr(json);
    }


    public boolean hasText() {
        return !TextUtils.isEmpty(text);
    }

    public boolean hasExcerpt() {
        return !TextUtils.isEmpty(excerpt);
    }

    public boolean hasFeaturedImage() {
        return !TextUtils.isEmpty(featuredImage);
    }

    public boolean hasFeaturedVideo() {
        return !TextUtils.isEmpty(featuredVideo);
    }

    public boolean hasPostAvatar() {
        return !TextUtils.isEmpty(postAvatar);
    }

    public boolean hasBlogName() {
        return !TextUtils.isEmpty(blogName);
    }

    public boolean hasAuthorName() {
        return !TextUtils.isEmpty(authorName);
    }

    public boolean hasTitle() {
        return !TextUtils.isEmpty(title);
    }

    public boolean hasBlogUrl() {
        return !TextUtils.isEmpty(blogUrl);
    }

    /*
     * only public wp posts can be reblogged
     */
    public boolean canReblog() {
        return !isExternal && !isPrivate;
    }

    /*
     * returns true if this post is from a WordPress blog
     */
    public boolean isWP() {
        return !isExternal;
    }

    /****
     * the following are transient variables - not stored in the db or returned in the json - whose
     * sole purpose is to cache commonly-used values for the post that speeds up using them inside
     * adapters
     ****/

    /*
     * returns the featured image url as a photon url set to the passed width/height
     */
    private transient String featuredImageForDisplay;
    public String getFeaturedImageForDisplay(int width, int height) {
        if (featuredImageForDisplay == null) {
            if (!hasFeaturedImage()) {
                featuredImageForDisplay = "";
            } else {
                featuredImageForDisplay = ReaderUtils.getResizedImageUrl(featuredImage, width, height, isPrivate);
            }
        }
        return featuredImageForDisplay;
    }

    /*
     * returns the avatar url as a photon url set to the passed size
     */
    private transient String avatarForDisplay;
    public String getPostAvatarForDisplay(int avatarSize) {
        if (avatarForDisplay == null) {
            if (!hasPostAvatar()) {
                return "";
            }
            avatarForDisplay = PhotonUtils.fixAvatar(postAvatar, avatarSize);
        }
        return avatarForDisplay;
    }

    /*
     * converts iso8601 published date to an actual java date
     */
    private transient java.util.Date dtPublished;
    public java.util.Date getDatePublished() {
        if (dtPublished == null) {
            dtPublished = DateTimeUtils.iso8601ToJavaDate(published);
        }
        return dtPublished;
    }

    /*
     * determine which tag to display for this post
     *  - no tag if this is a private blog or there is no primary tag for this post
     *  - primary tag, unless it's the same as the currently selected tag
     *  - secondary tag if primary tag is the same as the currently selected tag
     */
    private transient String tagForDisplay;
    public String getTagForDisplay(final String currentTagName) {
        if (tagForDisplay == null) {
            if (!isPrivate && hasPrimaryTag()) {
                if (getPrimaryTag().equalsIgnoreCase(currentTagName)) {
                    tagForDisplay = getSecondaryTag();
                } else {
                    tagForDisplay = getPrimaryTag();
                }
            } else {
                tagForDisplay = "";
            }
        }
        return tagForDisplay;
    }

    /*
     * used when a unique numeric id is required by an adapter (when hasStableIds() = true)
     */
    private transient long stableId;
    public long getStableId() {
        if (stableId == 0) {
            stableId = (pseudoId != null ? pseudoId.hashCode() : 0);
        }
        return stableId;
    }

}