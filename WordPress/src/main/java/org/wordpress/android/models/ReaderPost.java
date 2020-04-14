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
    private String mPseudoId;
    public long postId;
    public long blogId;
    public long feedId;
    public long feedItemId;
    public long authorId;

    private String mTitle;
    private String mText;
    private String mExcerpt;
    private String mAuthorName;
    private String mAuthorFirstName;
    private String mBlogName;
    private String mBlogUrl;
    private String mBlogImageUrl;
    private String mPostAvatar;

    private String mPrimaryTag; // most popular tag on this post based on usage in blog
    private String mSecondaryTag; // second most popular tag on this post based on usage in blog

    private String mDateLiked;
    private String mDateTagged;
    private String mDatePublished;
    public double score;

    private String mUrl;
    private String mShortUrl;
    private String mFeaturedImage;
    private String mFeaturedVideo;

    public int numReplies; // includes comments, trackbacks & pingbacks
    public int numLikes;

    public boolean isLikedByCurrentUser;
    public boolean isFollowedByCurrentUser;
    public boolean isBookmarked;
    public boolean isCommentsOpen;
    public boolean isExternal;
    public boolean isPrivate;
    public boolean isPrivateAtomic;
    public boolean isVideoPress;
    public boolean isJetpack;
    public boolean useExcerpt;

    private String mAttachmentsJson;
    private String mDiscoverJson;
    private String mFormat;

    public long xpostPostId;
    public long xpostBlogId;

    private String mRailcarJson;
    private ReaderCardType mCardType = ReaderCardType.DEFAULT;

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
            post.mPseudoId = JSONUtils.getString(json, "pseudo_ID"); // read/ endpoint
        } else {
            post.mPseudoId = JSONUtils.getString(json, "global_ID"); // sites/ endpoint
        }

        // remove HTML from the excerpt
        post.mExcerpt = HtmlUtils.fastStripHtml(JSONUtils.getString(json, "excerpt")).trim();

        post.mText = JSONUtils.getString(json, "content");
        post.mTitle = JSONUtils.getStringDecoded(json, "title");
        post.mFormat = JSONUtils.getString(json, "format");
        post.mUrl = JSONUtils.getString(json, "URL");
        post.mShortUrl = JSONUtils.getString(json, "short_URL");
        post.setBlogUrl(JSONUtils.getString(json, "site_URL"));

        post.numLikes = json.optInt("like_count");
        post.isLikedByCurrentUser = JSONUtils.getBool(json, "i_like");
        post.isFollowedByCurrentUser = JSONUtils.getBool(json, "is_following");
        post.isExternal = JSONUtils.getBool(json, "is_external");
        post.isPrivate = JSONUtils.getBool(json, "site_is_private");
        post.isPrivateAtomic = JSONUtils.getBool(json, "site_is_atomic");
        post.isJetpack = JSONUtils.getBool(json, "is_jetpack");
        post.useExcerpt = JSONUtils.getBool(json, "use_excerpt");

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

        post.mFeaturedImage = JSONUtils.getString(json, "featured_image");
        post.mBlogName = JSONUtils.getStringDecoded(json, "site_name");

        post.mDatePublished = JSONUtils.getString(json, "date");
        post.mDateLiked = JSONUtils.getString(json, "date_liked");
        post.mDateTagged = JSONUtils.getString(json, "tagged_on");

        // "score" only exists for search results
        post.score = json.optDouble("score");

        // if the post is untitled, make up a title from the excerpt
        if (!post.hasTitle() && post.hasExcerpt()) {
            post.mTitle = extractTitle(post.mExcerpt, 50);
        }

        // remove html from title (rare, but does happen)
        if (post.hasTitle() && post.mTitle.contains("<") && post.mTitle.contains(">")) {
            post.mTitle = HtmlUtils.stripHtml(post.mTitle);
        }

        // parse the tags section
        assignTagsFromJson(post, json.optJSONObject("tags"));

        // parse the attachments
        JSONObject jsonAttachments = json.optJSONObject("attachments");
        if (jsonAttachments != null && jsonAttachments.length() > 0) {
            post.mAttachmentsJson = jsonAttachments.toString();
        }

        // site metadata - returned when ?meta=site was added to the request
        JSONObject jsonSite = JSONUtils.getJSONChild(json, "meta/data/site");
        if (jsonSite != null) {
            post.blogId = jsonSite.optInt("ID");
            post.mBlogName = JSONUtils.getString(jsonSite, "name");
            post.setBlogUrl(JSONUtils.getString(jsonSite, "URL"));
            post.isPrivate = JSONUtils.getBool(jsonSite, "is_private");
            JSONObject jsonSiteIcon = jsonSite.optJSONObject("icon");
            if (jsonSiteIcon != null) {
                post.mBlogImageUrl = JSONUtils.getString(jsonSiteIcon, "img");
            }
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

        // if there's no featured image, check if featured media has been set to an image
        if (!post.hasFeaturedImage() && json.has("featured_media")) {
            JSONObject jsonMedia = json.optJSONObject("featured_media");
            String type = JSONUtils.getString(jsonMedia, "type");
            if (type.equals("image")) {
                post.mFeaturedImage = JSONUtils.getString(jsonMedia, "uri");
            }
        }

        // if the post doesn't have a featured image but it contains an IMG tag, check whether
        // we can find a suitable image from the content
        if (!post.hasFeaturedImage() && post.hasImages()) {
            post.mFeaturedImage = new ReaderImageScanner(post.mText, post.isPrivate)
                    .getLargestImage(ReaderConstants.MIN_FEATURED_IMAGE_WIDTH);
        }

        // if there's no featured image or featured video and the post contains an iframe, scan
        // the content for a suitable featured video
        if (!post.hasFeaturedImage()
            && !post.hasFeaturedVideo()
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

    public boolean hasImages() {
        return hasText() && mText.contains("<img ");
    }

    /*
     * assigns cross post blog & post IDs from post's metadata section
     * "metadata": [
     * {
     * "id": "21192",
     * "key": "xpost_origin",
     * "value": "11326809:18427"
     * }
     * ],
     */
    private static void assignXpostIdsFromJson(ReaderPost post, JSONArray jsonMetadata) {
        if (jsonMetadata == null) {
            return;
        }

        for (int i = 0; i < jsonMetadata.length(); i++) {
            JSONObject jsonMetaItem = jsonMetadata.optJSONObject(i);
            if (jsonMetaItem != null) {
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
    }

    /*
     * assigns author-related info to the passed post from the passed JSON "author" object
     */
    private static void assignAuthorFromJson(ReaderPost post, JSONObject jsonAuthor) {
        if (jsonAuthor == null) {
            return;
        }

        post.mAuthorName = JSONUtils.getStringDecoded(jsonAuthor, "name");
        post.mAuthorFirstName = JSONUtils.getStringDecoded(jsonAuthor, "first_name");
        post.authorId = jsonAuthor.optLong("ID");

        // v1.2 endpoint contains a "has_avatar" boolean which tells us whether the author
        // has a valid avatar - if this field exists and is set to false, skip setting
        // the avatar URL
        boolean hasAvatar;
        if (jsonAuthor.has("has_avatar")) {
            hasAvatar = jsonAuthor.optBoolean("has_avatar");
        } else {
            hasAvatar = true;
        }
        if (hasAvatar) {
            post.mPostAvatar = JSONUtils.getString(jsonAuthor, "avatar_URL");
        }

        // site_URL doesn't exist for /sites/ endpoints, so get it from the author
        if (TextUtils.isEmpty(post.mBlogUrl)) {
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
        if (!post.hasPrimaryTag()) {
            post.setPrimaryTag(mostPopularTag);
        }
        post.setSecondaryTag(nextMostPopularTag);
    }

    /*
     * extracts a title from a post's excerpt - used when the post has no title
     */
    private static String extractTitle(final String excerpt, int maxLen) {
        if (TextUtils.isEmpty(excerpt)) {
            return null;
        }

        if (excerpt.length() < maxLen) {
            return excerpt.trim();
        }

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
            if (totalLen >= maxLen) {
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

    // --------------------------------------------------------------------------------------------

    public String getAuthorName() {
        return StringUtils.notNullStr(mAuthorName);
    }

    public void setAuthorName(String name) {
        this.mAuthorName = StringUtils.notNullStr(name);
    }

    public String getAuthorFirstName() {
        return StringUtils.notNullStr(mAuthorFirstName);
    }

    public void setAuthorFirstName(String name) {
        this.mAuthorFirstName = StringUtils.notNullStr(name);
    }

    public String getTitle() {
        return StringUtils.notNullStr(mTitle);
    }

    public void setTitle(String title) {
        this.mTitle = StringUtils.notNullStr(title);
    }

    public String getText() {
        return StringUtils.notNullStr(mText);
    }

    public void setText(String text) {
        this.mText = StringUtils.notNullStr(text);
    }

    public String getExcerpt() {
        return StringUtils.notNullStr(mExcerpt);
    }

    public void setExcerpt(String excerpt) {
        this.mExcerpt = StringUtils.notNullStr(excerpt);
    }

    // https://codex.wordpress.org/Post_Formats
    public String getFormat() {
        return StringUtils.notNullStr(mFormat);
    }

    public void setFormat(String format) {
        this.mFormat = StringUtils.notNullStr(format);
    }

    public boolean isGallery() {
        return mFormat != null && mFormat.equals("gallery");
    }


    public String getUrl() {
        return StringUtils.notNullStr(mUrl);
    }

    public void setUrl(String url) {
        this.mUrl = StringUtils.notNullStr(url);
    }

    public String getShortUrl() {
        return StringUtils.notNullStr(mShortUrl);
    }

    public void setShortUrl(String shortUrl) {
        this.mShortUrl = StringUtils.notNullStr(shortUrl);
    }

    public boolean hasShortUrl() {
        return !TextUtils.isEmpty(mShortUrl);
    }

    public String getFeaturedImage() {
        return StringUtils.notNullStr(mFeaturedImage);
    }

    public void setFeaturedImage(String featuredImage) {
        this.mFeaturedImage = StringUtils.notNullStr(featuredImage);
    }

    public String getFeaturedVideo() {
        return StringUtils.notNullStr(mFeaturedVideo);
    }

    public void setFeaturedVideo(String featuredVideo) {
        this.mFeaturedVideo = StringUtils.notNullStr(featuredVideo);
    }

    public String getBlogName() {
        return StringUtils.notNullStr(mBlogName);
    }

    public void setBlogName(String blogName) {
        this.mBlogName = StringUtils.notNullStr(blogName);
    }

    public String getBlogUrl() {
        return StringUtils.notNullStr(mBlogUrl);
    }

    public void setBlogUrl(String blogUrl) {
        this.mBlogUrl = StringUtils.notNullStr(blogUrl);
    }

    public String getBlogImageUrl() {
        return StringUtils.notNullStr(mBlogImageUrl);
    }

    public void setBlogImageUrl(String imageUrl) {
        this.mBlogImageUrl = StringUtils.notNullStr(imageUrl);
    }

    public String getPostAvatar() {
        return StringUtils.notNullStr(mPostAvatar);
    }

    public void setPostAvatar(String postAvatar) {
        this.mPostAvatar = StringUtils.notNullStr(postAvatar);
    }

    public String getPseudoId() {
        return StringUtils.notNullStr(mPseudoId);
    }

    public void setPseudoId(String pseudoId) {
        this.mPseudoId = StringUtils.notNullStr(pseudoId);
    }

    public String getDatePublished() {
        return StringUtils.notNullStr(mDatePublished);
    }

    public void setDatePublished(String dateStr) {
        this.mDatePublished = StringUtils.notNullStr(dateStr);
    }

    public String getDateLiked() {
        return StringUtils.notNullStr(mDateLiked);
    }

    public void setDateLiked(String dateStr) {
        this.mDateLiked = StringUtils.notNullStr(dateStr);
    }

    public String getDateTagged() {
        return StringUtils.notNullStr(mDateTagged);
    }

    public void setDateTagged(String dateStr) {
        this.mDateTagged = StringUtils.notNullStr(dateStr);
    }

    public String getPrimaryTag() {
        return StringUtils.notNullStr(mPrimaryTag);
    }

    public void setPrimaryTag(String tagName) {
        this.mPrimaryTag = StringUtils.notNullStr(tagName);
    }

    public boolean hasPrimaryTag() {
        return !TextUtils.isEmpty(mPrimaryTag);
    }

    public String getSecondaryTag() {
        return StringUtils.notNullStr(mSecondaryTag);
    }

    public void setSecondaryTag(String tagName) {
        this.mSecondaryTag = StringUtils.notNullStr(tagName);
    }

    public boolean hasSecondaryTag() {
        return !TextUtils.isEmpty(mSecondaryTag);
    }

    /*
     * attachments are stored as the actual JSON to avoid having a separate table for
     * them, may need to revisit this if/when attachments become more important
     */
    public String getAttachmentsJson() {
        return StringUtils.notNullStr(mAttachmentsJson);
    }

    public void setAttachmentsJson(String json) {
        mAttachmentsJson = StringUtils.notNullStr(json);
    }

    public boolean hasAttachments() {
        return !TextUtils.isEmpty(mAttachmentsJson);
    }

    /*
     * "discover" posts also store the actual JSON
     */
    public String getDiscoverJson() {
        return StringUtils.notNullStr(mDiscoverJson);
    }

    public void setDiscoverJson(String json) {
        mDiscoverJson = StringUtils.notNullStr(json);
    }

    public boolean isDiscoverPost() {
        return !TextUtils.isEmpty(mDiscoverJson);
    }

    private transient ReaderPostDiscoverData mDiscoverData;

    public ReaderPostDiscoverData getDiscoverData() {
        if (mDiscoverData == null && !TextUtils.isEmpty(mDiscoverJson)) {
            try {
                mDiscoverData = new ReaderPostDiscoverData(new JSONObject(mDiscoverJson));
            } catch (JSONException e) {
                return null;
            }
        }
        return mDiscoverData;
    }

    public boolean hasText() {
        return !TextUtils.isEmpty(mText);
    }

    public boolean hasUrl() {
        return !TextUtils.isEmpty(mUrl);
    }

    public boolean hasExcerpt() {
        return !TextUtils.isEmpty(mExcerpt);
    }

    public boolean hasFeaturedImage() {
        return !TextUtils.isEmpty(mFeaturedImage);
    }

    public boolean hasFeaturedVideo() {
        return !TextUtils.isEmpty(mFeaturedVideo);
    }

    public boolean hasPostAvatar() {
        return !TextUtils.isEmpty(mPostAvatar);
    }

    public boolean hasBlogName() {
        return !TextUtils.isEmpty(mBlogName);
    }

    public boolean hasAuthorName() {
        return !TextUtils.isEmpty(mAuthorName);
    }

    public boolean hasAuthorFirstName() {
        return !TextUtils.isEmpty(mAuthorFirstName);
    }

    public boolean hasTitle() {
        return !TextUtils.isEmpty(mTitle);
    }

    public boolean hasBlogUrl() {
        return !TextUtils.isEmpty(mBlogUrl);
    }

    public boolean hasBlogImageUrl() {
        return !TextUtils.isEmpty(mBlogImageUrl);
    }

    /*
     * we should show the excerpt rather than full content for Jetpack posts that specify to show only the excerpt
     */
    public boolean shouldShowExcerpt() {
        return isJetpack && useExcerpt && hasExcerpt();
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
               && post.useExcerpt == this.useExcerpt
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


    public String getRailcarJson() {
        return StringUtils.notNullStr(mRailcarJson);
    }

    public void setRailcarJson(String jsonRailcar) {
        this.mRailcarJson = StringUtils.notNullStr(jsonRailcar);
    }

    public boolean hasRailcar() {
        return !TextUtils.isEmpty(mRailcarJson);
    }

    public ReaderCardType getCardType() {
        return mCardType != null ? mCardType : ReaderCardType.DEFAULT;
    }

    public void setCardType(ReaderCardType cardType) {
        this.mCardType = cardType;
    }

    /****
     * the following are transient variables - not stored in the db or returned in the json - whose
     * sole purpose is to cache commonly-used values for the post that speeds up using them inside
     * adapters
     ****/

    /*
     * returns the featured image url as a photon url set to the passed width/height
     */
    private transient String mFeaturedImageForDisplay;

    public String getFeaturedImageForDisplay(int width, int height) {
        if (mFeaturedImageForDisplay == null) {
            if (!hasFeaturedImage()) {
                mFeaturedImageForDisplay = "";
            } else {
                mFeaturedImageForDisplay = ReaderUtils.getResizedImageUrl(
                        mFeaturedImage, width, height, isPrivate, isPrivateAtomic);
            }
        }
        return mFeaturedImageForDisplay;
    }

    /*
     * returns the avatar url as a photon url set to the passed size
     */
    private transient String mAvatarForDisplay;

    public String getPostAvatarForDisplay(int size) {
        if (mAvatarForDisplay == null) {
            if (!hasPostAvatar()) {
                return "";
            }
            mAvatarForDisplay = GravatarUtils.fixGravatarUrl(mPostAvatar, size);
        }
        return mAvatarForDisplay;
    }

    /*
     * returns the blog's blavatar url as a photon url set to the passed size
     */
    private transient String mBlavatarForDisplay;

    public String getPostBlavatarForDisplay(int size) {
        if (mBlavatarForDisplay == null) {
            if (!hasBlogUrl()) {
                return "";
            }
            mBlavatarForDisplay = GravatarUtils.blavatarFromUrl(getBlogUrl(), size);
        }
        return mBlavatarForDisplay;
    }

    /*
     * converts iso8601 pubDate to a java date for display - this is the date that appears on posts
     */
    private transient java.util.Date mDateDisplay;

    public java.util.Date getDisplayDate() {
        if (mDateDisplay == null) {
            mDateDisplay = DateTimeUtils.dateFromIso8601(this.mDatePublished);
        }
        return mDateDisplay;
    }

    /*
     * used when a unique numeric id is required by an adapter (when hasStableIds() = true)
     */
    private transient long mStableId;

    public long getStableId() {
        if (mStableId == 0) {
            mStableId = (mPseudoId != null ? mPseudoId.hashCode() : 0);
        }
        return mStableId;
    }
}
