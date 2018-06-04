package org.wordpress.android.fluxc.model;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

public class ReaderFeedModel extends Payload<BaseNetworkError> {
    private long mFeedId;
    public int mSubscriberCount;
    private String mSubscribeUrl;
    private String mTitle;
    private String mUrl;

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
}
