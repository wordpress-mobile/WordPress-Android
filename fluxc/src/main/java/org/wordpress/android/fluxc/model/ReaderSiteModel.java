package org.wordpress.android.fluxc.model;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

public class ReaderSiteModel extends Payload<BaseNetworkError> {
    private long mSiteId;
    private long mFeedId;
    private int mSubscriberCount;
    private boolean mIsFollowing;
    private String mSubscribeUrl;
    private String mTitle;
    private String mDescription;
    private String mUrl;
    private String mIconUrl;

    public long getSiteId() {
        return mSiteId;
    }

    public void setSiteId(long siteId) {
        mSiteId = siteId;
    }

    public long getFeedId() {
        return mFeedId;
    }

    public void setFeedId(long feedId) {
        mFeedId = feedId;
    }

    public int getSubscriberCount() {
        return mSubscriberCount;
    }

    public void setSubscriberCount(int subscriberCount) {
        mSubscriberCount = subscriberCount;
    }

    public String getSubscribeUrl() {
        return mSubscribeUrl;
    }

    public void setSubscribeUrl(String subscribeUrl) {
        mSubscribeUrl = subscribeUrl;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public boolean isFollowing() {
        return mIsFollowing;
    }

    public void setFollowing(boolean following) {
        mIsFollowing = following;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getIconUrl() {
        return mIconUrl;
    }

    public void setIconUrl(String iconUrl) {
        mIconUrl = iconUrl;
    }
}
