package org.wordpress.android.fluxc.network.xmlrpc.taxonomy;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TaxonomyXMLRPCClient extends BaseXMLRPCClient {
    @Inject public TaxonomyXMLRPCClient(
            Dispatcher dispatcher,
            @Named("custom-ssl") RequestQueue requestQueue,
            UserAgent userAgent,
            HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    public void fetchTerm(@NonNull final TermModel term, @NonNull final SiteModel site) {
        fetchTerm(term, site, TaxonomyAction.FETCH_TERM);
    }

    public void fetchTerm(
            @NonNull final TermModel term,
            @NonNull final SiteModel site,
            @NonNull final TaxonomyAction origin) {
        List<Object> params = new ArrayList<>(5);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(term.getTaxonomy());
        params.add(term.getRemoteTermId());

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_TERM, params,
                (Listener<Object>) response -> {
                    if (response instanceof Map) {
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
                },
                error -> {
                    // Possible non-generic errors:
                    // 403 - "Invalid taxonomy."
                    // 404 - "Invalid term ID."
                    FetchTermResponsePayload payload = new FetchTermResponsePayload(term, site);
                    payload.error = getTaxonomyError(error);
                    payload.origin = origin;
                    mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermAction(payload));
                });

        add(request);
    }

    public void fetchTerms(@NonNull final SiteModel site, @NonNull final String taxonomyName) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(taxonomyName);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_TERMS, params,
                response -> {
                    FetchTermsResponsePayload payload;
                    TermsModel terms = termsResponseToTermsModel(response, site);
                    if (terms != null) {
                        payload = new FetchTermsResponsePayload(terms, site, taxonomyName);
                    } else {
                        payload = new FetchTermsResponsePayload(
                                new TaxonomyError(TaxonomyErrorType.INVALID_RESPONSE),
                                taxonomyName
                        );
                    }
                    mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload));
                },
                error -> {
                    // Possible non-generic errors:
                    // 403 - "Invalid taxonomy."
                    FetchTermsResponsePayload payload = new FetchTermsResponsePayload(
                            getTaxonomyError(error),
                            taxonomyName
                    );
                    mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload));
                });

        add(request);
    }

    public void pushTerm(@NonNull final TermModel term, @NonNull final SiteModel site) {
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
                (Listener<Object>) response -> {
                    // `term_id` is only returned for XMLRPC.NEW_TERM
                    if (!updatingExistingTerm) {
                        term.setRemoteTermId(Long.parseLong((String) response));
                    }

                    RemoteTermPayload payload = new RemoteTermPayload(term, site);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload));
                },
                error -> {
                    // Possible non-generic errors:
                    // 403 - "Invalid taxonomy."
                    // 403 - "Parent term does not exist."
                    // 403 - "The term name cannot be empty."
                    // 500 - "A term with the name provided already exists with this parent."
                    RemoteTermPayload payload = new RemoteTermPayload(term, site);
                    payload.error = getTaxonomyError(error);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload));
                });

        request.disableRetries();
        add(request);
    }

    public void deleteTerm(@NonNull final TermModel term, @NonNull final SiteModel site) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(term.getTaxonomy());
        params.add(term.getRemoteTermId());

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.DELETE_TERM, params,
                (Listener<Object>) response -> {
                    RemoteTermPayload payload = new RemoteTermPayload(term, site);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newDeletedTermAction(payload));
                },
                error -> {
                    RemoteTermPayload payload = new RemoteTermPayload(term, site);
                    payload.error = getTaxonomyError(error);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newDeletedTermAction(payload));
                });

        request.disableRetries();
        add(request);
    }

    @Nullable
    private TermsModel termsResponseToTermsModel(@NonNull Object[] response, @NonNull SiteModel site) {
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

    @Nullable
    private TermModel termResponseObjectToTermModel(@NonNull Object termObject, @NonNull SiteModel site) {
        // Sanity checks
        if (!(termObject instanceof Map)) {
            return null;
        }

        Map<?, ?> termMap = (Map<?, ?>) termObject;
        String termId = MapUtils.getMapStr(termMap, "term_id");
        if (TextUtils.isEmpty(termId)) {
            // If we don't have a term ID, move on
            return null;
        }

        return new TermModel(
                0,
                site.getId(),
                Long.parseLong(termId),
                MapUtils.getMapStr(termMap, "taxonomy"),
                StringEscapeUtils.unescapeHtml4(MapUtils.getMapStr(termMap, "name")),
                MapUtils.getMapStr(termMap, "slug"),
                StringEscapeUtils.unescapeHtml4(MapUtils.getMapStr(termMap, "description")),
                MapUtils.getMapLong(termMap, "parent"),
                MapUtils.getMapInt(termMap, "count", 0)
        );
    }

    @NonNull
    private static Map<String, Object> termModelToContentStruct(@NonNull TermModel term) {
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

    // TODO: Check the error message and flag this as a specific error if applicable.
    // Convert GenericErrorType to TaxonomyErrorType where applicable
    @NonNull
    private TaxonomyError getTaxonomyError(@NonNull BaseNetworkError error) {
        TaxonomyError taxonomyError;
        switch (error.type) {
            case AUTHORIZATION_REQUIRED:
                taxonomyError = new TaxonomyError(TaxonomyErrorType.UNAUTHORIZED, error.message);
                break;
            case TIMEOUT:
            case NO_CONNECTION:
            case NETWORK_ERROR:
            case NOT_FOUND:
            case CENSORED:
            case SERVER_ERROR:
            case INVALID_SSL_CERTIFICATE:
            case HTTP_AUTH_ERROR:
            case INVALID_RESPONSE:
            case NOT_AUTHENTICATED:
            case PARSE_ERROR:
            case UNKNOWN:
            default:
                taxonomyError = new TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, error.message);
        }
        return taxonomyError;
    }
}
