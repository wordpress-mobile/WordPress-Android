package org.wordpress.android.ui.reader.models;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.PhotonUtils;

/**
 * simplified version of a reader post
 */
public class ReaderSimplePost {
    private long mPostId;
    private long mSiteId;
    private String mTitle;
    private String mExcerpt;
    private String mSiteName;
    private String mFeaturedImageUrl;
    private transient String mFeaturedImageForDisplay;

    private String mRailcarJson;

    /*
       these are the specific fields we should ask for when requesting simple posts from
       the endpoint - note that we want to avoid ever requesting the post content, since
       that makes the call much heavier
    */
    public static final String SIMPLE_POST_FIELDS =
            "ID,site_ID,title,excerpt,site_name,featured_image,featured_media,railcar";

    public static ReaderSimplePost fromJson(JSONObject json) {
        if (json == null) {
            throw new IllegalArgumentException("ReaderSimplePost requires a non-null JSONObject");
        }

        ReaderSimplePost post = new ReaderSimplePost();

        // ID and site_ID are required, so make sure we have them
        try {
            post.mPostId = json.getLong("ID");
            post.mSiteId = json.getLong("site_ID");
        } catch (JSONException e) {
            AppLog.e(AppLog.T.READER, e);
            return null;
        }

        post.mTitle = JSONUtils.getStringDecoded(json, "title");
        post.mExcerpt = HtmlUtils.fastStripHtml(JSONUtils.getString(json, "excerpt")).trim();
        post.mSiteName = JSONUtils.getStringDecoded(json, "site_name");
        post.mFeaturedImageUrl = JSONUtils.getString(json, "featured_image");

        // if there's no featured image, check if featured media has been set to an image
        if (!post.hasFeaturedImageUrl() && json.has("featured_media")) {
            JSONObject jsonMedia = json.optJSONObject("featured_media");
            String type = JSONUtils.getString(jsonMedia, "type");
            if (type.equals("image")) {
                post.mFeaturedImageUrl = JSONUtils.getString(jsonMedia, "uri");
            }
        }

        JSONObject jsonRailcar = json.optJSONObject("railcar");
        if (jsonRailcar != null) {
            post.mRailcarJson = jsonRailcar.toString();
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


    public String getFeaturedImageUrl() {
        return mFeaturedImageUrl;
    }

    public String getRailcarJson() {
        return mRailcarJson;
    }

    public boolean hasExcerpt() {
        return !TextUtils.isEmpty(mExcerpt);
    }

    public boolean hasTitle() {
        return !TextUtils.isEmpty(mTitle);
    }

    public boolean hasFeaturedImageUrl() {
        return !TextUtils.isEmpty(mFeaturedImageUrl);
    }

    public String getFeaturedImageForDisplay(int width, int height) {
        if (mFeaturedImageForDisplay == null) {
            if (!hasFeaturedImageUrl()) {
                mFeaturedImageForDisplay = "";
            } else {
                mFeaturedImageForDisplay = PhotonUtils.getPhotonImageUrl(getFeaturedImageUrl(), width, height);
            }
        }
        return mFeaturedImageForDisplay;
    }
}
