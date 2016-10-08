package org.wordpress.android.ui.reader.models;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtils;

/**
 * simplified version of a reader post that contains only the fields necessary for a related post
 */
public class ReaderRelatedPost {
    private long mPostId;
    private long mSiteId;
    private boolean mIsFollowing;

    private String mTitle;
    private String mAuthorName;
    private String mAuthorAvatarUrl;
    private String mExcerpt;
    private String mSiteName;
    private String mFeaturedImageUrl;

    // these are the specific fields we should ask for when requesting related posts from
    // the endpoint - note that we want to avoid ever requesting the post content, since
    // that makes the call much heavier
    public static final String RELATED_POST_FIELDS = "ID,site_ID,title,excerpt,site_name,is_following,author,featured_image,featured_media";

    public static ReaderRelatedPost fromJson(@NonNull JSONObject json) {
        ReaderRelatedPost post = new ReaderRelatedPost();

        post.mPostId = json.optLong("ID");
        post.mSiteId = json.optLong("site_ID");
        post.mIsFollowing = JSONUtils.getBool(json, "is_following");

        post.mTitle = JSONUtils.getStringDecoded(json, "title");
        post.mExcerpt = HtmlUtils.fastStripHtml(JSONUtils.getString(json, "excerpt")).trim();
        post.mSiteName = JSONUtils.getStringDecoded(json, "site_name");
        post.mFeaturedImageUrl = JSONUtils.getString(json, "featured_image");

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor != null) {
            post.mAuthorName = JSONUtils.getStringDecoded(jsonAuthor, "name");
            post.mAuthorAvatarUrl = JSONUtils.getString(jsonAuthor, "avatar_URL");
        }

        // if there's no featured image, check if featured media has been set to an image
        if (!post.hasFeaturedImageUrl() && json.has("featured_media")) {
            JSONObject jsonMedia = json.optJSONObject("featured_media");
            String type = JSONUtils.getString(jsonMedia, "type");
            if (type.equals("image")) {
                post.mFeaturedImageUrl = JSONUtils.getString(jsonMedia, "uri");
            }
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

    public boolean isFollowing() {
        return mIsFollowing;
    }

    public void setIsFollowing(boolean isFollowing) {
        mIsFollowing = isFollowing;
    }

    public boolean hasExcerpt() {
        return !TextUtils.isEmpty(mExcerpt);
    }

    public boolean hasAuthorAvatarUrl() {
        return !TextUtils.isEmpty(mAuthorAvatarUrl);
    }

    public boolean hasFeaturedImageUrl() {
        return !TextUtils.isEmpty(mFeaturedImageUrl);
    }
}
