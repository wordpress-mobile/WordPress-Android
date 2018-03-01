package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.StockMediaAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StockMediaStore extends Store {
    public static final int DEFAULT_NUM_STOCK_MEDIA_PER_FETCH = 20;
    private final StockMediaRestClient mMediaRestClient;

    @Inject
    public StockMediaStore(Dispatcher dispatcher, StockMediaRestClient restClient) {
        super(dispatcher);
        mMediaRestClient = restClient;
    }

    /**
     * Actions: FETCH_MEDIA_LIST
     */
    public static class FetchStockMediaListPayload extends Payload<BaseNetworkError> {
        public String searchTerm;
        public int page;
        public BaseRequest.BaseNetworkError error;
        public int number = DEFAULT_NUM_STOCK_MEDIA_PER_FETCH;

        public FetchStockMediaListPayload(@NonNull String searchTerm, int page) {
            this.searchTerm = searchTerm;
            this.page = page;
        }
    }

    /**
     * Actions: FETCHED_MEDIA_LIST
     */
    public static class FetchedStockMediaListPayload extends Payload<MediaStore.MediaError> {
        public boolean canLoadMore;
        public int nextPage;
        public String searchTerm;
        public List<StockMediaModel> mediaList;

        public FetchedStockMediaListPayload(@NonNull List<StockMediaModel> mediaList,
                                            @NonNull String searchTerm,
                                            int nextPage,
                                            boolean canLoadMore) {
            this.mediaList = mediaList;
            this.searchTerm = searchTerm;
            this.canLoadMore = canLoadMore;
            this.nextPage = nextPage;
        }

        public FetchedStockMediaListPayload(@NonNull MediaStore.MediaError error) {
            this.error = error;
        }
    }

    public static class OnStockMediaListFetched extends OnChanged<MediaStore.MediaError> {
        public boolean canLoadMore;
        public int nextPage;
        public String searchTerm;
        public List<StockMediaModel> mediaList;

        public OnStockMediaListFetched(@NonNull List<StockMediaModel> mediaList,
                                       @NonNull String searchTerm,
                                       int nextPage,
                                       boolean canLoadMore) {
            this.mediaList = mediaList;
            this.searchTerm = searchTerm;
            this.canLoadMore = canLoadMore;
            this.nextPage = nextPage;
        }
        public OnStockMediaListFetched(MediaStore.MediaError error) {
            this.error = error;
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof StockMediaAction)) {
            return;
        }

        switch ((StockMediaAction) actionType) {
            case FETCH_STOCK_MEDIA:
                performFetchStockMediaList((StockMediaStore.FetchStockMediaListPayload) action.getPayload());
                break;
            case FETCHED_STOCK_MEDIA:
                handleStockMediaListFetched((StockMediaStore.FetchedStockMediaListPayload) action.getPayload());
                break;
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.MEDIA, "StockMediaStore onRegister");
    }

    private void performFetchStockMediaList(StockMediaStore.FetchStockMediaListPayload payload) {
        mMediaRestClient.searchStockMedia(payload.searchTerm, payload.number, payload.page);
    }

    private void handleStockMediaListFetched(@NonNull StockMediaStore.FetchedStockMediaListPayload payload) {
        OnStockMediaListFetched onStockMediaListFetched;

        if (payload.isError()) {
            onStockMediaListFetched = new OnStockMediaListFetched(payload.error);
        } else {
            onStockMediaListFetched = new OnStockMediaListFetched(
                    payload.mediaList,
                    payload.searchTerm,
                    payload.nextPage,
                    payload.canLoadMore);
        }

        emitChange(onStockMediaListFetched);
    }
}
