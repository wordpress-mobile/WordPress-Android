package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.Locale;

public class ReaderBlog {
    public long blogId;
    public long feedId;

    public boolean isPrivate;
    public boolean isJetpack;
    public boolean isFollowing;
    public boolean isNotificationsEnabled;
    public int numSubscribers;

    private String mName;
    private String mDescription;
    private String mUrl;
    private String mImageUrl;
    private String mFeedUrl;

    public static ReaderBlog fromJson(JSONObject json) {
        ReaderBlog blog = new ReaderBlog();
        if (json == null) {
            return blog;
        }

        JSONObject jsonNotification = JSONUtils.getJSONChild(json, "delivery_methods/notification");
        if (jsonNotification != null) {
            blog.isNotificationsEnabled = JSONUtils.getBool(jsonNotification, "send_posts");
        }

        // if meta/data/site exists then JSON is for a read/following/mine?meta=site subscription,
        // if meta/data/feed exists then JSON is for a read/following/mine?meta=feed subscription,
        // otherwise JSON the response for a single site/$siteId or read/feed/$feedId
        JSONObject jsonSite = JSONUtils.getJSONChild(json, "meta/data/site");
        JSONObject jsonFeed = JSONUtils.getJSONChild(json, "meta/data/feed");
        if (jsonSite != null) {
            blog.blogId = jsonSite.optLong("ID");
            blog.setName(JSONUtils.getStringDecoded(jsonSite, "name"));
            blog.setDescription(JSONUtils.getStringDecoded(jsonSite, "description"));
            blog.setUrl(JSONUtils.getString(jsonSite, "URL"));
            blog.isJetpack = JSONUtils.getBool(jsonSite, "jetpack");
            blog.isPrivate = JSONUtils.getBool(jsonSite, "is_private");
            blog.isFollowing = JSONUtils.getBool(jsonSite, "is_following");
            blog.numSubscribers = jsonSite.optInt("subscribers_count");
            JSONObject jsonIcon = jsonSite.optJSONObject("icon");
            if (jsonIcon != null) {
                blog.setImageUrl(JSONUtils.getString(jsonIcon, "img"));
            }
        } else if (jsonFeed != null) {
            blog.feedId = jsonFeed.optLong("feed_ID");
            blog.setFeedUrl(JSONUtils.getString(jsonFeed, "feed_URL"));
            blog.setName(JSONUtils.getStringDecoded(jsonFeed, "name"));
            blog.setUrl(JSONUtils.getString(jsonFeed, "URL"));
            blog.numSubscribers = jsonFeed.optInt("subscribers_count");
            // read/following/mine doesn't include is_following for feeds, so assume to be true
            blog.isFollowing = true;
        } else {
            blog.blogId = json.optLong("ID");
            if (blog.blogId == 0) {
                blog.blogId = json.optLong("blog_ID");
            }
            blog.feedId = json.optLong("feed_ID");
            blog.setName(JSONUtils.getStringDecoded(json, "name"));
            blog.setDescription(JSONUtils.getStringDecoded(json, "description"));
            blog.setUrl(JSONUtils.getString(json, "URL"));
            blog.setFeedUrl(JSONUtils.getString(json, "feed_URL"));
            blog.isJetpack = JSONUtils.getBool(json, "jetpack");
            blog.isPrivate = JSONUtils.getBool(json, "is_private");
            blog.isFollowing = JSONUtils.getBool(json, "is_following");
            blog.numSubscribers = json.optInt("subscribers_count");
        }

        // blogId will be empty for feeds, so set it to the feedId (consistent with /read/ endpoints)
        if (blog.blogId == 0 && blog.feedId != 0) {
            blog.blogId = blog.feedId;
        }

        JSONObject jsonIcon = JSONUtils.getJSONChild(json, "icon");
        if (jsonIcon != null) {
            blog.setImageUrl(JSONUtils.getString(jsonIcon, "img"));
            if (!blog.hasImageUrl()) {
                blog.setImageUrl(JSONUtils.getString(jsonIcon, "ico"));
            }
        }

        return blog;
    }

    public String getName() {
        return StringUtils.notNullStr(mName);
    }

    public void setName(String blogName) {
        this.mName = StringUtils.notNullStr(blogName).trim();
    }

    public String getDescription() {
        return StringUtils.notNullStr(mDescription);
    }

    public void setDescription(String description) {
        this.mDescription = StringUtils.notNullStr(description).trim();
    }

    public String getImageUrl() {
        return StringUtils.notNullStr(mImageUrl);
    }

    public void setImageUrl(String imageUrl) {
        this.mImageUrl = StringUtils.notNullStr(imageUrl);
    }

    public String getUrl() {
        return StringUtils.notNullStr(mUrl);
    }

    public void setUrl(String url) {
        this.mUrl = StringUtils.notNullStr(url);
    }

    public String getFeedUrl() {
        return StringUtils.notNullStr(mFeedUrl);
    }

    public void setFeedUrl(String feedUrl) {
        this.mFeedUrl = StringUtils.notNullStr(feedUrl);
    }

    public boolean hasUrl() {
        return !TextUtils.isEmpty(mUrl);
    }

    public boolean hasFeedUrl() {
        return !TextUtils.isEmpty(mFeedUrl);
    }

    public boolean hasImageUrl() {
        return !TextUtils.isEmpty(mImageUrl);
    }

    public boolean hasName() {
        return !TextUtils.isEmpty(mName);
    }

    public boolean hasDescription() {
        return !TextUtils.isEmpty(mDescription);
    }

    public boolean isExternal() {
        return (feedId != 0);
    }

    /*
     * returns the mshot url to use for this blog, ex:
     * http://s.wordpress.com/mshots/v1/http%3A%2F%2Fnickbradbury.com?w=600
     * note that while mshots support a "h=" parameter, this crops rather than
     * scales the image to that height
     * https://github.com/Automattic/mShots
     */
    public String getMshotsUrl(int width) {
        return "http://s.wordpress.com/mshots/v1/"
               + UrlUtils.urlEncode(getUrl())
               + String.format(Locale.US, "?w=%d", width);
    }

    public boolean isSameAs(ReaderBlog blogInfo) {
        return blogInfo != null
               && this.blogId == blogInfo.blogId
               && this.feedId == blogInfo.feedId
               && this.isFollowing == blogInfo.isFollowing
               && this.isPrivate == blogInfo.isPrivate
               && this.numSubscribers == blogInfo.numSubscribers
               && this.getName().equals(blogInfo.getName())
               && this.getDescription().equals(blogInfo.getDescription())
               && this.getUrl().equals(blogInfo.getUrl())
               && this.getFeedUrl().equals(blogInfo.getFeedUrl())
               && this.getImageUrl().equals(blogInfo.getImageUrl());
    }

    public boolean isSameBlogOrFeedAs(ReaderBlog blogInfo) {
        boolean areBothValidFeeds = this.blogId == this.feedId
                                    && blogInfo.blogId == blogInfo.feedId
                                    && this.hasFeedUrl()
                                    && blogInfo.hasFeedUrl();

        return blogInfo != null
               && this.blogId == blogInfo.blogId
               && this.getUrl().equals(blogInfo.getUrl())
               && (!areBothValidFeeds || this.getFeedUrl().equals(blogInfo.getFeedUrl()));
    }
}
