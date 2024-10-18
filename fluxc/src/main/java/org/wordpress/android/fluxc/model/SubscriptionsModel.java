package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionsModel extends Payload<BaseNetworkError> {
    private List<SubscriptionModel> mSubscriptions;

    public SubscriptionsModel() {
        mSubscriptions = new ArrayList<>();
    }

    public SubscriptionsModel(@NonNull List<SubscriptionModel> subscriptions) {
        mSubscriptions = subscriptions;
    }

    public List<SubscriptionModel> getSubscriptions() {
        return mSubscriptions;
    }

    public void setSubscriptions(List<SubscriptionModel> subscriptions) {
        this.mSubscriptions = subscriptions;
    }
}
