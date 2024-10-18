package org.wordpress.android.fluxc.store;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.ReaderAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.ReaderSiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.reader.ReaderRestClient;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ReaderStore extends Store {
    private ReaderRestClient mReaderRestClient;

    @Inject public ReaderStore(Dispatcher dispatcher, ReaderRestClient readerRestClient) {
        super(dispatcher);
        mReaderRestClient = readerRestClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "ReaderStore onRegister");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof ReaderAction)) {
            return;
        }
        switch ((ReaderAction) actionType) {
            case READER_SEARCH_SITES:
                performReaderSearchSites((ReaderSearchSitesPayload) action.getPayload());
                break;
            case READER_SEARCHED_SITES:
                handleReaderSearchedSites((ReaderSearchSitesResponsePayload) action.getPayload());
                break;
        }
    }

    public static class ReaderSearchSitesPayload extends Payload<BaseNetworkError> {
        public @NonNull String searchTerm;
        public int count;
        public int offset;
        public boolean excludeFollowed;

        public ReaderSearchSitesPayload(@NonNull String searchTerm, int count, int offset, boolean excludeFollowed) {
            this.searchTerm = searchTerm;
            this.count = count;
            this.offset = offset;
            this.excludeFollowed = excludeFollowed;
        }
    }

    public static class ReaderSearchSitesResponsePayload extends Payload<ReaderError> {
        public @NonNull List<ReaderSiteModel> sites;
        public @NonNull String searchTerm;
        public int offset;
        public boolean canLoadMore;

        public ReaderSearchSitesResponsePayload(@NonNull List<ReaderSiteModel> sites,
                                                @NonNull String searchTerm,
                                                int offset,
                                                boolean canLoadMore) {
            this.sites = sites;
            this.searchTerm = searchTerm;
            this.offset = offset;
            this.canLoadMore = canLoadMore;
        }

        public ReaderSearchSitesResponsePayload(@NonNull ReaderError error, @NonNull String searchTerm, int offset) {
            this.searchTerm = searchTerm;
            this.offset = offset;
            this.error = error;
            this.sites = new ArrayList<>();
        }
    }

    public enum ReaderErrorType {
        GENERIC_ERROR;

        public static ReaderErrorType fromBaseNetworkError(BaseNetworkError baseError) {
            return ReaderErrorType.GENERIC_ERROR;
        }
    }

    public static class ReaderError implements OnChangedError {
        public ReaderErrorType type;
        public String message;

        public ReaderError(ReaderErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class OnReaderSitesSearched extends OnChanged<ReaderError> {
        @NonNull public String searchTerm;
        @NonNull public List<ReaderSiteModel> sites;
        public boolean canLoadMore;
        public int offset;

        public OnReaderSitesSearched(@NonNull List<ReaderSiteModel> sites,
                                     @NonNull String searchTerm,
                                     int offset,
                                     boolean canLoadMore) {
            this.sites = sites;
            this.searchTerm = searchTerm;
            this.canLoadMore = canLoadMore;
            this.offset = offset;
        }

        public OnReaderSitesSearched(@NonNull ReaderError error, @NonNull String searchTerm, int offset) {
            this.error = error;
            this.searchTerm = searchTerm;
            this.offset = offset;
            this.sites = new ArrayList<>();
        }
    }

    private void performReaderSearchSites(ReaderSearchSitesPayload payload) {
        mReaderRestClient.searchReaderSites(payload.searchTerm, payload.count, payload.offset, payload.excludeFollowed);
    }

    private void handleReaderSearchedSites(@NonNull ReaderSearchSitesResponsePayload payload) {
        OnReaderSitesSearched onReaderSitesSearched;

        if (payload.isError()) {
            onReaderSitesSearched = new OnReaderSitesSearched(payload.error, payload.searchTerm, payload.offset);
        } else {
            onReaderSitesSearched = new OnReaderSitesSearched(
                    payload.sites,
                    payload.searchTerm,
                    payload.offset,
                    payload.canLoadMore);
        }

        emitChange(onReaderSitesSearched);
    }
}
