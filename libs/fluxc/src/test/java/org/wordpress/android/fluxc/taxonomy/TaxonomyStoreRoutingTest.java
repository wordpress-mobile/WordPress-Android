package org.wordpress.android.fluxc.taxonomy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.network.rest.wpapi.taxonomy.TaxonomiesRestApiMigrationConfig;
import org.wordpress.android.fluxc.network.rest.wpapi.taxonomy.TaxonomyRsApiRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.taxonomy.TaxonomyXMLRPCClient;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY;

/**
 * Verifies that {@link TaxonomyStore} only routes self-hosted taxonomy operations to the new wp-rs
 * REST client when the {@code taxonomies_rest_api_migration} feature flag is enabled, and falls back
 * to the legacy XML-RPC client when it is disabled.
 */
@RunWith(RobolectricTestRunner.class)
public class TaxonomyStoreRoutingTest {
    private TaxonomyRestClient mRestClient;
    private TaxonomyXMLRPCClient mXmlRpcClient;
    private TaxonomyRsApiRestClient mRsApiRestClient;
    private TaxonomiesRestApiMigrationConfig mMigrationConfig;

    private TaxonomyStore mStore;

    @Before
    public void setUp() {
        mRestClient = mock(TaxonomyRestClient.class);
        mXmlRpcClient = mock(TaxonomyXMLRPCClient.class);
        mRsApiRestClient = mock(TaxonomyRsApiRestClient.class);
        mMigrationConfig = mock(TaxonomiesRestApiMigrationConfig.class);
        mStore = new TaxonomyStore(
                new Dispatcher(),
                mRestClient,
                mXmlRpcClient,
                mRsApiRestClient,
                mMigrationConfig
        );
    }

    private SiteModel selfHostedSite() {
        SiteModel site = new SiteModel();
        site.setId(6);
        site.setIsWPCom(false);
        site.setApiRestUsernamePlain("user");
        site.setApiRestPasswordPlain("pass");
        return site;
    }

    private TermModel sampleCategory() {
        return TaxonomyTestUtils.generateSampleCategory();
    }

    // region fetch
    @Test
    public void selfHostedFetchUsesRsApiClientWhenFlagEnabled() {
        when(mMigrationConfig.isEnabled()).thenReturn(true);
        SiteModel site = selfHostedSite();

        mStore.onAction(TaxonomyActionBuilder.newFetchCategoriesAction(site));

        verify(mRsApiRestClient).fetchTerms(site, DEFAULT_TAXONOMY_CATEGORY);
        verifyNoInteractions(mXmlRpcClient);
    }

    @Test
    public void selfHostedFetchFallsBackToXmlRpcWhenFlagDisabled() {
        when(mMigrationConfig.isEnabled()).thenReturn(false);
        SiteModel site = selfHostedSite();

        mStore.onAction(TaxonomyActionBuilder.newFetchCategoriesAction(site));

        verify(mXmlRpcClient).fetchTerms(site, DEFAULT_TAXONOMY_CATEGORY);
        verifyNoInteractions(mRsApiRestClient);
    }
    // endregion

    // region push
    @Test
    public void selfHostedPushUsesRsApiClientWhenFlagEnabled() {
        when(mMigrationConfig.isEnabled()).thenReturn(true);
        SiteModel site = selfHostedSite();
        TermModel term = sampleCategory(); // remoteTermId > 0 -> update

        mStore.onAction(TaxonomyActionBuilder.newPushTermAction(new RemoteTermPayload(term, site)));

        verify(mRsApiRestClient).updateTerm(site, term);
        verifyNoInteractions(mXmlRpcClient);
    }

    @Test
    public void selfHostedPushFallsBackToXmlRpcWhenFlagDisabled() {
        when(mMigrationConfig.isEnabled()).thenReturn(false);
        SiteModel site = selfHostedSite();
        TermModel term = sampleCategory();

        mStore.onAction(TaxonomyActionBuilder.newPushTermAction(new RemoteTermPayload(term, site)));

        verify(mXmlRpcClient).pushTerm(term, site);
        verifyNoInteractions(mRsApiRestClient);
    }
    // endregion

    // region delete
    @Test
    public void selfHostedDeleteUsesRsApiClientWhenFlagEnabled() {
        when(mMigrationConfig.isEnabled()).thenReturn(true);
        SiteModel site = selfHostedSite();
        TermModel term = sampleCategory();

        mStore.onAction(TaxonomyActionBuilder.newDeleteTermAction(new RemoteTermPayload(term, site)));

        verify(mRsApiRestClient).deleteTerm(site, term);
        verifyNoInteractions(mXmlRpcClient);
    }

    @Test
    public void selfHostedDeleteFallsBackToXmlRpcWhenFlagDisabled() {
        when(mMigrationConfig.isEnabled()).thenReturn(false);
        SiteModel site = selfHostedSite();
        TermModel term = sampleCategory();

        mStore.onAction(TaxonomyActionBuilder.newDeleteTermAction(new RemoteTermPayload(term, site)));

        verify(mXmlRpcClient).deleteTerm(term, site);
        verifyNoInteractions(mRsApiRestClient);
    }
    // endregion

    @Test
    public void wpComSiteIsUnaffectedByFlag() {
        SiteModel site = new SiteModel();
        site.setId(7);
        site.setIsWPCom(true);

        mStore.onAction(TaxonomyActionBuilder.newFetchCategoriesAction(site));

        verify(mRestClient).fetchTerms(site, DEFAULT_TAXONOMY_CATEGORY);
        verifyNoInteractions(mRsApiRestClient);
        verifyNoInteractions(mXmlRpcClient);
    }
}
