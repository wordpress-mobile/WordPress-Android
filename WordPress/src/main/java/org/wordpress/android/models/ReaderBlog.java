package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.ui.Organization;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

public class ReaderBlog {
    public long blogId;
    public long feedId;

    public boolean isPrivate;
    public boolean isJetpack;
    public boolean isFollowing;
    public boolean isNotificationsEnabled;
    public int numSubscribers;
    public int organizationId;
    public int numUnseenPosts;

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
        // if date_subscribed exists then JSON is a read/following/mine subscription whose site or feed
        // the API returned no details for, otherwise JSON the response for a single site/$siteId or
        // read/feed/$feedId
        boolean isSubscription = json.has("date_subscribed");
        JSONObject jsonSite = withoutError(JSONUtils.getJSONChild(json, "meta/data/site"));
        JSONObject jsonFeed = withoutError(JSONUtils.getJSONChild(json, "meta/data/feed"));
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
            blog.organizationId = jsonSite.optInt("organization_id");
        } else if (jsonFeed != null) {
            blog.feedId = jsonFeed.optLong("feed_ID");
            blog.setFeedUrl(JSONUtils.getString(jsonFeed, "feed_URL"));
            blog.setName(JSONUtils.getStringDecoded(jsonFeed, "name"));
            blog.setUrl(JSONUtils.getString(jsonFeed, "URL"));
            blog.numSubscribers = jsonFeed.optInt("subscribers_count");
            // read/following/mine doesn't include is_following for feeds, so assume to be true
            blog.isFollowing = true;
        } else if (isSubscription) {
            // read/following/mine returned no usable site or feed for this subscription, which it does for
            // a site the user can't access - everything it tells us about it is filled in below
            // read/following/mine doesn't include is_following, so assume to be true
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

        if (isSubscription) {
            fillGapsFromSubscription(blog, json);
        }

        blog.numUnseenPosts = json.optInt("unseen_count");

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

    /*
     * returns the passed site or feed unless it's an error, which read/following/mine returns in place of
     * the site or feed of a subscription the user can't access, such as a private site
     */
    private static JSONObject withoutError(JSONObject json) {
        return json != null && json.has("errors") ? null : json;
    }

    /*
     * fills in what the site or feed of a read/following/mine subscription didn't provide from the
     * subscription's own fields - for a subscription the user can't access the site or feed comes back
     * as an error, or with no name and no URL, and then this is all the API tells us about it
     */
    private static void fillGapsFromSubscription(ReaderBlog blog, JSONObject json) {
        if (blog.blogId == 0) {
            blog.blogId = json.optLong("blog_ID");
        }
        if (blog.blogId != 0) {
            // a site is keyed by its blog id alone, as it is when the API does return its site
            blog.feedId = 0;
        } else if (blog.feedId == 0) {
            blog.feedId = json.optLong("feed_ID");
        }
        if (!blog.hasName()) {
            blog.setName(JSONUtils.getStringDecoded(json, "name"));
        }
        if (!blog.hasUrl()) {
            blog.setUrl(JSONUtils.getString(json, "URL"));
        }
        if (!blog.hasFeedUrl()) {
            blog.setFeedUrl(JSONUtils.getString(json, "feed_URL"));
        }
        if (!blog.hasImageUrl() && !json.isNull("site_icon")) {
            blog.setImageUrl(JSONUtils.getString(json, "site_icon"));
        }
        if (blog.organizationId == 0) {
            blog.organizationId = json.optInt("organization_id");
        }
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

    public Organization getOrganization() {
        return Organization.fromOrgId(organizationId);
    }

    /*
     * returns true if this is a P2 or A8C site
     */
    public boolean isP2orA8C() {
        return getOrganization() == Organization.P2 || getOrganization() == Organization.A8C;
    }

    public boolean isSameAs(ReaderBlog blogInfo) {
        return blogInfo != null
               && this.blogId == blogInfo.blogId
               && this.feedId == blogInfo.feedId
               && this.isFollowing == blogInfo.isFollowing
               && this.isPrivate == blogInfo.isPrivate
               && this.numSubscribers == blogInfo.numSubscribers
               && this.numUnseenPosts == blogInfo.numUnseenPosts
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
