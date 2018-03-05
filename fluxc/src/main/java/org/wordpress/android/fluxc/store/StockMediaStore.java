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
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StockMediaStore extends Store {
    private final StockMediaRestClient mStockMediaRestClient;

    @Inject
    public StockMediaStore(Dispatcher dispatcher, StockMediaRestClient restClient) {
        super(dispatcher);
        mStockMediaRestClient = restClient;
    }

    /**
     * Actions: FETCH_MEDIA_LIST
     */
    public static class FetchStockMediaListPayload extends Payload<BaseNetworkError> {
        @NonNull public final String searchTerm;
        public final int page;

        public FetchStockMediaListPayload(@NonNull String searchTerm, int page) {
            this.searchTerm = searchTerm;
            this.page = page;
        }
    }

    /**
     * Actions: FETCHED_MEDIA_LIST
     */
    public static class FetchedStockMediaListPayload extends Payload<StockMediaError> {
        @NonNull public String searchTerm;
        @NonNull public List<StockMediaModel> mediaList;
        public boolean canLoadMore;
        public int nextPage;

        public FetchedStockMediaListPayload(@NonNull List<StockMediaModel> mediaList,
                                            @NonNull String searchTerm,
                                            int nextPage,
                                            boolean canLoadMore) {
            this.mediaList = mediaList;
            this.searchTerm = searchTerm;
            this.canLoadMore = canLoadMore;
            this.nextPage = nextPage;
        }

        public FetchedStockMediaListPayload(@NonNull StockMediaError error) {
            this.error = error;
        }
    }

    public static class OnStockMediaListFetched extends OnChanged<StockMediaError> {
        @NonNull public String searchTerm;
        @NonNull public List<StockMediaModel> mediaList;
        public boolean canLoadMore;
        public int nextPage;

        public OnStockMediaListFetched(@NonNull List<StockMediaModel> mediaList,
                                       @NonNull String searchTerm,
                                       int nextPage,
                                       boolean canLoadMore) {
            this.mediaList = mediaList;
            this.searchTerm = searchTerm;
            this.canLoadMore = canLoadMore;
            this.nextPage = nextPage;
        }
        public OnStockMediaListFetched(StockMediaError error) {
            this.error = error;
        }
    }

    public enum StockMediaErrorType {
        AUTHORIZATION_REQUIRED,
        CONNECTION_ERROR,
        NOT_AUTHENTICATED,
        NOT_FOUND,
        PARSE_ERROR,
        REQUEST_TOO_LARGE,
        SERVER_ERROR,
        TIMEOUT,
        GENERIC_ERROR;

        public static StockMediaErrorType fromBaseNetworkError(BaseNetworkError baseError) {
            switch (baseError.type) {
                case NOT_FOUND:
                    return StockMediaErrorType.NOT_FOUND;
                case NOT_AUTHENTICATED:
                    return StockMediaErrorType.NOT_AUTHENTICATED;
                case AUTHORIZATION_REQUIRED:
                    return StockMediaErrorType.AUTHORIZATION_REQUIRED;
                case PARSE_ERROR:
                    return StockMediaErrorType.PARSE_ERROR;
                case SERVER_ERROR:
                    return StockMediaErrorType.SERVER_ERROR;
                case TIMEOUT:
                    return StockMediaErrorType.TIMEOUT;
                default:
                    return StockMediaErrorType.GENERIC_ERROR;
            }
        }
    }

    public static class StockMediaError implements OnChangedError {
        public StockMediaErrorType type;
        public String message;
        public StockMediaError(StockMediaErrorType type) {
            this.type = type;
        }
        public StockMediaError(StockMediaErrorType type, String message) {
            this.type = type;
            this.message = message;
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
        mStockMediaRestClient.searchStockMedia(payload.searchTerm, payload.page);
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
