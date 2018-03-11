package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.StockMediaAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.model.StockMediaUploadModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StockMediaStore extends Store {
    private final StockMediaRestClient mStockMediaRestClient;

    @SuppressWarnings("WeakerAccess")
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
    @SuppressWarnings("WeakerAccess")
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

        public FetchedStockMediaListPayload(@NonNull StockMediaError error, @NonNull String searchTerm) {
            this.error = error;
            this.mediaList = new ArrayList<>();
            this.searchTerm = searchTerm;
            this.canLoadMore = false;
        }
    }

    /**
     * Actions: UPLOAD_STOCK_MEDIA
     */
    public static class UploadStockMediaPayload extends Payload<BaseNetworkError> {
        @NonNull List<StockMediaModel> mediaList;
        public SiteModel site;

        public UploadStockMediaPayload(@NonNull SiteModel site, @NonNull List<StockMediaModel> mediaList) {
            this.mediaList = mediaList;
            this.site = site;
        }
    }

    /**
     * Actions: UPLOADED_STOCK_MEDIA
     */
    @SuppressWarnings("WeakerAccess")
    public static class UploadedStockMediaPayload extends Payload<StockMediaError> {
        @NonNull public List<StockMediaUploadModel> mediaList;
        @NonNull public SiteModel site;

        public UploadedStockMediaPayload(@NonNull SiteModel site, @NonNull List<StockMediaUploadModel> mediaList) {
            this.site = site;
            this.mediaList = mediaList;
        }

        public UploadedStockMediaPayload(@NonNull SiteModel site, @NonNull StockMediaError error) {
            this.site = site;
            this.error = error;
            this.mediaList = new ArrayList<>();
        }
    }

    @SuppressWarnings("WeakerAccess")
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
        public OnStockMediaListFetched(@NonNull StockMediaError error, @NonNull String searchTerm) {
            this.error = error;
            this.searchTerm = searchTerm;
            this.mediaList = new ArrayList<>();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnStockMediaUploaded extends OnChanged<StockMediaError> {
        @NonNull public List<StockMediaUploadModel> uploadedMedia;
        @Nullable public SiteModel site;

        public OnStockMediaUploaded(@NonNull SiteModel site, @NonNull List<StockMediaUploadModel> uploadedMedia) {
            this.site = site;
            this.uploadedMedia = uploadedMedia;
        }
        public OnStockMediaUploaded(@NonNull SiteModel site, @NonNull StockMediaError error) {
            this.site = site;
            this.error = error;
            this.uploadedMedia = new ArrayList<>();
        }
    }

    public enum StockMediaErrorType {
        // the endpoint for stock media search simply returns an empty list if there's any type of
        // error (timeout, authentication, etc.), so we use only a simple GENERIC_ERROR here
        GENERIC_ERROR;

        public static StockMediaErrorType fromBaseNetworkError(BaseNetworkError baseError) {
            return StockMediaErrorType.GENERIC_ERROR;
        }
    }

    public static class StockMediaError implements OnChangedError {
        public StockMediaErrorType type;
        public String message;
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
                performFetchStockMediaList((FetchStockMediaListPayload) action.getPayload());
                break;
            case FETCHED_STOCK_MEDIA:
                handleStockMediaListFetched((FetchedStockMediaListPayload) action.getPayload());
                break;
            case UPLOAD_STOCK_MEDIA:
                performUploadStockMedia((UploadStockMediaPayload) action.getPayload());
                break;
            case UPLOADED_STOCK_MEDIA:
                handleStockMediaUploaded(((UploadedStockMediaPayload) action.getPayload()));
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
            onStockMediaListFetched = new OnStockMediaListFetched(payload.error, payload.searchTerm);
        } else {
            onStockMediaListFetched = new OnStockMediaListFetched(
                    payload.mediaList,
                    payload.searchTerm,
                    payload.nextPage,
                    payload.canLoadMore);
        }

        emitChange(onStockMediaListFetched);
    }

    private void performUploadStockMedia(StockMediaStore.UploadStockMediaPayload payload) {
        mStockMediaRestClient.uploadStockMedia(payload.site, payload.mediaList);
    }

    private void handleStockMediaUploaded(StockMediaStore.UploadedStockMediaPayload payload) {
        OnStockMediaUploaded onStockMediaUploaded;

        if (payload.isError()) {
            onStockMediaUploaded = new OnStockMediaUploaded(payload.site, payload.error);
        } else {
            onStockMediaUploaded = new OnStockMediaUploaded(payload.site, payload.mediaList);
        }

        emitChange(onStockMediaUploaded);
    }
}
