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
    private long mSiteId;
    private String mTitle;
    private String mAuthorName;
    private String mAuthorAvatarUrl;
    private String mExcerpt;
    private String mSiteName;
    private String mFeaturedImageUrl;

    public static ReaderRelatedPost fromJson(@NonNull JSONObject json) {
        ReaderRelatedPost post = new ReaderRelatedPost();

        post.mPostId = json.optLong("ID");
        post.mSiteId = json.optLong("site_ID");
        post.mTitle = JSONUtils.getStringDecoded(json, "title");
        post.mExcerpt = HtmlUtils.fastStripHtml(JSONUtils.getString(json, "excerpt")).trim();
        post.mSiteName = JSONUtils.getStringDecoded(json, "site_name");
        post.mFeaturedImageUrl = JSONUtils.getString(json, "featured_image");

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor != null) {
            post.mAuthorName = JSONUtils.getStringDecoded(jsonAuthor, "name");
            post.mAuthorAvatarUrl = JSONUtils.getString(jsonAuthor, "avatar_URL");
        }

        // if the post doesn't have an assigned featured image, try to guess one from its attachments
        if (!post.hasFeaturedImageUrl() && json.has("attachments")) {
            JSONObject jsonAttachments = json.optJSONObject("attachments");
            post.mFeaturedImageUrl = new ImageSizeMap(jsonAttachments.toString())
                    .getLargestImageUrl(ReaderConstants.MIN_FEATURED_IMAGE_WIDTH);
        }

        return post;
    }

    public long getPostId() {
        return mPostId;
    }

    public long getSiteId() {
        return mSiteId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getExcerpt() {
        return mExcerpt;
    }

    public String getSiteName() {
        return mSiteName;
    }

    public String getAuthorName() {
        return mAuthorName;
    }

    public String getAuthorAvatarUrl() {
        return mAuthorAvatarUrl;
    }

    public String getFeaturedImageUrl() {
        return mFeaturedImageUrl;
    }

    public boolean hasExcerpt() {
        return !TextUtils.isEmpty(mExcerpt);
    }

    public boolean hasAuthorName() {
        return !TextUtils.isEmpty(mAuthorName);
    }

    public boolean hasAuthorAvatarUrl() {
        return !TextUtils.isEmpty(mAuthorAvatarUrl);
    }

    public boolean hasFeaturedImageUrl() {
        return !TextUtils.isEmpty(mFeaturedImageUrl);
    }
}
