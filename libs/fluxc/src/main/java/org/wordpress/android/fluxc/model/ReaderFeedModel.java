package org.wordpress.android.fluxc.model;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

public class ReaderFeedModel extends Payload<BaseNetworkError> {
    /*
    {
    "algorithm": "reader/manage/search:0",
    "feeds": [
        {
            "feed_ID": "46238560",
            "meta": {
                "links": {
                    "feed": "https://public-api.wordpress.com/rest/v1.1/read/feed/46238560"
                }
            },
            "railcar": {
                "fetch_algo": "reader/manage/search:0",
                "fetch_lang": "en",
                "fetch_position": 0,
                "fetch_query": "yoga",
                "railcar": "8&6VDLsINQLj",
                "rec_feed_id": 46238560
            },
            "subscribe_URL": "http://theyogalunchbox.co.nz/feed",
            "subscribers_count": 167,
            "title": "The Yoga Lunchbox",
            "URL": "https://theyogalunchbox.co.nz/"
        },
     */

    private long mFeedId;
    public int SubscriberCount;
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
        return SubscriberCount;
    }

    public void setSubscriberCount(int subscriberCount) {
        SubscriberCount = subscriberCount;
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
