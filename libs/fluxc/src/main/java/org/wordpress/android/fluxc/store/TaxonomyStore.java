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
import org.wordpress.android.fluxc.model.TaxonomyModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.model.TermsModel;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.taxonomy.TaxonomyXMLRPCClient;
import org.wordpress.android.fluxc.persistence.TaxonomySqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;

public class TaxonomyStore extends Store {
    public static final String DEFAULT_TAXONOMY_CATEGORY = "category";
    public static final String DEFAULT_TAXONOMY_TAG = "post_tag";

    public static class FetchTermsPayload extends Payload {
        public SiteModel site;
        public TaxonomyModel taxonomy;

        public FetchTermsPayload(SiteModel site, TaxonomyModel taxonomy) {
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

    // OnChanged events
    public class OnTaxonomyChanged extends OnChanged<TaxonomyError> {
        public int rowsAffected;
        public String taxonomyName;
        public TaxonomyAction causeOfChange;

        public OnTaxonomyChanged(int rowsAffected, String taxonomyName) {
            this.rowsAffected = rowsAffected;
            this.taxonomyName = taxonomyName;
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
        INVALID_TAXONOMY,
        UNAUTHORIZED,
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

    /**
     * Returns all categories for the given site as a {@link TermModel} list.
     */
    public List<TermModel> getCategoriesForSite(SiteModel site) {
        return TaxonomySqlUtils.getTermsForSite(site, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns all tags for the given site as a {@link TermModel} list.
     */
    public List<TermModel> getTagsForSite(SiteModel site) {
        return TaxonomySqlUtils.getTermsForSite(site, DEFAULT_TAXONOMY_TAG);
    }

    /**
     * Returns all the terms of a taxonomy for the given site as a {@link TermModel} list.
     */
    public List<TermModel> getTermsForSite(SiteModel site, String taxonomyName) {
        return TaxonomySqlUtils.getTermsForSite(site, taxonomyName);
    }

    /**
     * Returns a category as a {@link TermModel} given its remote id.
     */
    public TermModel getCategoryByRemoteId(SiteModel site, long remoteId) {
        return TaxonomySqlUtils.getTermByRemoteId(site, remoteId, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns a category as a {@link TermModel} given its remote id.
     */
    public TermModel getTagByRemoteId(SiteModel site, long remoteId) {
        return TaxonomySqlUtils.getTermByRemoteId(site, remoteId, DEFAULT_TAXONOMY_TAG);
    }

    /**
     * Returns a category as a {@link TermModel} given its remote id.
     */
    public TermModel getTermByRemoteId(SiteModel site, long remoteId, String taxonomyName) {
        return TaxonomySqlUtils.getTermByRemoteId(site, remoteId, taxonomyName);
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
                fetchTerms(((SiteModel) action.getPayload()), DEFAULT_TAXONOMY_CATEGORY);
                break;
            case FETCH_TAGS:
                fetchTerms(((SiteModel) action.getPayload()), DEFAULT_TAXONOMY_TAG);
                break;
            case FETCH_TERMS:
                fetchTerms((FetchTermsPayload) action.getPayload());
                break;
            case FETCHED_TERMS:
                handleFetchTermsCompleted((FetchTermsResponsePayload) action.getPayload());
                break;
        }
    }

    private void fetchTerms(SiteModel site, String taxonomyName) {
        // TODO: Support large number of terms (currently pulling 100 from REST, and ? from XML-RPC) - pagination?
        if (site.isWPCom()) {
            mTaxonomyRestClient.fetchTerms(site, taxonomyName);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
             mTaxonomyXMLRPCClient.fetchTerms(site, taxonomyName);
        }
    }

    private void fetchTerms(FetchTermsPayload payload) {
        fetchTerms(payload.site, payload.taxonomy.getName());
    }

    private void handleFetchTermsCompleted(FetchTermsResponsePayload payload) {
        OnTaxonomyChanged onTaxonomyChanged;

        if (payload.isError()) {
            onTaxonomyChanged = new OnTaxonomyChanged(0, payload.taxonomy);
            onTaxonomyChanged.error = payload.error;
        } else {
            // Clear existing terms for this taxonomy
            // This is the simplest way of keeping our local terms in sync with their remote versions
            // (in case of deletions,or if the user manually changed some term IDs)
            // TODO: This may have to change when we support large numbers of terms and require multiple requests
            TaxonomySqlUtils.clearTaxonomyForSite(payload.site, payload.taxonomy);

            int rowsAffected = 0;
            for (TermModel term : payload.terms.getTerms()) {
                rowsAffected += TaxonomySqlUtils.insertOrUpdateTerm(term);
            }

            onTaxonomyChanged = new OnTaxonomyChanged(rowsAffected, payload.taxonomy);
        }

        switch (payload.taxonomy) {
            case DEFAULT_TAXONOMY_CATEGORY:
                onTaxonomyChanged.causeOfChange = TaxonomyAction.FETCH_CATEGORIES;
                break;
            case DEFAULT_TAXONOMY_TAG:
                onTaxonomyChanged.causeOfChange = TaxonomyAction.FETCH_TAGS;
                break;
            default:
                onTaxonomyChanged.causeOfChange = TaxonomyAction.FETCH_TERMS;
        }

        emitChange(onTaxonomyChanged);
    }
}
