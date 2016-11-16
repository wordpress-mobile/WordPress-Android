package org.wordpress.android.fluxc.network.rest.wpcom.taxonomy;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.model.TermsModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TermWPComRestResponse.TermsResponse;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaxonomyRestClient extends BaseWPComRestClient {
    @Inject
    public TaxonomyRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                              AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchTerm(final TermModel term, final SiteModel site) {
        final String taxonomy = term.getTaxonomy();
        final String slug = term.getSlug();
        String url = WPCOMREST.sites.site(site.getSiteId()).taxonomies.taxonomy(taxonomy).terms.slug(slug).getUrlV1_1();

        final WPComGsonRequest request = WPComGsonRequest.buildGetRequest(url, null,
                TermWPComRestResponse.class,
                new Listener<TermWPComRestResponse>() {
                    @Override
                    public void onResponse(TermWPComRestResponse response) {
                        TermModel fetchedTerm = termResponseToTermModel(response);
                        fetchedTerm.setId(term.getId());
                        fetchedTerm.setTaxonomy(taxonomy);
                        fetchedTerm.setLocalSiteId(site.getId());

                        FetchTermResponsePayload payload = new FetchTermResponsePayload(fetchedTerm, site);
                        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // Possible non-generic errors: 400 invalid_taxonomy
                        TaxonomyError taxonomyError = new TaxonomyError(((WPComGsonNetworkError) error).apiError,
                                error.message);
                        FetchTermResponsePayload payload = new FetchTermResponsePayload(term, site);
                        payload.error = taxonomyError;
                        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermAction(payload));
                    }
                }
        );
        add(request);
    }

    public void fetchTerms(final SiteModel site, final String taxonomyName) {
        String url = WPCOMREST.sites.site(site.getSiteId()).taxonomies.taxonomy(taxonomyName).terms.getUrlV1_1();

        final WPComGsonRequest request = WPComGsonRequest.buildGetRequest(url, null,
                TermsResponse.class,
                new Listener<TermsResponse>() {
                    @Override
                    public void onResponse(TermsResponse response) {
                        List<TermModel> termArray = new ArrayList<>();
                        TermModel term;
                        for (TermWPComRestResponse termResponse : response.terms) {
                            term = termResponseToTermModel(termResponse);
                            term.setTaxonomy(taxonomyName);
                            term.setLocalSiteId(site.getId());
                            termArray.add(term);
                        }

                        FetchTermsResponsePayload payload = new FetchTermsResponsePayload(new TermsModel(termArray),
                                site, taxonomyName);
                        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // Possible non-generic errors: 400 invalid_taxonomy
                        TaxonomyError taxonomyError = new TaxonomyError(((WPComGsonNetworkError) error).apiError,
                                error.message);
                        FetchTermsResponsePayload payload = new FetchTermsResponsePayload(taxonomyError, taxonomyName);
                        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload));
                    }
                }
        );
        add(request);
    }

    private TermModel termResponseToTermModel(TermWPComRestResponse from) {
        TermModel term = new TermModel();
        term.setRemoteTermId(from.ID);
        term.setName(from.name);
        term.setSlug(from.slug);
        term.setDescription(from.description);
        term.setParentRemoteId(from.parent);

        return term;
    }
}
