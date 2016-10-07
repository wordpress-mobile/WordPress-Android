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
    private String mTitle;
    private String mAuthorName;
    private String mAuthorAvatarUrl;
    private String mExcerpt;
    private String mSiteName;

    public static ReaderRelatedPost fromJson(@NonNull JSONObject json) {
        ReaderRelatedPost post = new ReaderRelatedPost();

        post.mPostId = json.optLong("ID");
        post.mSiteId = json.optLong("site_ID");
        post.mTitle = JSONUtils.getStringDecoded(json, "title");
        post.mExcerpt = HtmlUtils.fastStripHtml(JSONUtils.getString(json, "excerpt")).trim();
        post.mSiteName = JSONUtils.getStringDecoded(json, "site_name");

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor != null) {
            post.mAuthorName = JSONUtils.getStringDecoded(jsonAuthor, "name");
            post.mAuthorAvatarUrl = JSONUtils.getString(jsonAuthor, "avatar_URL");
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

    public boolean hasExcerpt() {
        return !TextUtils.isEmpty(mExcerpt);
    }

    public boolean hasAuthorName() {
        return !TextUtils.isEmpty(mAuthorName);
    }

    public boolean hasAuthorAvatarUrl() {
        return !TextUtils.isEmpty(mAuthorAvatarUrl);
    }
}
