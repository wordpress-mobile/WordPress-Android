package org.wordpress.android.fluxc.model;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

import java.util.ArrayList;
import java.util.List;

public class ReaderFeedsModel extends Payload<BaseNetworkError> {
    private List<ReaderFeedModel> mFeeds;

    public ReaderFeedsModel() {
        mFeeds = new ArrayList<>();
    }

    public ReaderFeedsModel(@NonNull List<ReaderFeedModel> feeds) {
        mFeeds = feeds;
    }

    public List<ReaderFeedModel> getSites() {
        return mFeeds;
    }

    public void setSites(List<ReaderFeedModel> sites) {
        this.mFeeds = sites;
    }
}
