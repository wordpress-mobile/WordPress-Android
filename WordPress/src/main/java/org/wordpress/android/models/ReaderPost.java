package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.utils.ReaderIframeScanner;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

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
    private String blogImageUrl;
    private String postAvatar;

    private String primaryTag;    // most popular tag on this post based on usage in blog
    private String secondaryTag;  // second most popular tag on this post based on usage in blog

    private String dateLiked;
    private String dateTagged;
    private String datePublished;
    public double score;

    private String url;
    private String shortUrl;
    private String featuredImage;
    private String featuredVideo;

    public int numReplies;        // includes comments, trackbacks & pingbacks
    public int numLikes;

    public boolean likedByCurrentUserEh;
    public boolean followedByCurrentUserEh;
    public boolean commentsOpenEh;
    public boolean externalEh;
    public boolean privateEh;
    public boolean videoPressEh;
    public boolean jetpackEh;

    private String attachmentsJson;
    private String discoverJson;
    private String format;

    public long xpostPostId;
    public long xpostBlogId;

    private String railcarJson;
    private ReaderCardType cardType = ReaderCardType.DEFAULT;

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
        post.likedByCurrentUserEh = JSONUtils.getBool(json, "i_like");
        post.followedByCurrentUserEh = JSONUtils.getBool(json, "is_following");
        post.externalEh = JSONUtils.getBool(json, "is_external");
        post.privateEh = JSONUtils.getBool(json, "site_is_private");
        post.jetpackEh = JSONUtils.getBool(json, "is_jetpack");

        JSONObject jsonDiscussion = json.optJSONObject("discussion");
        if (jsonDiscussion != null) {
            post.commentsOpenEh = JSONUtils.getBool(jsonDiscussion, "comments_open");
            post.numReplies = jsonDiscussion.optInt("comment_count");
        } else {
            post.commentsOpenEh = JSONUtils.getBool(json, "comments_open");
            post.numReplies = json.optInt("comment_count");
        }

        // parse the author section
        assignAuthorFromJson(post, json.optJSONObject("author"));

        post.featuredImage = JSONUtils.getString(json, "featured_image");
        post.blogName = JSONUtils.getStringDecoded(json, "site_name");

        post.datePublished = JSONUtils.getString(json, "date");
        post.dateLiked = JSONUtils.getString(json, "date_liked");
        post.dateTagged = JSONUtils.getString(json, "tagged_on");

        // "score" only exists for search results
        post.score = json.optDouble("score");

        // if the post is untitled, make up a title from the excerpt
        if (!post.titleEh() && post.excerptEh()) {
            post.title = extractTitle(post.excerpt, 50);
        }

        // remove html from title (rare, but does happen)
        if (post.titleEh() && post.title.contains("<") && post.title.contains(">")) {
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
            post.privateEh = JSONUtils.getBool(jsonSite, "is_private");
            JSONObject jsonSiteIcon = jsonSite.optJSONObject("icon");
            if (jsonSiteIcon != null) {
                post.blogImageUrl = JSONUtils.getString(jsonSiteIcon, "img");
            }
            // TODO: as of 29-Sept-2014, this is broken - endpoint returns false when it should be true
            post.jetpackEh = JSONUtils.getBool(jsonSite, "jetpack");
        }

        // "discover" posts
        JSONObject jsonDiscover = json.optJSONObject("discover_metadata");
        if (jsonDiscover != null) {
            post.setDiscoverJson(jsonDiscover.toString());
        }

        // xpost info
        assignXpostIdsFromJson(post, json.optJSONArray("metadata"));

        // if the post doesn't have a featured image but it contains an IMG tag, check whether
        // we can find a suitable image from the content
        if (!post.featuredImageEh() && post.imagesEh()) {
            post.featuredImage = new ReaderImageScanner(post.text, post.privateEh)
                    .getLargestImage(ReaderConstants.MIN_FEATURED_IMAGE_WIDTH);
        }

        // if there's no featured image or featured video and the post contains an iframe, scan
        // the content for a suitable featured video
        if (!post.featuredImageEh()
                && !post.featuredVideoEh()
                && post.getText().contains("<iframe")) {
            post.setFeaturedVideo(new ReaderIframeScanner(post.getText()).getFirstUsableVideo());
        }

        // "railcar" data - currently used in search streams, used by TrainTracks
        JSONObject jsonRailcar = json.optJSONObject("railcar");
        if (jsonRailcar != null) {
            post.setRailcarJson(jsonRailcar.toString());
        }

        // set the card type last since it depends on information contained in the post - note
        // that this is stored in the post table rather than calculated on-the-fly
        post.setCardType(ReaderCardType.fromReaderPost(post));

        return post;
    }

    public boolean imagesEh() {
        return textEh() && text.contains("<img ");
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
        post.authorId = jsonAuthor.optLong("ID");

        // v1.2 endpoint contains a "has_avatar" boolean which tells us whether the author
        // has a valid avatar - if this field exists and is set to false, skip setting
        // the avatar URL
        boolean avatarEh;
        if (jsonAuthor.has("has_avatar")) {
            avatarEh = jsonAuthor.optBoolean("has_avatar");
        } else {
            avatarEh = true;
        }
        if (avatarEh) {
            post.postAvatar = JSONUtils.getString(jsonAuthor, "avatar_URL");
        }

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
            String thisTagName = UrlUtils.urlDecode(JSONUtils.getString(jsonThisTag, "slug"));

            // if the number of posts on this blog that use this tag is higher than previous,
            // set this as the most popular tag, and set the second most popular tag to
            // the current most popular tag
            int postCount = jsonThisTag.optInt("post_count");
            if (postCount > popularCount) {
                nextMostPopularTag = mostPopularTag;
                mostPopularTag = thisTagName;
                popularCount = postCount;
            } else if (nextMostPopularTag == null) {
                nextMostPopularTag = thisTagName;
            }
        }

        // don't set primary tag if one is already set
        if (!post.primaryTagEh()) {
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

    public boolean galleryEh() {
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
    public boolean shortUrlEh() {
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

    public String getBlogImageUrl() {
        return StringUtils.notNullStr(blogImageUrl);
    }
    public void setBlogImageUrl(String imageUrl) {
        this.blogImageUrl = StringUtils.notNullStr(imageUrl);
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

    public String getDatePublished() {
        return StringUtils.notNullStr(datePublished);
    }
    public void setDatePublished(String dateStr) {
        this.datePublished = StringUtils.notNullStr(dateStr);
    }

    public String getDateLiked() {
        return StringUtils.notNullStr(dateLiked);
    }
    public void setDateLiked(String dateStr) {
        this.dateLiked = StringUtils.notNullStr(dateStr);
    }

    public String getDateTagged() {
        return StringUtils.notNullStr(dateTagged);
    }
    public void setDateTagged(String dateStr) {
        this.dateTagged = StringUtils.notNullStr(dateStr);
    }

    public String getPrimaryTag() {
        return StringUtils.notNullStr(primaryTag);
    }
    public void setPrimaryTag(String tagName) {
        // this is a bit of a hack to avoid setting the primary tag to one of the defaults
        if (!ReaderTag.defaultTagTitleEh(tagName)) {
            this.primaryTag = StringUtils.notNullStr(tagName);
        }
    }
    public boolean primaryTagEh() {
        return !TextUtils.isEmpty(primaryTag);
    }

    public String getSecondaryTag() {
        return StringUtils.notNullStr(secondaryTag);
    }
    public void setSecondaryTag(String tagName) {
        if (!ReaderTag.defaultTagTitleEh(tagName)) {
            this.secondaryTag = StringUtils.notNullStr(tagName);
        }
    }
    public boolean secondaryTagEh() {
        return !TextUtils.isEmpty(secondaryTag);
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
    public boolean attachmentsEh() {
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
    public boolean discoverPostEh() {
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

    public boolean textEh() {
        return !TextUtils.isEmpty(text);
    }

    public boolean urlEh() {
        return !TextUtils.isEmpty(url);
    }

    public boolean excerptEh() {
        return !TextUtils.isEmpty(excerpt);
    }

    public boolean featuredImageEh() {
        return !TextUtils.isEmpty(featuredImage);
    }

    public boolean featuredVideoEh() {
        return !TextUtils.isEmpty(featuredVideo);
    }

    public boolean postAvatarEh() {
        return !TextUtils.isEmpty(postAvatar);
    }

    public boolean blogNameEh() {
        return !TextUtils.isEmpty(blogName);
    }

    public boolean authorNameEh() {
        return !TextUtils.isEmpty(authorName);
    }

    public boolean authorFirstNameEh() {
        return !TextUtils.isEmpty(authorFirstName);
    }

    public boolean titleEh() {
        return !TextUtils.isEmpty(title);
    }

    public boolean blogUrlEh() {
        return !TextUtils.isEmpty(blogUrl);
    }

    public boolean blogImageUrlEh() {
        return !TextUtils.isEmpty(blogImageUrl);
    }

    /*
     * returns true if this post is from a WordPress blog
     */
    public boolean wPEh() {
        return !externalEh;
    }

    /*
     * returns true if this is a cross-post
     */
    public boolean xpostEh() {
        return xpostBlogId != 0 && xpostPostId != 0;
    }

    /*
     * returns true if the passed post appears to be the same as this one - used when posts are
     * retrieved to determine which ones are new/changed/unchanged
     */
    public boolean samePostEh(ReaderPost post) {
        return post != null
                && post.blogId == this.blogId
                && post.postId == this.postId
                && post.feedId == this.feedId
                && post.feedItemId == this.feedItemId
                && post.numLikes == this.numLikes
                && post.numReplies == this.numReplies
                && post.followedByCurrentUserEh == this.isFollowedByCurrentUser
                && post.likedByCurrentUserEh == this.isLikedByCurrentUser
                && post.commentsOpenEh == this.isCommentsOpen
                && post.getTitle().equals(this.getTitle())
                && post.getExcerpt().equals(this.getExcerpt())
                && post.getText().equals(this.getText());
    }

    public boolean idsEh(ReaderBlogIdPostId ids) {
        return ids != null
                && ids.getBlogId() == this.blogId
                && ids.getPostId() == this.postId;
    }

    /*
     * liking is enabled for all wp.com and jp posts with the exception of discover posts
     */
    public boolean canLikePost() {
        return (wPEh() || jetpackEh) && (!discoverPostEh());
    }


    public String getRailcarJson() {
        return StringUtils.notNullStr(railcarJson);
    }
    public void setRailcarJson(String jsonRailcar) {
        this.railcarJson = StringUtils.notNullStr(jsonRailcar);
    }
    public boolean railcarEh() {
        return !TextUtils.isEmpty(railcarJson);
    }

    public ReaderCardType getCardType() {
        return cardType != null ? cardType : ReaderCardType.DEFAULT;
    }
    public void setCardType(ReaderCardType cardType) {
        this.cardType = cardType;
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
            if (!featuredImageEh()) {
                featuredImageForDisplay = "";
            } else {
                featuredImageForDisplay = ReaderUtils.getResizedImageUrl(featuredImage, width, height, privateEh);
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
            if (!postAvatarEh()) {
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
            if (!blogUrlEh()) {
                return "";
            }
            blavatarForDisplay = GravatarUtils.blavatarFromUrl(getBlogUrl(), size);
        }
        return blavatarForDisplay;
    }

    /*
     * converts iso8601 pubDate to a java date for display - this is the date that appears on posts
     */
    private transient java.util.Date dtDisplay;
    public java.util.Date getDisplayDate() {
        if (dtDisplay == null) {
            dtDisplay = DateTimeUtils.dateFromIso8601(this.datePublished);
        }
        return dtDisplay;
    }

    /*
     * used when a unique numeric id is required by an adapter (when stableIdsEh() = true)
     */
    private transient long stableId;
    public long getStableId() {
        if (stableId == 0) {
            stableId = (pseudoId != null ? pseudoId.hashCode() : 0);
        }
        return stableId;
    }

}
