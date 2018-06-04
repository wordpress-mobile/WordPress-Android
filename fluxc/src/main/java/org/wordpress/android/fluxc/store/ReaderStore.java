package org.wordpress.android.fluxc.store;

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
import org.wordpress.android.fluxc.store.PostStore.PostError;
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
        public String searchTerm;
        public int offset;

        public ReaderSearchSitesPayload(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        public ReaderSearchSitesPayload(String searchTerm, int offset) {
            this(searchTerm);
            this.offset = offset;
        }
    }

    public static class ReaderSearchSitesResponsePayload extends Payload<PostError> {
        public SitesModel sites;
        public String searchTerm;
        public boolean loadedMore;
        public boolean canLoadMore;

        public ReaderSearchSitesResponsePayload(SitesModel sites, String searchTerm,
                                                boolean loadedMore, boolean canLoadMore) {
            this.sites = sites;
            this.searchTerm = searchTerm;
            this.loadedMore = loadedMore;
            this.canLoadMore = canLoadMore;
        }

        public ReaderSearchSitesResponsePayload(String searchTerm, PostError error) {
            this.searchTerm = searchTerm;
            this.error = error;
        }
    }
}
