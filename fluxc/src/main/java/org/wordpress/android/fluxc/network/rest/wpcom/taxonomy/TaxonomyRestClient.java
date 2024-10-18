package org.wordpress.android.fluxc.network.rest.wpcom.taxonomy;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST.SitesEndpoint.SiteEndpoint.TaxonomiesEndpoint;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.model.TermsModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TermWPComRestResponse.TermsResponse;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TaxonomyRestClient extends BaseWPComRestClient {
    @Inject public TaxonomyRestClient(
            Context appContext,
            Dispatcher dispatcher,
            @Named("regular") RequestQueue requestQueue,
            AccessToken accessToken,
            UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchTerm(@NonNull final TermModel term, @NonNull final SiteModel site) {
        final String taxonomy = term.getTaxonomy();
        final String slug = term.getSlug();
        String url = WPCOMREST.sites.site(site.getSiteId()).taxonomies.taxonomy(taxonomy).terms.slug(slug).getUrlV1_1();

        final WPComGsonRequest<TermWPComRestResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                TermWPComRestResponse.class,
                response -> {
                    TermModel fetchedTerm = termResponseToTermModel(
                            term.getId(),
                            site.getId(),
                            taxonomy,
                            response);
                    FetchTermResponsePayload payload = new FetchTermResponsePayload(fetchedTerm, site);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermAction(payload));
                },
                error -> {
                    // Possible non-generic errors: 400 invalid_taxonomy
                    TaxonomyError taxonomyError = new TaxonomyError(error.apiError, error.message);
                    FetchTermResponsePayload payload = new FetchTermResponsePayload(term, site);
                    payload.error = taxonomyError;
                    mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermAction(payload));
                });
        add(request);
    }

    public void fetchTerms(@NonNull final SiteModel site, @NonNull final String taxonomyName) {
        String url = WPCOMREST.sites.site(site.getSiteId()).taxonomies.taxonomy(taxonomyName).terms.getUrlV1_1();

        Map<String, String> params = new HashMap<>();
        params.put("number", "1000");

        final WPComGsonRequest<TermsResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                TermsResponse.class,
                response -> {
                    List<TermModel> termArray = new ArrayList<>();
                    TermModel term;
                    for (TermWPComRestResponse termResponse : response.terms) {
                        term = termResponseToTermModel(
                                0,
                                site.getId(),
                                taxonomyName,
                                termResponse);
                        termArray.add(term);
                    }

                    FetchTermsResponsePayload payload = new FetchTermsResponsePayload(new TermsModel(termArray),
                            site, taxonomyName);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload));
                },
                error -> {
                    // Possible non-generic errors: 400 invalid_taxonomy
                    TaxonomyError taxonomyError = new TaxonomyError(error.apiError, error.message);
                    FetchTermsResponsePayload payload = new FetchTermsResponsePayload(taxonomyError, taxonomyName);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload));
                });
        add(request);
    }

    public void pushTerm(@NonNull final TermModel term, @NonNull final SiteModel site) {
        final String taxonomy = term.getTaxonomy();
        TaxonomiesEndpoint endpoint = WPCOMREST.sites.site(site.getSiteId()).taxonomies;
        String url = term.getRemoteTermId() > 0
                ? endpoint.taxonomy(taxonomy).terms.slug(term.getSlug()).getUrlV1_1() // update existing term
                : endpoint.taxonomy(taxonomy).terms.new_.getUrlV1_1(); // upload new term

        Map<String, Object> body = termModelToParams(term);

        final WPComGsonRequest<TermWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, body,
                TermWPComRestResponse.class,
                response -> {
                    TermModel uploadedTerm = termResponseToTermModel(
                            term.getId(),
                            site.getId(),
                            taxonomy,
                            response);
                    RemoteTermPayload payload = new RemoteTermPayload(uploadedTerm, site);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload));
                },
                error -> {
                    // Possible non-generic errors: 400 invalid_taxonomy, 409 duplicate
                    RemoteTermPayload payload = new RemoteTermPayload(term, site);
                    payload.error = new TaxonomyError(error.apiError, error.message);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload));
                });

        request.addQueryParameter("context", "edit");

        request.disableRetries();
        add(request);
    }

    public void deleteTerm(@NonNull final TermModel term, @NonNull final SiteModel site) {
        final String taxonomy = term.getTaxonomy();
        String url = WPCOMREST.sites.site(site.getSiteId()).taxonomies.taxonomy(taxonomy).terms
                .slug(term.getSlug()).delete.getUrlV1_1();

        final WPComGsonRequest<TermWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                TermWPComRestResponse.class,
                response -> {
                    RemoteTermPayload payload = new RemoteTermPayload(term, site);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newDeletedTermAction(payload));
                },
                error -> {
                    // Possible non-generic errors: 400 invalid_taxonomy, 409 duplicate
                    RemoteTermPayload payload = new RemoteTermPayload(term, site);
                    payload.error = new TaxonomyError(error.apiError, error.message);
                    mDispatcher.dispatch(TaxonomyActionBuilder.newDeletedTermAction(payload));
                });

        request.disableRetries();
        add(request);
    }

    @NonNull
    private TermModel termResponseToTermModel(
            int termId,
            int siteId,
            @NonNull String taxonomy,
            @NonNull TermWPComRestResponse from) {
        return new TermModel(
                termId,
                siteId,
                from.ID,
                taxonomy,
                StringEscapeUtils.unescapeHtml4(from.name),
                from.slug,
                StringEscapeUtils.unescapeHtml4(from.description),
                from.parent,
                from.post_count
        );
    }

    @NonNull
    private Map<String, Object> termModelToParams(@NonNull TermModel term) {
        Map<String, Object> body = new HashMap<>();

        body.put("name", term.getName());
        body.put("description", StringUtils.notNullStr(term.getDescription()));
        body.put("parent", String.valueOf(term.getParentRemoteId()));

        return body;
    }
}
