package org.wordpress.android.models;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.StringUtils;

/**
 * additional data for "discover" posts in the reader - these are posts chosen by
 * Editorial which highlight other posts or sites - the reader shows an attribution
 * line for these posts, and when tapped they open the original post - the like
 * and comment counts come from the original post
 */
public class ReaderPostDiscoverData {
    private String authorName;
    private String authorUrl;
    private String blogName;
    private String blogUrl;
    private String avatarUrl;
    private String permaLink;

    private long blogId;
    private long postId;

    public int numLikes;
    public int numComments;

    /*
     * passed JSONObject is the "discover_metadata" section of a reader post
     */
    public ReaderPostDiscoverData(@NonNull JSONObject json) {
        setPermaLink(json.optString("permalink"));

        JSONObject jsonAttribution = json.optJSONObject("attribution");
        if (jsonAttribution != null) {
            setAuthorName(jsonAttribution.optString("author_name"));
            setAuthorUrl(jsonAttribution.optString("author_url"));
            setBlogName(jsonAttribution.optString("blog_name"));
            setBlogUrl(jsonAttribution.optString("blog_url"));
            setAvatarUrl(jsonAttribution.optString("avatar_url"));
        }

        JSONObject jsonWpcomData = json.optJSONObject("featured_post_wpcom_data");
        if (jsonWpcomData != null) {
            blogId = jsonWpcomData.optLong("blog_id");
            postId = jsonWpcomData.optLong("post_id");
            numLikes = jsonWpcomData.optInt("like_count");
            numComments = jsonWpcomData.optInt("comment_count");
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
    private void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    private String getAuthorUrl() {
        return StringUtils.notNullStr(authorUrl);
    }
    private void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }

    private String getBlogName() {
        return StringUtils.notNullStr(blogName);
    }
    private void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    private String getBlogUrl() {
        return StringUtils.notNullStr(blogUrl);
    }
    private void setBlogUrl(String blogUrl) {
        this.blogUrl = blogUrl;
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(avatarUrl);
    }
    private void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPermaLink() {
        return StringUtils.notNullStr(permaLink);
    }
    private void setPermaLink(String permaLink) {
        this.permaLink = permaLink;
    }

    public boolean hasPostAndBlogIds() {
        return (postId != 0 && blogId != 0);
    }

    private boolean hasBlogName() {
        return !TextUtils.isEmpty(blogName);
    }

    private boolean hasAuthorName() {
        return !TextUtils.isEmpty(authorName);
    }

    /*
     * returns the spanned html for the attribution line, ex: "Originally posted by [AuthorName] in [BlogName]"
     */
    private transient Spanned attributionHtml;
    public Spanned getAttributionHtml() {
        if (attributionHtml == null) {
            String author = "<strong>" + getAuthorName() + "</strong>";
            String blog = "<strong>" + getBlogName() + "</strong>";
            String html;
            Context context = WordPress.getContext();
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

            attributionHtml = Html.fromHtml(html);
        }
        return attributionHtml;
    }

    public boolean hasAttributionHtml() {
        return getAttributionHtml() != null;
    }
}
