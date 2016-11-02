package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.TaxonomyAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermsModel;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.taxonomy.TaxonomyXMLRPCClient;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;

public class TaxonomyStore extends Store {
    public static class FetchTermsPayload extends Payload {
        public SiteModel site;
        public String taxonomy;

        public FetchTermsPayload(SiteModel site, String taxonomy) {
            this.site = site;
            this.taxonomy = taxonomy;
        }
    }

    public static class FetchTermsResponsePayload extends Payload {
        public TaxonomyError error;
        public TermsModel terms;
        public SiteModel site;
        public String taxonomy;

        public FetchTermsResponsePayload(TermsModel terms, SiteModel site, String taxonomy) {
            this.terms = terms;
            this.site = site;
            this.taxonomy = taxonomy;
        }

        public FetchTermsResponsePayload(TaxonomyError error, String taxonomy) {
            this.error = error;
            this.taxonomy = taxonomy;
        }

        @Override
        public boolean isError() {
            return error != null;
        }
    }

    public static class TaxonomyError implements OnChangedError {
        public TaxonomyErrorType type;
        public String message;

        public TaxonomyError(TaxonomyErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }

        public TaxonomyError(@NonNull String type, @NonNull String message) {
            this.type = TaxonomyErrorType.fromString(type);
            this.message = message;
        }

        public TaxonomyError(TaxonomyErrorType type) {
            this(type, "");
        }
    }

    public enum TaxonomyErrorType {
        // TODO: Fill in
        INVALID_RESPONSE,
        GENERIC_ERROR;

        public static TaxonomyErrorType fromString(String string) {
            if (string != null) {
                for (TaxonomyErrorType v : TaxonomyErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    private final TaxonomyRestClient mTaxonomyRestClient;
    private final TaxonomyXMLRPCClient mTaxonomyXMLRPCClient;

    @Inject
    public TaxonomyStore(Dispatcher dispatcher, TaxonomyRestClient taxonomyRestClient,
                         TaxonomyXMLRPCClient taxonomyXMLRPCClient) {
        super(dispatcher);
        mTaxonomyRestClient = taxonomyRestClient;
        mTaxonomyXMLRPCClient = taxonomyXMLRPCClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "TaxonomyStore onRegister");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof TaxonomyAction)) {
            return;
        }

        switch ((TaxonomyAction) actionType) {
            case FETCH_CATEGORIES:
                break;
            case FETCH_TAGS:
                break;
            case FETCH_TERMS:
                break;
            case FETCHED_TERMS:
                break;
        }
    }
}
