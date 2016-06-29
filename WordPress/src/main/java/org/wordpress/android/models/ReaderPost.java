package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.utils.ImageSizeMap;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

import java.text.BreakIterator;
import java.util.Iterator;

public class ReaderPost {
    private String pseudoId;
    public long postId;
    public long blogId;
    public long feedId;
    public long feedItemId;
    public long authorId;

    private String title;
    private String text;
    private String excerpt;
    private String authorName;
    private String authorFirstName;
    private String blogName;
    private String blogUrl;
    private String postAvatar;

    private String primaryTag;    // most popular tag on this post based on usage in blog
    private String secondaryTag;  // second most popular tag on this post based on usage in blog

    public double sortIndex;
    private String published;

    private String url;
    private String shortUrl;
    private String featuredImage;
    private String featuredVideo;

    public int numReplies;        // includes comments, trackbacks & pingbacks
    public int numLikes;
    public int wordCount;

    public boolean isLikedByCurrentUser;
    public boolean isFollowedByCurrentUser;
    public boolean isCommentsOpen;
    public boolean isExternal;
    public boolean isPrivate;
    public boolean isVideoPress;
    public boolean isJetpack;

    private String attachmentsJson;
    private String discoverJson;
    private String format;

    public long xpostPostId;
    public long xpostBlogId;

    public static ReaderPost fromJson(JSONObject json) {
        if (json == null) {
            throw new IllegalArgumentException("null json post");
        }

        ReaderPost post = new ReaderPost();

        post.postId = json.optLong("ID");
        post.blogId = json.optLong("site_ID");
        post.feedId = json.optLong("feed_ID");
        post.feedItemId = json.optLong("feed_item_ID");

        if (json.has("pseudo_ID")) {
            post.pseudoId = JSONUtils.getString(json, "pseudo_ID");  // read/ endpoint
        } else {
            post.pseudoId = JSONUtils.getString(json, "global_ID");  // sites/ endpoint
        }

        // remove HTML from the excerpt
        post.excerpt = HtmlUtils.fastStripHtml(JSONUtils.getString(json, "excerpt")).trim();

        post.text = JSONUtils.getString(json, "content");
        post.title = JSONUtils.getStringDecoded(json, "title");
        post.format = JSONUtils.getString(json, "format");
        post.url = JSONUtils.getString(json, "URL");
        post.shortUrl = JSONUtils.getString(json, "short_URL");
        post.setBlogUrl(JSONUtils.getString(json, "site_URL"));

        post.numLikes = json.optInt("like_count");
        post.wordCount = json.optInt("word_count");
        post.isLikedByCurrentUser = JSONUtils.getBool(json, "i_like");
        post.isFollowedByCurrentUser = JSONUtils.getBool(json, "is_following");
        post.isExternal = JSONUtils.getBool(json, "is_external");
        post.isPrivate = JSONUtils.getBool(json, "site_is_private");
        post.isJetpack = JSONUtils.getBool(json, "is_jetpack");

        JSONObject jsonDiscussion = json.optJSONObject("discussion");
        if (jsonDiscussion != null) {
            post.isCommentsOpen = JSONUtils.getBool(jsonDiscussion, "comments_open");
            post.numReplies = jsonDiscussion.optInt("comment_count");
        } else {
            post.isCommentsOpen = JSONUtils.getBool(json, "comments_open");
            post.numReplies = json.optInt("comment_count");
        }

        // parse the author section
        assignAuthorFromJson(post, json.optJSONObject("author"));

        post.featuredImage = JSONUtils.getString(json, "featured_image");
        post.blogName = JSONUtils.getStringDecoded(json, "site_name");
        post.published = JSONUtils.getString(json, "date");

        // sort index determines how posts are sorted - this is a "score" for search results,
        // liked date for liked posts, and published date for all others
        if (json.has("score")) {
            post.sortIndex = json.optDouble("score");
        } else if (json.has("date_liked")) {
            String likeDate = JSONUtils.getString(json, "date_liked");
            post.sortIndex = DateTimeUtils.iso8601ToTimestamp(likeDate);
        } else {
            post.sortIndex = DateTimeUtils.iso8601ToTimestamp(post.published);
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
        if (jsonAttachments != null && jsonAttachments.length() > 0) {
            post.attachmentsJson = jsonAttachments.toString();
        }

        // site metadata - returned when ?meta=site was added to the request
        JSONObject jsonSite = JSONUtils.getJSONChild(json, "meta/data/site");
        if (jsonSite != null) {
            post.blogId = jsonSite.optInt("ID");
            post.blogName = JSONUtils.getString(jsonSite, "name");
            post.setBlogUrl(JSONUtils.getString(jsonSite, "URL"));
            post.isPrivate = JSONUtils.getBool(jsonSite, "is_private");
            // TODO: as of 29-Sept-2014, this is broken - endpoint returns false when it should be true
            post.isJetpack = JSONUtils.getBool(jsonSite, "jetpack");
        }

        // "discover" posts
        JSONObject jsonDiscover = json.optJSONObject("discover_metadata");
        if (jsonDiscover != null) {
            post.setDiscoverJson(jsonDiscover.toString());
        }

        // xpost info
        assignXpostIdsFromJson(post, json.optJSONArray("metadata"));

        // if there's no featured image, check if featured media has been set - this is sometimes
        // a YouTube or Vimeo video, in which case store it as the featured video so we can treat
        // it as a video
        if (!post.hasFeaturedImage()) {
            JSONObject jsonMedia = json.optJSONObject("featured_media");
            if (jsonMedia != null && jsonMedia.length() > 0) {
                String mediaUrl = JSONUtils.getString(jsonMedia, "uri");
                if (!TextUtils.isEmpty(mediaUrl)) {
                    String type = JSONUtils.getString(jsonMedia, "type");
                    boolean isVideo = (type != null && type.equals("video"));
                    if (isVideo) {
                        post.featuredVideo = mediaUrl;
                    } else {
                        post.featuredImage = mediaUrl;
                    }
                }
            }
        }
        // if the post still doesn't have a featured image but we have attachment data, check whether
        // we can find a suitable featured image from the attachments
        if (!post.hasFeaturedImage() && post.hasAttachments()) {
            post.featuredImage = new ImageSizeMap(post.attachmentsJson)
                    .getLargestImageUrl(ReaderConstants.MIN_FEATURED_IMAGE_WIDTH);
        }
        // if we *still* don't have a featured image but the text contains an IMG tag, check whether
        // we can find a suitable image from the text
        if (!post.hasFeaturedImage() && post.hasText() && post.text.contains("<img")) {
            post.featuredImage = new ReaderImageScanner(post.text, post.isPrivate)
                    .getLargestImage(ReaderConstants.MIN_FEATURED_IMAGE_WIDTH);
        }

        return post;
    }

    /*
     * assigns cross post blog & post IDs from post's metadata section
     *  "metadata": [
     *       {
     *           "id": "21192",
     *           "key": "xpost_origin",
     *           "value": "11326809:18427"
     *       }
     *     ],
     */
    private static void assignXpostIdsFromJson(ReaderPost post, JSONArray jsonMetadata) {
        if (jsonMetadata ==  null) return;

        for (int i = 0; i < jsonMetadata.length(); i++) {
            JSONObject jsonMetaItem = jsonMetadata.optJSONObject(i);
            String metaKey = jsonMetaItem.optString("key");
            if (!TextUtils.isEmpty(metaKey) && metaKey.equals("xpost_origin")) {
                String value = jsonMetaItem.optString("value");
                if (!TextUtils.isEmpty(value) && value.contains(":")) {
                    String[] valuePair = value.split(":");
                    if (valuePair.length == 2) {
                        post.xpostBlogId = StringUtils.stringToLong(valuePair[0]);
                        post.xpostPostId = StringUtils.stringToLong(valuePair[1]);
                        return;
                    }
                }
            }
        }
    }

     /*
      * assigns author-related info to the passed post from the passed JSON "author" object
      */
    private static void assignAuthorFromJson(ReaderPost post, JSONObject jsonAuthor) {
        if (jsonAuthor == null) return;

        post.authorName = JSONUtils.getStringDecoded(jsonAuthor, "name");
        post.authorFirstName = JSONUtils.getStringDecoded(jsonAuthor, "first_name");
        post.postAvatar = JSONUtils.getString(jsonAuthor, "avatar_URL");
        post.authorId = jsonAuthor.optLong("ID");

        // site_URL doesn't exist for /sites/ endpoints, so get it from the author
        if (TextUtils.isEmpty(post.blogUrl)) {
            post.setBlogUrl(JSONUtils.getString(jsonAuthor, "URL"));
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
                mostPopularTag = JSONUtils.getStringDecoded(jsonThisTag, "slug");
                popularCount = postCount;
            }
        }

        // don't set primary tag if one is already set
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
    public void setAuthorName(String name) {
        this.authorName = StringUtils.notNullStr(name);
    }

    public String getAuthorFirstName() {
        return StringUtils.notNullStr(authorFirstName);
    }
    public void setAuthorFirstName(String name) {
        this.authorFirstName = StringUtils.notNullStr(name);
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

    // https://codex.wordpress.org/Post_Formats
    public String getFormat() {
        return StringUtils.notNullStr(format);
    }
    public void setFormat(String format) {
        this.format = StringUtils.notNullStr(format);
    }

    public boolean isGallery() {
        return format != null && format.equals("gallery");
    }


    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
    }

    public String getShortUrl() {
        return StringUtils.notNullStr(shortUrl);
    }
    public void setShortUrl(String url) {
        this.shortUrl = StringUtils.notNullStr(url);
    }
    public boolean hasShortUrl() {
        return !TextUtils.isEmpty(shortUrl);
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
        // this is a bit of a hack to avoid setting the primary tag to one of the defaults
        if (!ReaderTag.isDefaultTagTitle(tagName)) {
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
        if (!ReaderTag.isDefaultTagTitle(tagName)) {
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
    public boolean hasAttachments() {
        return !TextUtils.isEmpty(attachmentsJson);
    }

    /*
     * "discover" posts also store the actual JSON
     */
    public String getDiscoverJson() {
        return StringUtils.notNullStr(discoverJson);
    }
    public void setDiscoverJson(String json) {
        discoverJson = StringUtils.notNullStr(json);
    }
    public boolean isDiscoverPost() {
        return !TextUtils.isEmpty(discoverJson);
    }

    private transient ReaderPostDiscoverData discoverData;
    public ReaderPostDiscoverData getDiscoverData() {
        if (discoverData == null && !TextUtils.isEmpty(discoverJson)) {
            try {
                discoverData = new ReaderPostDiscoverData(new JSONObject(discoverJson));
            } catch (JSONException e) {
                return null;
            }
        }
        return discoverData;
    }

    public boolean hasText() {
        return !TextUtils.isEmpty(text);
    }

    public boolean hasUrl() {
        return !TextUtils.isEmpty(url);
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

    public boolean hasAuthorFirstName() {
        return !TextUtils.isEmpty(authorFirstName);
    }

    public boolean hasTitle() {
        return !TextUtils.isEmpty(title);
    }

    public boolean hasBlogUrl() {
        return !TextUtils.isEmpty(blogUrl);
    }

    /*
     * returns true if this post is from a WordPress blog
     */
    public boolean isWP() {
        return !isExternal;
    }

    /*
     * returns true if this is a cross-post
     */
    public boolean isXpost() {
        return xpostBlogId != 0 && xpostPostId != 0;
    }

    /*
     * returns true if the passed post appears to be the same as this one - used when posts are
     * retrieved to determine which ones are new/changed/unchanged
     */
    public boolean isSamePost(ReaderPost post) {
        return post != null
                && post.blogId == this.blogId
                && post.postId == this.postId
                && post.feedId == this.feedId
                && post.feedItemId == this.feedItemId
                && post.numLikes == this.numLikes
                && post.numReplies == this.numReplies
                && post.isFollowedByCurrentUser == this.isFollowedByCurrentUser
                && post.isLikedByCurrentUser == this.isLikedByCurrentUser
                && post.isCommentsOpen == this.isCommentsOpen
                && post.getTitle().equals(this.getTitle())
                && post.getExcerpt().equals(this.getExcerpt())
                && post.getText().equals(this.getText());
    }

    public boolean hasIds(ReaderBlogIdPostId ids) {
        return ids != null
                && ids.getBlogId() == this.blogId
                && ids.getPostId() == this.postId;
    }

    /*
     * liking is enabled for all wp.com and jp posts with the exception of discover posts
     */
    public boolean canLikePost() {
        return (isWP() || isJetpack) && (!isDiscoverPost());
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
    public String getPostAvatarForDisplay(int size) {
        if (avatarForDisplay == null) {
            if (!hasPostAvatar()) {
                return "";
            }
            avatarForDisplay = GravatarUtils.fixGravatarUrl(postAvatar, size);
        }
        return avatarForDisplay;
    }

    /*
     * returns the blog's blavatar url as a photon url set to the passed size
     */
    private transient String blavatarForDisplay;
    public String getPostBlavatarForDisplay(int size) {
        if (blavatarForDisplay == null) {
            if (!hasBlogUrl()) {
                return "";
            }
            blavatarForDisplay = GravatarUtils.blavatarFromUrl(getBlogUrl(), size);
        }
        return blavatarForDisplay;
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
