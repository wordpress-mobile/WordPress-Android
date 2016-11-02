package org.wordpress.android.fluxc.network.xmlrpc.taxonomy;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.model.TermsModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaxonomyXMLRPCClient extends BaseXMLRPCClient {
    public TaxonomyXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                                UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
    }

    public void fetchTerms(final SiteModel site, final String taxonomyName) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(taxonomyName);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_TERMS, params,
                new Response.Listener<Object[]>() {
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
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        // Possible non-generic errors:
                        // 403 - Invalid taxonomy
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
        term.setName(MapUtils.getMapStr(termMap, "name"));
        term.setDescription(MapUtils.getMapStr(termMap, "description"));
        term.setParentRemoteId(MapUtils.getMapLong(termMap, "parent"));
        term.setTaxonomy(MapUtils.getMapStr(termMap, "taxonomy"));

        return term;
    }
}
