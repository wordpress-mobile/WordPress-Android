package org.wordpress.android.ui.reader.models;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.utils.ImageSizeMap;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtils;

/**
 * simplified version of a reader post that contains only the fields necessary for a related post
 */
public class ReaderRelatedPost {
    private long mPostId;
    private long mBlogId;
    private String mTitle;
    private String mAuthorName;
    private String mExcerpt;
    private String mFeaturedImage;
    private String mSiteName;
    private String mPubDate;

    public static ReaderRelatedPost fromJson(@NonNull JSONObject json) {
        ReaderRelatedPost post = new ReaderRelatedPost();

        post.mPostId = json.optLong("ID");
        post.mBlogId = json.optLong("site_ID");

        post.mTitle = JSONUtils.getStringDecoded(json, "title");
        post.mExcerpt = HtmlUtils.fastStripHtml(JSONUtils.getString(json, "excerpt")).trim();
        post.mFeaturedImage = JSONUtils.getString(json, "featured_image");
        post.mSiteName = JSONUtils.getStringDecoded(json, "site_name");
        post.mPubDate = JSONUtils.getString(json, "date");

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor != null) {
            post.mAuthorName = JSONUtils.getStringDecoded(jsonAuthor, "name");
        }

        // if we don't have a featured image, check whethe we can find a suitable image from the attachments
        if (!post.hasFeaturedImage() && json.has("attachments")) {
            JSONObject jsonAttachments = json.optJSONObject("attachments");
            post.mFeaturedImage = new ImageSizeMap(jsonAttachments.toString())
                    .getLargestImageUrl(ReaderConstants.MIN_FEATURED_IMAGE_WIDTH);
        }

        return post;
    }

    public long getPostId() {
        return mPostId;
    }

    public long getBlogId() {
        return mBlogId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getExcerpt() {
        return mExcerpt;
    }

    public String getAuthorName() {
        return mAuthorName;
    }

    public String getFeaturedImage() {
        return mFeaturedImage;
    }

    public boolean hasExcerpt() {
        return !TextUtils.isEmpty(mExcerpt);
    }

    public boolean hasFeaturedImage() {
        return !TextUtils.isEmpty(mFeaturedImage);
    }

    public boolean hasAuthorName() {
        return !TextUtils.isEmpty(mAuthorName);
    }
}
