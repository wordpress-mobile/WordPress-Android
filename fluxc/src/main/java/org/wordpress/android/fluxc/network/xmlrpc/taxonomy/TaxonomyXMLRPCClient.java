package org.wordpress.android.fluxc.network.xmlrpc.taxonomy;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.TaxonomyAction;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.model.TermsModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class TaxonomyXMLRPCClient extends BaseXMLRPCClient {
    public TaxonomyXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, UserAgent userAgent,
                                HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    public void fetchTerm(final TermModel term, final SiteModel site) {
        fetchTerm(term, site, TaxonomyAction.FETCH_TERM);
    }

    public void fetchTerm(final TermModel term, final SiteModel site, final TaxonomyAction origin) {
        List<Object> params = new ArrayList<>(5);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(term.getTaxonomy());
        params.add(term.getRemoteTermId());

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_TERM, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                    if (response != null && response instanceof Map) {
                        TermModel termModel = termResponseObjectToTermModel(response, site);
                        FetchTermResponsePayload payload;
                        if (termModel != null) {
                            if (origin == TaxonomyAction.PUSH_TERM) {
                                termModel.setId(term.getId());
                            }
                            payload = new FetchTermResponsePayload(termModel, site);
                        } else {
                            payload = new FetchTermResponsePayload(term, site);
                            payload.error = new TaxonomyError(TaxonomyErrorType.INVALID_RESPONSE);
                        }
                        payload.origin = origin;

                        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermAction(payload));
                    }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // Possible non-generic errors:
                        // 403 - "Invalid taxonomy."
                        // 404 - "Invalid term ID."
                        FetchTermResponsePayload payload = new FetchTermResponsePayload(term, site);
                        // TODO: Check the error message and flag this as INVALID_TAXONOMY or UNKNOWN_TERM
                        // Convert GenericErrorType to TaxonomyErrorType where applicable
                        TaxonomyError taxonomyError;
                        switch (error.type) {
                            case AUTHORIZATION_REQUIRED:
                                taxonomyError = new TaxonomyError(TaxonomyErrorType.UNAUTHORIZED, error.message);
                                break;
                            default:
                                taxonomyError = new TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, error.message);
                        }
                        payload.error = taxonomyError;
                        payload.origin = origin;
                        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermAction(payload));
                    }
                }
        );

        add(request);
    }

    public void fetchTerms(final SiteModel site, final String taxonomyName) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(taxonomyName);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_TERMS, params,
                new Listener<Object[]>() {
                    @Override
                    public void onResponse(Object[] response) {
                        TermsModel terms = termsResponseToTermsModel(response, site);

                        FetchTermsResponsePayload payload = new FetchTermsResponsePayload(terms, site, taxonomyName);

                        if (terms != null) {
                            mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload));
                        } else {
                            payload.error = new TaxonomyError(TaxonomyErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // Possible non-generic errors:
                        // 403 - "Invalid taxonomy."
                        // TODO: Check the error message and flag this as INVALID_TAXONOMY if applicable
                        // Convert GenericErrorType to TaxonomyErrorType where applicable
                        TaxonomyError taxonomyError;
                        switch (error.type) {
                            case AUTHORIZATION_REQUIRED:
                                taxonomyError = new TaxonomyError(TaxonomyErrorType.UNAUTHORIZED, error.message);
                                break;
                            default:
                                taxonomyError = new TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, error.message);
                        }
                        FetchTermsResponsePayload payload = new FetchTermsResponsePayload(taxonomyError, taxonomyName);
                        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload));
                    }
                }
        );

        add(request);
    }

    public void pushTerm(final TermModel term, final SiteModel site) {
        Map<String, Object> contentStruct = termModelToContentStruct(term);
        final boolean updatingExistingTerm = term.getRemoteTermId() > 0;

        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        if (updatingExistingTerm) {
            params.add(term.getRemoteTermId());
        }
        params.add(contentStruct);

        XMLRPC method = updatingExistingTerm ? XMLRPC.EDIT_TERM : XMLRPC.NEW_TERM;
        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), method, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        // `term_id` is only returned for XMLRPC.NEW_TERM
                        if (!updatingExistingTerm) {
                            term.setRemoteTermId(Long.valueOf((String) response));
                        }

                        RemoteTermPayload payload = new RemoteTermPayload(term, site);
                        mDispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // Possible non-generic errors:
                        // 403 - "Invalid taxonomy."
                        // 403 - "Parent term does not exist."
                        // 403 - "The term name cannot be empty."
                        // 500 - "A term with the name provided already exists with this parent."
                        RemoteTermPayload payload = new RemoteTermPayload(term, site);
                        // TODO: Check the error message and flag this as one of the above specific errors if applicable
                        // Convert GenericErrorType to PostErrorType where applicable
                        TaxonomyError taxonomyError;
                        switch (error.type) {
                            case AUTHORIZATION_REQUIRED:
                                taxonomyError = new TaxonomyError(TaxonomyErrorType.UNAUTHORIZED, error.message);
                                break;
                            default:
                                taxonomyError = new TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, error.message);
                        }
                        payload.error = taxonomyError;
                        mDispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload));
                    }
                }
        );

        request.disableRetries();
        add(request);
    }

    public void deleteTerm(final TermModel term, final SiteModel site) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(term.getTaxonomy());
        params.add(term.getRemoteTermId());

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.DELETE_TERM, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        RemoteTermPayload payload = new RemoteTermPayload(term, site);
                        mDispatcher.dispatch(TaxonomyActionBuilder.newDeletedTermAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        RemoteTermPayload payload = new RemoteTermPayload(term, site);
                        TaxonomyError taxonomyError;
                        switch (error.type) {
                            case AUTHORIZATION_REQUIRED:
                                taxonomyError = new TaxonomyError(TaxonomyErrorType.UNAUTHORIZED, error.message);
                                break;
                            default:
                                taxonomyError = new TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, error.message);
                        }
                        payload.error = taxonomyError;
                        mDispatcher.dispatch(TaxonomyActionBuilder.newDeletedTermAction(payload));
                    }
                }
        );

        request.disableRetries();
        add(request);
    }

    private TermsModel termsResponseToTermsModel(Object[] response, SiteModel site) {
        List<Map<?, ?>> termsList = new ArrayList<>();
        for (Object responseObject : response) {
            Map<?, ?> termMap = (Map<?, ?>) responseObject;
            termsList.add(termMap);
        }

        List<TermModel> termArray = new ArrayList<>();
        TermModel term;

        for (Object termObject : termsList) {
            term = termResponseObjectToTermModel(termObject, site);
            if (term != null) {
                termArray.add(term);
            }
        }

        if (termArray.isEmpty()) {
            return null;
        }

        return new TermsModel(termArray);
    }

    private TermModel termResponseObjectToTermModel(Object termObject, SiteModel site) {
        // Sanity checks
        if (!(termObject instanceof Map)) {
            return null;
        }

        Map<?, ?> termMap = (Map<?, ?>) termObject;
        TermModel term = new TermModel();

        String termId = MapUtils.getMapStr(termMap, "term_id");
        if (TextUtils.isEmpty(termId)) {
            // If we don't have a term ID, move on
            return null;
        }

        term.setLocalSiteId(site.getId());
        term.setRemoteTermId(Integer.valueOf(termId));
        term.setSlug(MapUtils.getMapStr(termMap, "slug"));
        term.setName(StringEscapeUtils.unescapeHtml4(MapUtils.getMapStr(termMap, "name")));
        term.setDescription(StringEscapeUtils.unescapeHtml4(MapUtils.getMapStr(termMap, "description")));
        term.setParentRemoteId(MapUtils.getMapLong(termMap, "parent"));
        term.setTaxonomy(MapUtils.getMapStr(termMap, "taxonomy"));
        term.setPostCount(MapUtils.getMapInt(termMap, "count", 0));

        return term;
    }

    private static Map<String, Object> termModelToContentStruct(TermModel term) {
        Map<String, Object> contentStruct = new HashMap<>();

        contentStruct.put("name", term.getName());
        contentStruct.put("taxonomy", term.getTaxonomy());

        if (term.getSlug() != null) {
            contentStruct.put("slug", term.getSlug());
        }

        if (term.getDescription() != null) {
            contentStruct.put("description", term.getDescription());
        }

        if (term.getParentRemoteId() > 0) {
            contentStruct.put("parent", term.getParentRemoteId());
        }

        return contentStruct;
    }
}
