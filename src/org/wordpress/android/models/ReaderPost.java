package org.wordpress.android.models;

import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.text.BreakIterator;
import java.util.Iterator;

/**
 * Created by nbradbury on 6/27/13.
 */
public class ReaderPost {
    private String pseudoId;
    public long postId;
    public long blogId;

    private String title;
    private String text;
    private String excerpt;
    private String authorName;
    private String blogName;
    private String blogUrl;
    private String postAvatar;

    public long timestamp;        // used for sorting
    public String published;

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

    public static ReaderPost fromJson(JSONObject json) {
        if (json==null)
            throw new IllegalArgumentException("null json post");

        ReaderPost post = new ReaderPost();

        post.postId = json.optLong("ID");
        post.blogId = json.optLong("site_ID");
        post.pseudoId = JSONUtil.getString(json, "pseudo_ID");

        // remove HTML from the excerpt
        post.excerpt = HtmlUtils.fastStripHtml(JSONUtil.getString(json, "excerpt"));

        post.text = JSONUtil.getString(json, "content");
        post.title = JSONUtil.getStringDecoded(json, "title");
        post.url = JSONUtil.getString(json, "URL");
        post.blogUrl = JSONUtil.getString(json, "site_URL");

        post.numReplies = json.optInt("comment_count");
        post.numLikes = json.optInt("like_count");
        post.isLikedByCurrentUser = JSONUtil.getBool(json, "i_like");
        post.isFollowedByCurrentUser = JSONUtil.getBool(json, "is_following");
        post.isRebloggedByCurrentUser = JSONUtil.getBool(json, "is_reblogged");
        post.isCommentsOpen = JSONUtil.getBool(json, "comments_open");
        post.isExternal = JSONUtil.getBool(json, "is_external");
        post.isPrivate = JSONUtil.getBool(json, "site_is_private");

        post.published = JSONUtil.getString(json, "date");

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor!=null) {
            post.authorName = JSONUtil.getString(jsonAuthor, "name");
            post.postAvatar = JSONUtil.getString(jsonAuthor, "avatar_URL");
        }

        final String dateForTimestamp;

        // only freshly-pressed posts have the "editorial" section
        JSONObject jsonEditorial = json.optJSONObject("editorial");
        if (jsonEditorial!=null) {
            post.blogId = jsonEditorial.optLong("blog_id");
            post.blogName = JSONUtil.getStringDecoded(jsonEditorial, "blog_name");
            post.featuredImage = getImageUrlFromFeaturedImageUrl(JSONUtil.getString(jsonEditorial, "image"));
            // we want freshly-pressed posts to be sorted by the date they were chosen, not the date they were published
            dateForTimestamp = JSONUtil.getString(jsonEditorial, "displayed_on");
        } else {
            post.featuredImage = JSONUtil.getString(json, "featured_image");
            post.blogName = JSONUtil.getStringDecoded(json, "site_name");
            // the date a post was liked is only returned by the read/liked/ endpoint - if this exists,
            // set it as the timestamp so posts are sorted by the date they were liked rather than the
            // date they were published
            String likeDate = JSONUtil.getString(json, "date_liked");
            if (!TextUtils.isEmpty(likeDate)) {
                dateForTimestamp = likeDate;
            } else {
                // date_liked doesn't exist, so set timestamp to published date
                dateForTimestamp = post.published;
            }
        }

        // set the timestamp for sorting
        post.timestamp = DateTimeUtils.iso8601ToTimestamp(dateForTimestamp);

        // parse attachments to get the VideoPress thumbnail & url
        /*"attachments": {
				"321": {
					"ID": 321,
					"URL": "http://dissolvingbuildings.files.wordpress.com/2013/08/webcam-video-from-25-august-2013-18-33.mp4",
					"guid": "http://dissolvingbuildings.files.wordpress.com/2013/08/webcam-video-from-25-august-2013-18-33.mp4",
					"mime_type": "video/mp4",
					"width": 640,
					"height": 360,
					"duration": 21,
					"videopress_files": {
						"dvd": {
							"url": "https://videos.files.wordpress.com/K2nkj5C2/webcam-video-from-25-august-2013-18-33_dvd.mp4",
							"mime_type": "video/mp4"
						},
						"std": {
							"url": "https://videos.files.wordpress.com/K2nkj5C2/webcam-video-from-25-august-2013-18-33_std.mp4",
							"mime_type": "video/mp4"
						},
						"ogg": {
							"url": "https://videos.files.wordpress.com/K2nkj5C2/webcam-video-from-25-august-2013-18-33_fmt1.ogv",
							"mime_type": "video/ogg"
						}
					},
					"videopress_thumbnail": "https://videos.files.wordpress.com/K2nkj5C2/webcam-video-from-25-august-2013-18-33_dvd.original.jpg"
				}
			}, */
        JSONObject jsonAttachments = json.optJSONObject("attachments");
        if (jsonAttachments != null) {
            Iterator<String> it = jsonAttachments.keys();
            if (it != null && it.hasNext()) {
                JSONObject jsonFirstAttachment = jsonAttachments.optJSONObject(it.next());
                if (jsonFirstAttachment != null) {
                    String thumbnail = JSONUtil.getString(jsonFirstAttachment, "videopress_thumbnail");
                    if (!TextUtils.isEmpty(thumbnail))
                        post.featuredImage = thumbnail;
                    JSONObject jsonVideoPress = jsonFirstAttachment.optJSONObject("videopress_files");
                    if (jsonVideoPress != null) {
                        JSONObject jsonStdVideo = jsonVideoPress.optJSONObject("std");
                        if (jsonStdVideo != null)
                            post.featuredVideo = JSONUtil.getString(jsonStdVideo, "url");
                    }
                }
            }
        }

        // if there's no featured thumbnail, check if featured media has been set - this is sometimes
        // a YouTube or Vimeo video, in which case store it as the featured video so we can treat
        // it as a video
        if (!post.hasFeaturedImage()) {
            JSONObject jsonMedia = json.optJSONObject("featured_media");
            if (jsonMedia!=null) {
                String mediaUrl = JSONUtil.getString(jsonMedia, "uri");
                String type = JSONUtil.getString(jsonMedia, "type");
                boolean isVideo = (type!=null && type.equals("video"));
                if (isVideo) {
                    post.featuredVideo = mediaUrl;
                } else {
                    post.featuredImage = mediaUrl;
                }
            }

            // if we still don't have a featured image, parse the content for an image that's
            // suitable as a featured image - this is done since featured_media seems to miss
            // some images that would work well as featured images on mobile
            if (!post.hasFeaturedImage() && post.isWP())
                post.featuredImage = findFeaturedImage(post.text);
        }

        // if the post is untitled, make up a title from the excerpt
        if (!post.hasTitle() && post.hasExcerpt())
            post.title = extractTitle(post.excerpt, 50);

        return post;
    }

    /*
     * extracts a title from a post's excerpt
     */
    private static String extractTitle(final String excerpt, int maxLen) {
        if (TextUtils.isEmpty(excerpt))
            return null;

        if (excerpt.length() < maxLen)
            return excerpt.trim();

        //return excerpt.substring(0, maxLen).trim() + "...";

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
     * specific WP image classes
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
                ReaderLog.d("found featured image");
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
     * This gawdawful hack is necessary to get VideoPress videos to work in the Reader since the v1
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
     * returns true if this post is from a WordPress blog
     */
    public boolean isWP() {
        return !isExternal;
    }

    /****
     * the following are transient variables - not stored in the db or returned in the json - whose
     * sole purpose is cache commonly-used values for the post that speeds up using them inside
     * adapters
     ****/

    /*
     * returns the featured image url as a photon url set to the passed width/height
     */
    private transient String featuredImageForDisplay;
    public String getFeaturedImageForDisplay(int width, int height) {
        if (featuredImageForDisplay==null) {
            if (!hasFeaturedImage())
                return "";
            if (isPrivate) {
                // can't use photon on images in private posts since they require authentication, and must
                // use https: in order for AuthToken to work when requesting them
                featuredImageForDisplay = UrlUtils.makeHttps(featuredImage);
            } else if (UrlUtils.isHttps(featuredImage)) {
                // skip photon for https images since we can't authenticate them
                featuredImageForDisplay = featuredImage;
            } else {
                // not private or https, so set to correctly sized photon url
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
        if (avatarForDisplay==null) {
            if (!hasPostAvatar())
                return "";
            avatarForDisplay = PhotonUtils.fixAvatar(postAvatar, avatarSize);
        }
        return avatarForDisplay;
    }

    /*
     * converts iso8601 published date to an actual java date
     */
    private transient java.util.Date dtPublished;
    public java.util.Date getDatePublished() {
        if (dtPublished==null)
            dtPublished = DateTimeUtils.iso8601ToJavaDate(published);
        return dtPublished;
    }

    /*
     * returns "blog name | author name | date" - not cached since we want the timespan to accurately
     * reflect the time this was called, but the dtPublished that this relies on *is* cached above
     */
    private static final String SOURCE_SEP = " | ";
    public String getSource() {
        String source;
        if (hasBlogName() && hasAuthorName()) {
            // skip author name if it's the same as the blog name (sometimes it's a lowercase version of the blog name)
            if (authorName.equalsIgnoreCase(blogName)) {
                source = blogName;
            } else {
                source = blogName + SOURCE_SEP + authorName;
            }
        } else if (hasAuthorName()) {
            source = authorName;
        } else if (hasBlogName()) {
            source = blogName;
        } else {
            source = "";
        }

        return source + SOURCE_SEP + DateTimeUtils.javaDateToTimeSpan(getDatePublished());
    }
}