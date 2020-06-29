package org.wordpress.android.models;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import androidx.annotation.NonNull;

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

    private String mAuthorName;
    private String mAuthorUrl;
    private String mBlogName;
    private String mBlogUrl;
    private String mAvatarUrl;
    private final String mPermaLink;

    private long mBlogId;
    private long mPostId;

    private int mNumLikes;
    private int mNumComments;

    private DiscoverType mDiscoverType = DiscoverType.OTHER;

    /*
     * passed JSONObject is the "discover_metadata" section of a reader post
     */
    public ReaderPostDiscoverData(@NonNull JSONObject json) {
        mPermaLink = json.optString("permalink");

        JSONObject jsonAttribution = json.optJSONObject("attribution");
        if (jsonAttribution != null) {
            mAuthorName = jsonAttribution.optString("author_name");
            mAuthorUrl = jsonAttribution.optString("author_url");
            mBlogName = jsonAttribution.optString("blog_name");
            mBlogUrl = jsonAttribution.optString("blog_url");
            mAvatarUrl = jsonAttribution.optString("avatar_url");
        }

        JSONObject jsonWpcomData = json.optJSONObject("featured_post_wpcom_data");
        if (jsonWpcomData != null) {
            mBlogId = jsonWpcomData.optLong("blog_id");
            mPostId = jsonWpcomData.optLong("post_id");
            mNumLikes = jsonWpcomData.optInt("like_count");
            mNumComments = jsonWpcomData.optInt("comment_count");
        }

        // walk the post formats array until we find one we know we should handle differently
        // - image-pick, quote-pick, and standard-pick all display as editors picks
        // - site-pick displays as a site pick
        // - collection + feature can be ignored because those display the same as normal posts
        JSONArray jsonPostFormats = json.optJSONArray("discover_fp_post_formats");
        if (jsonPostFormats != null) {
            for (int i = 0; i < jsonPostFormats.length(); i++) {
                String slug = JSONUtils.getString(jsonPostFormats.optJSONObject(i), "slug");
                if (slug.equals("site-pick")) {
                    mDiscoverType = DiscoverType.SITE_PICK;
                    break;
                } else if (slug.equals("standard-pick") || slug.equals("image-pick") || slug.equals("quote-pick")) {
                    mDiscoverType = DiscoverType.EDITOR_PICK;
                    break;
                }
            }
        }
    }

    public long getBlogId() {
        return mBlogId;
    }

    public long getPostId() {
        return mPostId;
    }

    private String getAuthorName() {
        return StringUtils.notNullStr(mAuthorName);
    }

    private String getAuthorUrl() {
        return StringUtils.notNullStr(mAuthorUrl);
    }

    public String getBlogName() {
        return StringUtils.notNullStr(mBlogName);
    }

    public String getBlogUrl() {
        return StringUtils.notNullStr(mBlogUrl);
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(mAvatarUrl);
    }

    public String getPermaLink() {
        return StringUtils.notNullStr(mPermaLink);
    }

    public boolean hasBlogUrl() {
        return !TextUtils.isEmpty(mBlogUrl);
    }

    public boolean hasBlogName() {
        return !TextUtils.isEmpty(mBlogName);
    }

    private boolean hasAuthorName() {
        return !TextUtils.isEmpty(mAuthorName);
    }

    public boolean hasPermalink() {
        return !TextUtils.isEmpty(mPermaLink);
    }

    public boolean hasAvatarUrl() {
        return !TextUtils.isEmpty(mAvatarUrl);
    }

    public DiscoverType getDiscoverType() {
        return mDiscoverType;
    }

    /*
     * returns the spanned html for the attribution line
     */
    private transient Spanned mAttributionHtml;

    public Spanned getAttributionHtml() {
        if (mAttributionHtml == null) {
            String html;
            String author = "<strong>" + getAuthorName() + "</strong>";
            String blog = "<strong>" + getBlogName() + "</strong>";
            Context context = WordPress.getContext();

            switch (getDiscoverType()) {
                case EDITOR_PICK:
                    if (hasBlogName() && hasAuthorName()) {
                        // "Originally posted by [AuthorName] on [BlogName]"
                        html = String.format(context.getString(R.string.reader_discover_attribution_author_and_blog),
                                             author, blog);
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
                    if (mBlogId != 0 && hasBlogName()) {
                        // "Visit [BlogName]" - opens blog preview when tapped
                        html = String.format(context.getString(R.string.reader_discover_visit_blog), blog);
                    } else {
                        html = "";
                    }
                    break;

                default:
                    html = "";
            }

            mAttributionHtml = Html.fromHtml(html);
        }
        return mAttributionHtml;
    }
}
