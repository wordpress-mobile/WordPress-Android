package org.wordpress.android.models;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

/**
 * additional data for "discover" posts in the reader - these are posts chosen by
 * Editorial which highlight other posts or sites - the reader shows an attribution
 * line for these posts, and when tapped they open the original post - the like
 * and comment counts come from the original post
 */
public class ReaderPostDiscoverData {

    public enum DiscoverType {
        EDITOR_PICK,
        SITE_PICK,
        OTHER
    }

    private String authorName;
    private String authorUrl;
    private String blogName;
    private String blogUrl;
    private String avatarUrl;
    private final String permaLink;

    private long blogId;
    private long postId;

    private int numLikes;
    private int numComments;

    private DiscoverType discoverType = DiscoverType.OTHER;

    /*
     * passed JSONObject is the "discover_metadata" section of a reader post
     */
    public ReaderPostDiscoverData(@NonNull JSONObject json) {
        permaLink = json.optString("permalink");

        JSONObject jsonAttribution = json.optJSONObject("attribution");
        if (jsonAttribution != null) {
            authorName = jsonAttribution.optString("author_name");
            authorUrl = jsonAttribution.optString("author_url");
            blogName = jsonAttribution.optString("blog_name");
            blogUrl = jsonAttribution.optString("blog_url");
            avatarUrl = jsonAttribution.optString("avatar_url");
        }

        JSONObject jsonWpcomData = json.optJSONObject("featured_post_wpcom_data");
        if (jsonWpcomData != null) {
            blogId = jsonWpcomData.optLong("blog_id");
            postId = jsonWpcomData.optLong("post_id");
            numLikes = jsonWpcomData.optInt("like_count");
            numComments = jsonWpcomData.optInt("comment_count");
        }

        // walk the post formats array until we find one we know we should handle differently
        //  - image-pick, quote-pick, and standard-pick all display as editors picks
        //  - site-pick displays as a site pick
        //  - collection + feature can be ignored because those display the same as normal posts
        JSONArray jsonPostFormats = json.optJSONArray("discover_fp_post_formats");
        if (jsonPostFormats != null) {
            for (int i = 0; i < jsonPostFormats.length(); i++) {
                String slug = JSONUtils.getString(jsonPostFormats.optJSONObject(i), "slug");
                if (slug.equals("site-pick")) {
                    discoverType = DiscoverType.SITE_PICK;
                    break;
                } else if (slug.equals("standard-pick") || slug.equals("image-pick") || slug.equals("quote-pick")) {
                    discoverType = DiscoverType.EDITOR_PICK;
                    break;
                }
            }
        }
    }

    public long getBlogId() {
        return blogId;
    }

    public long getPostId() {
        return postId;
    }

    private String getAuthorName() {
        return StringUtils.notNullStr(authorName);
    }

    private String getAuthorUrl() {
        return StringUtils.notNullStr(authorUrl);
    }

    public String getBlogName() {
        return StringUtils.notNullStr(blogName);
    }

    public String getBlogUrl() {
        return StringUtils.notNullStr(blogUrl);
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(avatarUrl);
    }

    public String getPermaLink() {
        return StringUtils.notNullStr(permaLink);
    }

    public boolean hasBlogUrl() {
        return !TextUtils.isEmpty(blogUrl);
    }

    public boolean hasBlogName() {
        return !TextUtils.isEmpty(blogName);
    }

    private boolean hasAuthorName() {
        return !TextUtils.isEmpty(authorName);
    }

    public boolean hasPermalink() {
        return !TextUtils.isEmpty(permaLink);
    }

    public boolean hasAvatarUrl() {
        return !TextUtils.isEmpty(avatarUrl);
    }

    public DiscoverType getDiscoverType() {
        return discoverType;
    }

    /*
     * returns the spanned html for the attribution line
     */
    private transient Spanned attributionHtml;
    public Spanned getAttributionHtml() {
        if (attributionHtml == null) {
            String html;
            String author = "<strong>" + getAuthorName() + "</strong>";
            String blog = "<strong>" + getBlogName() + "</strong>";
            Context context = WordPress.getContext();

            switch (getDiscoverType()) {
                case EDITOR_PICK:
                    if (hasBlogName() && hasAuthorName()) {
                        // "Originally posted by [AuthorName] on [BlogName]"
                        html = String.format(context.getString(R.string.reader_discover_attribution_author_and_blog), author, blog);
                    } else if (hasBlogName()) {
                        // "Originally posted on [BlogName]"
                        html = String.format(context.getString(R.string.reader_discover_attribution_blog), blog);
                    } else if (hasAuthorName()) {
                        // "Originally posted by [AuthorName]"
                        html = String.format(context.getString(R.string.reader_discover_attribution_author), author);
                    } else {
                        return null;
                    }
                    break;

                case SITE_PICK:
                    if (blogId != 0 && hasBlogName()) {
                        // "Visit [BlogName]" - opens blog preview when tapped
                        html = String.format(context.getString(R.string.reader_discover_visit_blog), blog);
                    } else {
                        return null;
                    }
                    break;

                default:
                    return null;
            }

            attributionHtml = Html.fromHtml(html);
        }
        return attributionHtml;
    }
}
