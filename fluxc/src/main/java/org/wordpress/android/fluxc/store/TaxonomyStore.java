package org.wordpress.android.fluxc.store;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.TaxonomyAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TaxonomyModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.model.TermsModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.taxonomy.TaxonomyXMLRPCClient;
import org.wordpress.android.fluxc.persistence.TaxonomySqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaxonomyStore extends Store {
    public static final String DEFAULT_TAXONOMY_CATEGORY = "category";
    public static final String DEFAULT_TAXONOMY_TAG = "post_tag";

    public static class FetchTermsPayload extends Payload<BaseNetworkError> {
        @NonNull public SiteModel site;
        @NonNull public TaxonomyModel taxonomy;

        public FetchTermsPayload(@NonNull SiteModel site, @NonNull TaxonomyModel taxonomy) {
            this.site = site;
            this.taxonomy = taxonomy;
        }
    }

    @SuppressWarnings("NotNullFieldNotInitialized")
    public static class FetchTermsResponsePayload extends Payload<TaxonomyError> {
        @NonNull public TermsModel terms;
        @NonNull public SiteModel site;
        @NonNull public String taxonomy; // This field is also included in error payload.

        public FetchTermsResponsePayload(@NonNull TermsModel terms, @NonNull SiteModel site, @NonNull String taxonomy) {
            this.terms = terms;
            this.site = site;
            this.taxonomy = taxonomy;
        }

        public FetchTermsResponsePayload(@NonNull TaxonomyError error, @NonNull String taxonomy) {
            this.error = error;
            this.taxonomy = taxonomy;
        }
    }

    public static class RemoteTermPayload extends Payload<TaxonomyError> {
        @NonNull public TermModel term;
        @NonNull public SiteModel site;

        public RemoteTermPayload(@NonNull TermModel term, @NonNull SiteModel site) {
            this.term = term;
            this.site = site;
        }
    }

    public static class FetchTermResponsePayload extends RemoteTermPayload {
        // Used to track fetching newly uploaded XML-RPC terms
        @NonNull public TaxonomyAction origin = TaxonomyAction.FETCH_TERM;

        public FetchTermResponsePayload(@NonNull TermModel term, @NonNull SiteModel site) {
            super(term, site);
        }
    }

    // OnChanged events
    public static class OnTaxonomyChanged extends OnChanged<TaxonomyError> {
        public int rowsAffected;
        public String taxonomyName;
        public TaxonomyAction causeOfChange;

        public OnTaxonomyChanged(int rowsAffected, String taxonomyName) {
            this.rowsAffected = rowsAffected;
            this.taxonomyName = taxonomyName;
        }

        public OnTaxonomyChanged(int rowsAffected) {
            this.rowsAffected = rowsAffected;
        }
    }

    public static class OnTermUploaded extends OnChanged<TaxonomyError> {
        @NonNull public TermModel term;

        public OnTermUploaded(@NonNull TermModel term) {
            this.term = term;
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
        INVALID_TAXONOMY,
        DUPLICATE,
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

    @Inject public TaxonomyStore(Dispatcher dispatcher, TaxonomyRestClient taxonomyRestClient,
                         TaxonomyXMLRPCClient taxonomyXMLRPCClient) {
        super(dispatcher);
        mTaxonomyRestClient = taxonomyRestClient;
        mTaxonomyXMLRPCClient = taxonomyXMLRPCClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "TaxonomyStore onRegister");
    }

    @Nullable
    public TermModel instantiateCategory(@NonNull SiteModel site) {
        return instantiateTermModel(site, DEFAULT_TAXONOMY_CATEGORY);
    }

    @Nullable
    public TermModel instantiateTag(@NonNull SiteModel site) {
        return instantiateTermModel(site, DEFAULT_TAXONOMY_TAG);
    }

    @Nullable
    public TermModel instantiateTerm(@NonNull SiteModel site, @NonNull TaxonomyModel taxonomy) {
        return instantiateTermModel(site, taxonomy.getName());
    }

    @Nullable
    private TermModel instantiateTermModel(@NonNull SiteModel site, @NonNull String taxonomyName) {
        TermModel newTerm = new TermModel();
        newTerm.setLocalSiteId(site.getId());
        newTerm.setTaxonomy(taxonomyName);

        // Insert the term into the db, updating the object to include the local ID
        newTerm = TaxonomySqlUtils.insertTermForResult(newTerm);

        // id is set to -1 if insertion fails
        if (newTerm.getId() == -1) {
            return null;
        }
        return newTerm;
    }

    /**
     * Returns all categories for the given site as a {@link TermModel} list.
     */
    @NonNull
    public List<TermModel> getCategoriesForSite(@NonNull SiteModel site) {
        return TaxonomySqlUtils.getTermsForSite(site, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns all tags for the given site as a {@link TermModel} list.
     */
    @NonNull
    public List<TermModel> getTagsForSite(@NonNull SiteModel site) {
        return TaxonomySqlUtils.getTermsForSite(site, DEFAULT_TAXONOMY_TAG);
    }

    /**
     * Returns all the terms of a taxonomy for the given site as a {@link TermModel} list.
     */
    @NonNull
    public List<TermModel> getTermsForSite(@NonNull SiteModel site, @NonNull String taxonomyName) {
        return TaxonomySqlUtils.getTermsForSite(site, taxonomyName);
    }

    /**
     * Returns a category as a {@link TermModel} given its remote id.
     */
    @Nullable
    public TermModel getCategoryByRemoteId(@NonNull SiteModel site, long remoteId) {
        return TaxonomySqlUtils.getTermByRemoteId(site, remoteId, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns a tag as a {@link TermModel} given its remote id.
     */
    @Nullable
    public TermModel getTagByRemoteId(@NonNull SiteModel site, long remoteId) {
        return TaxonomySqlUtils.getTermByRemoteId(site, remoteId, DEFAULT_TAXONOMY_TAG);
    }

    /**
     * Returns a term as a {@link TermModel} given its remote id.
     */
    @Nullable
    public TermModel getTermByRemoteId(@NonNull SiteModel site, long remoteId, @NonNull String taxonomyName) {
        return TaxonomySqlUtils.getTermByRemoteId(site, remoteId, taxonomyName);
    }

    /**
     * Returns a category as a {@link TermModel} given its name.
     */
    @Nullable
    public TermModel getCategoryByName(@NonNull SiteModel site, @NonNull String categoryName) {
        return TaxonomySqlUtils.getTermByName(site, categoryName, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns a tag as a {@link TermModel} given its name.
     */
    @Nullable
    public TermModel getTagByName(@NonNull SiteModel site, @NonNull String tagName) {
        return TaxonomySqlUtils.getTermByName(site, tagName, DEFAULT_TAXONOMY_TAG);
    }

    /**
     * Returns a term as a {@link TermModel} given its name.
     */
    @Nullable
    public TermModel getTermByName(@NonNull SiteModel site, @NonNull String termName, @NonNull String taxonomyName) {
        return TaxonomySqlUtils.getTermByName(site, termName, taxonomyName);
    }

    /**
     * Returns all the categories for the given post as a {@link TermModel} list.
     */
    @NonNull
    public List<TermModel> getCategoriesForPost(@NonNull PostImmutableModel post, @NonNull SiteModel site) {
        return TaxonomySqlUtils.getTermsFromRemoteIdList(post.getCategoryIdList(), site, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns all the tags for the given post as a {@link TermModel} list.
     */
    @NonNull
    public List<TermModel> getTagsForPost(@NonNull PostImmutableModel post, @NonNull SiteModel site) {
        return TaxonomySqlUtils.getTermsFromRemoteNameList(post.getTagNameList(), site, DEFAULT_TAXONOMY_TAG);
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
            case FETCH_TERM:
                fetchTerm((RemoteTermPayload) action.getPayload());
                break;
            case FETCHED_TERM:
                handleFetchSingleTermCompleted((FetchTermResponsePayload) action.getPayload());
                break;
            case PUSH_TERM:
                pushTerm((RemoteTermPayload) action.getPayload());
                break;
            case PUSHED_TERM:
                handlePushTermCompleted((RemoteTermPayload) action.getPayload());
                break;
            case DELETE_TERM:
                deleteTerm((RemoteTermPayload) action.getPayload());
                break;
            case DELETED_TERM:
                handleDeleteTermCompleted((RemoteTermPayload) action.getPayload());
                break;
            case REMOVE_ALL_TERMS:
                removeAllTerms();
                break;
        }
    }

    private void fetchTerm(@NonNull RemoteTermPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mTaxonomyRestClient.fetchTerm(payload.term, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mTaxonomyXMLRPCClient.fetchTerm(payload.term, payload.site);
        }
    }

    private void fetchTerms(@NonNull SiteModel site, @NonNull String taxonomyName) {
        // TODO: Support large number of terms (currently pulling 100 from REST, and ? from XML-RPC) - pagination?
        if (site.isUsingWpComRestApi()) {
            mTaxonomyRestClient.fetchTerms(site, taxonomyName);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
             mTaxonomyXMLRPCClient.fetchTerms(site, taxonomyName);
        }
    }

    private void fetchTerms(@NonNull FetchTermsPayload payload) {
        fetchTerms(payload.site, payload.taxonomy.getName());
    }

    private void handleFetchTermsCompleted(@NonNull FetchTermsResponsePayload payload) {
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

    private void handleFetchSingleTermCompleted(FetchTermResponsePayload payload) {
        if (payload.origin == TaxonomyAction.PUSH_TERM) {
            OnTermUploaded onTermUploaded = new OnTermUploaded(payload.term);
            if (payload.isError()) {
                onTermUploaded.error = payload.error;
            } else {
                updateTerm(payload.term);
            }
            emitChange(onTermUploaded);
            return;
        }

        if (payload.isError()) {
            OnTaxonomyChanged event = new OnTaxonomyChanged(0, payload.term.getTaxonomy());
            event.error = payload.error;
            event.causeOfChange = TaxonomyAction.UPDATE_TERM;
            emitChange(event);
        } else {
            updateTerm(payload.term);
        }
    }

    private void handleDeleteTermCompleted(RemoteTermPayload payload) {
        if (payload.isError()) {
            OnTaxonomyChanged event = new OnTaxonomyChanged(0, payload.term.getTaxonomy());
            event.error = payload.error;
            event.causeOfChange = TaxonomyAction.DELETE_TERM;
            emitChange(event);
        } else {
            removeTerm(payload.term);
        }
    }

    private void handlePushTermCompleted(RemoteTermPayload payload) {
        if (payload.isError()) {
            OnTermUploaded onTermUploaded = new OnTermUploaded(payload.term);
            onTermUploaded.error = payload.error;
            emitChange(onTermUploaded);
        } else {
            if (payload.site.isUsingWpComRestApi()) {
                // The WP.COM REST API response contains the modified term, so we're already in sync with the server
                // All we need to do is store it and emit OnTaxonomyChanged
                updateTerm(payload.term);
                emitChange(new OnTermUploaded(payload.term));
            } else {
                // XML-RPC does not respond to new/edit term calls with the resulting term - request it from the server
                // This needs to complete for us to obtain the slug for a newly created term
                TaxonomySqlUtils.insertOrUpdateTerm(payload.term);
                mTaxonomyXMLRPCClient.fetchTerm(payload.term, payload.site, TaxonomyAction.PUSH_TERM);
            }
        }
    }

    private void pushTerm(@NonNull RemoteTermPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mTaxonomyRestClient.pushTerm(payload.term, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mTaxonomyXMLRPCClient.pushTerm(payload.term, payload.site);
        }
    }

    private void deleteTerm(@NonNull RemoteTermPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mTaxonomyRestClient.deleteTerm(payload.term, payload.site);
        } else {
            mTaxonomyXMLRPCClient.deleteTerm(payload.term, payload.site);
        }
    }

    private void updateTerm(@NonNull TermModel term) {
        int rowsAffected = TaxonomySqlUtils.insertOrUpdateTerm(term);

        OnTaxonomyChanged onTaxonomyChanged = new OnTaxonomyChanged(rowsAffected, term.getTaxonomy());
        onTaxonomyChanged.causeOfChange = TaxonomyAction.UPDATE_TERM;
        emitChange(onTaxonomyChanged);
    }

    private void removeTerm(@NonNull TermModel term) {
        int rowsAffected = TaxonomySqlUtils.removeTerm(term);

        OnTaxonomyChanged onTaxonomyChanged = new OnTaxonomyChanged(rowsAffected, term.getTaxonomy());
        onTaxonomyChanged.causeOfChange = TaxonomyAction.REMOVE_TERM;
        emitChange(onTaxonomyChanged);
    }

    private void removeAllTerms() {
        int rowsAffected = TaxonomySqlUtils.deleteAllTerms();

        OnTaxonomyChanged onTaxonomyChanged = new OnTaxonomyChanged(rowsAffected);
        onTaxonomyChanged.causeOfChange = TaxonomyAction.REMOVE_ALL_TERMS;
        emitChange(onTaxonomyChanged);
    }
}
