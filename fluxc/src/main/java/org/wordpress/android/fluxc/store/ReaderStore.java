package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.ReaderAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.reader.ReaderRestClient;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ReaderStore extends Store {
    private ReaderRestClient mReaderRestClient;

    @Inject
    public ReaderStore(Dispatcher dispatcher, ReaderRestClient readerRestClient) {
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
                break;
            case READER_SEARCHED_SITES:
                break;
        }
    }

    public static class ReaderSearchSitesPayload extends Payload<BaseNetworkError> {
        public @NonNull String searchTerm;
        public int offset;

        public ReaderSearchSitesPayload(@NonNull String searchTerm) {
            this.searchTerm = searchTerm;
        }

        public ReaderSearchSitesPayload(@NonNull String searchTerm, int offset) {
            this(searchTerm);
            this.offset = offset;
        }
    }

    public static class ReaderSearchSitesResponsePayload extends Payload<ReaderError> {
        public @NonNull SitesModel sites;
        public @NonNull String searchTerm;
        public int offset;
        public boolean canLoadMore;

        public ReaderSearchSitesResponsePayload(@NonNull SitesModel sites,
                                                @NonNull String searchTerm,
                                                int offset,
                                                boolean canLoadMore) {
            this.sites = sites;
            this.searchTerm = searchTerm;
            this.offset = offset;
            this.canLoadMore = canLoadMore;
        }

        public ReaderSearchSitesResponsePayload(@NonNull ReaderError error, @NonNull String searchTerm) {
            this.searchTerm = searchTerm;
            this.error = error;
            this.sites = new SitesModel();
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
}
