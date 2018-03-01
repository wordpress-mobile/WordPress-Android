package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

import java.util.List;

import javax.inject.Singleton;

@Singleton
public class StockMediaStore extends Store {
    public static final int DEFAULT_NUM_STOCK_MEDIA_PER_FETCH = 20;

    StockMediaStore(Dispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * Actions: FETCH_MEDIA_LIST
     */
    public static class FetchStockMediaListPayload extends Payload<BaseNetworkError> {
        public boolean canLoadMore;
        public int nextPage;
        public List<StockMediaModel> mediaList;
        public int number = DEFAULT_NUM_STOCK_MEDIA_PER_FETCH;

        public FetchStockMediaListPayload(@NonNull List<StockMediaModel> mediaList, int nextPage, boolean canLoadMore) {
            this.mediaList = mediaList;
            this.canLoadMore = canLoadMore;
            this.nextPage = nextPage;
        }
    }

    @Override
    public void onAction(Action action) {
        // TODO
    }

    @Override
    public void onRegister() {
        // TODO
    }
}
