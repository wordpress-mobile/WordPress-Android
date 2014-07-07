package org.wordpress.android.models;

import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.ui.reader.ReaderUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

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

    private String tags;          // comma-separated list of tags
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
    public boolean isSharingEnabled;

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
            post.featuredImage = getImageUrlFromFeaturedImageUrl(JSONUtil.getString(jsonEditorial, "image"));
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
            // suitable as a featured image - this is done since featured_media seems to miss
            // some images that would work well as featured images on mobile
            if (!post.hasFeaturedImage()) {
                post.featuredImage = findFeaturedImage(post.text);
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
     * assigns tag-related info to the passed post from the passed JSON "tags" object
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

        StringBuilder sbAllTags = new StringBuilder();
        boolean isFirst = true;

        while (it.hasNext()) {
            JSONObject jsonThisTag = jsonTags.optJSONObject(it.next());
            String tagName = JSONUtil.getString(jsonThisTag, "name");

            // if the number of posts on this blog that use this tag is higher than previous,
            // set this as the most popular tag, and set the second most popular tag to
            // the current most popular tag
            int postCount = jsonThisTag.optInt("post_count");
            if (postCount > popularCount) {
                nextMostPopularTag = mostPopularTag;
                mostPopularTag = tagName;
                popularCount = postCount;
            }

            // add to list of all tags
            if (isFirst) {
                isFirst = false;
            } else {
                sbAllTags.append(",");
            }
            sbAllTags.append(tagName);
        }

        // don't set primary tag if one is already set (may have been set from the editorial
        // section if this is a Freshly Pressed post)
        if (!post.hasPrimaryTag()) {
            post.setPrimaryTag(mostPopularTag);
        }
        post.setSecondaryTag(nextMostPopularTag);

        post.setTags(sbAllTags.toString());
    }

    /*
     * extracts a title from a post's excerpt
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

    /*
     * called when a post doesn't have a featured image, searches post's content for an image that
     * may still be suitable as a featured image - only works with WP posts due to the search for
     * specific WP image classes (but will also work with RSS posts that come from WP blogs)
     */
    private static String findFeaturedImage(final String text) {
        if (text==null || !text.contains("<img "))
            return null;

        final String className;
        if (text.contains("size-full")) {
            className = "size-full";
        } else if (text.contains("size-large")) {
            className = "size-large";
        } else if (text.contains("size-medium")) {
            className = "size-medium";
        } else {
            return null;
        }

        // determine whether attributes are single- or double- quoted
        boolean usesSingleQuotes = text.contains("src='");

        int imgStart = text.indexOf("<img ");
        while (imgStart > -1) {
            int imgEnd = text.indexOf(">", imgStart);
            if (imgEnd == -1)
                return null;

            String img = text.substring(imgStart, imgEnd+1);
            if (img.contains(className)) {
                int srcStart = img.indexOf(usesSingleQuotes ? "src='" : "src=\"");
                if (srcStart == -1)
                    return null;
                int srcEnd = img.indexOf(usesSingleQuotes ? "'" : "\"", srcStart+5);
                if (srcEnd == -1)
                    return null;
                return img.substring(srcStart+5, srcEnd);
            }

            imgStart = text.indexOf("<img ", imgEnd);
        }

        // if we get this far, no suitable image was found
        return null;
    }

    /*
        returns the actual image url from a Freshly Pressed featured image url - this is necessary because the
        featured image returned by the API is often an ImagePress url that formats the actual image url for a
        specific size, and we want to define the size in the app when the image is requested.
        here's an example of an ImagePress featured image url from a freshly-pressed post:
        https://s1.wp.com/imgpress?crop=0px%2C0px%2C252px%2C160px&url=https%3A%2F%2Fs2.wp.com%2Fimgpress%3Fw%3D252%26url%3Dhttp%253A%252F%252Fmostlybrightideas.files.wordpress.com%252F2013%252F08%252Ftablet.png&unsharpmask=80,0.5,3
     */
    private static String getImageUrlFromFeaturedImageUrl(final String featuredImageUrl) {
        if (TextUtils.isEmpty(featuredImageUrl))
            return null;

        // if this is an mshots image, return the actual url without the query string (?h=n&w=n),
        // and change it from https: to http: so it can be cached (it's only https because it's
        // being returned by an authenticated REST endpoint - these images are found only in
        // FP posts so they don't require https)
        if (PhotonUtils.isMshotsUrl(featuredImageUrl))
            return UrlUtils.removeQuery(featuredImageUrl).replaceFirst("https", "http");

        if (featuredImageUrl.contains("imgpress")) {
            // parse the url parameter
            String actualImageUrl = Uri.parse(featuredImageUrl).getQueryParameter("url");
            if (actualImageUrl==null)
                return featuredImageUrl;

            // at this point the imageUrl may still be an ImagePress url, so check the url param again (see above example)
            if (actualImageUrl.contains("url=")) {
                return Uri.parse(actualImageUrl).getQueryParameter("url");
            } else {
                return actualImageUrl;
            }
        }

        // for all other featured images, return the passed url w/o the query string (since the query string
        // often contains Photon sizing params that we don't want here)
        int pos = featuredImageUrl.lastIndexOf("?");
        if (pos == -1)
            return featuredImageUrl;

        return featuredImageUrl.substring(0, pos);
    }

    /*
     * This is necessary to get VideoPress videos to work in the Reader since the v1
     * REST API returns VideoPress videos in a script block that relies on jQuery - which obviously
     * fails on mobile - here we extract the video thumbnail and insert a DIV at the top of the
     * post content which links the thumbnail IMG to the video so the user can tap the thumb to
     * play the video
     * iOS: https://github.com/wordpress-mobile/WordPress-iOS/blob/develop/WordPress/Classes/ReaderPost.m#L702
     */
    /*private static void cleanupVideoPress(ReaderPost post) {
        if (post==null || !post.hasText() || !post.hasFeaturedVideo())
            return;

        // extract the video thumbnail from them "videopress-poster" image class
        String text = post.getText();
        int pos = text.indexOf("videopress-poster");
        if (pos == -1)
            return;
        int srcStart = text.indexOf("src=\"", pos);
        if (srcStart == -1)
            return;
        srcStart += 5;
        int srcEnd = text.indexOf("\"", srcStart);
        if (srcEnd == -1)
            return;

        // set the featured image to the thumbnail if a featured image isn't already assigned
        String thumb = text.substring(srcStart, srcEnd);
        if (!post.hasFeaturedImage())
            post.featuredImage = thumb;

        // add the thumbnail linked to the actual video to the top of the content
        String videoLink = String.format("<div><a href='%s'><img src='%s'/></a></div>", post.getFeaturedVideo(), thumb);
        post.text = videoLink + text;
    }*/

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

    // --------------------------------------------------------------------------------------------

    /*
     * comma-separated tags
     */
    public String getTags() {
        return StringUtils.notNullStr(tags);
    }
    public void setTags(String tags) {
        this.tags = StringUtils.notNullStr(tags);
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
    public boolean hasPrimaryTag() {
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

    // --------------------------------------------------------------------------------------------

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
            } else if (isPrivate) {
                // images in private posts can't use photon, so handle separately
                featuredImageForDisplay = ReaderUtils.getPrivateImageForDisplay(featuredImage, width, height);
            } else {
                // not private, so set to correctly sized photon url
                featuredImageForDisplay = PhotonUtils.getPhotonImageUrl(featuredImage, width, height);
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