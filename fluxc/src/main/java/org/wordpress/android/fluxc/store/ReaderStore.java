package org.wordpress.android.fluxc.store;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.annotations.action.Action;
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

    @Override
    public void onAction(Action action) {
        // todo
    }
}
