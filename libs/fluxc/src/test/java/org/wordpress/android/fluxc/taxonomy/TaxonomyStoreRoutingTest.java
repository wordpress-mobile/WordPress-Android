package org.wordpress.android.fluxc.taxonomy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.network.rest.wpapi.taxonomy.TaxonomyRsApiRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.taxonomy.TaxonomyXMLRPCClient;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY;

/**
 * Verifies that {@link TaxonomyStore} routes self-hosted taxonomy operations to the wp-rs REST
 * client whenever the site exposes the REST API (Application Password), and only falls back to the
 * legacy XML-RPC client for self-hosted sites still authenticated with legacy credentials.
 */
@RunWith(RobolectricTestRunner.class)
public class TaxonomyStoreRoutingTest {
    private TaxonomyRestClient mRestClient;
    private TaxonomyXMLRPCClient mXmlRpcClient;
    private TaxonomyRsApiRestClient mRsApiRestClient;

    private TaxonomyStore mStore;

    @Before
    public void setUp() {
        mRestClient = mock(TaxonomyRestClient.class);
        mXmlRpcClient = mock(TaxonomyXMLRPCClient.class);
        mRsApiRestClient = mock(TaxonomyRsApiRestClient.class);
        mStore = new TaxonomyStore(
                new Dispatcher(),
                mRestClient,
                mXmlRpcClient,
                mRsApiRestClient
        );
    }

    /**
     * Self-hosted site authenticated with an application password, so the REST API is available.
     */
    private SiteModel selfHostedSiteWithApplicationPassword() {
        SiteModel site = new SiteModel();
        site.setId(6);
        site.setIsWPCom(false);
        site.setApiRestUsernamePlain("user");
        site.setApiRestPasswordPlain("pass");
        return site;
    }

    /**
     * Self-hosted site on legacy credentials: no application password, so no REST API.
     */
    private SiteModel selfHostedSiteWithoutApplicationPassword() {
        SiteModel site = new SiteModel();
        site.setId(8);
        site.setIsWPCom(false);
        return site;
    }

    private TermModel sampleCategory() {
        return TaxonomyTestUtils.generateSampleCategory();
    }

    // region fetch
    @Test
    public void selfHostedFetchUsesRsApiClientWithApplicationPassword() {
        SiteModel site = selfHostedSiteWithApplicationPassword();

        mStore.onAction(TaxonomyActionBuilder.newFetchCategoriesAction(site));

        verify(mRsApiRestClient).fetchTerms(site, DEFAULT_TAXONOMY_CATEGORY);
        verifyNoInteractions(mXmlRpcClient);
    }

    @Test
    public void selfHostedFetchFallsBackToXmlRpcWithoutApplicationPassword() {
        SiteModel site = selfHostedSiteWithoutApplicationPassword();

        mStore.onAction(TaxonomyActionBuilder.newFetchCategoriesAction(site));

        verify(mXmlRpcClient).fetchTerms(site, DEFAULT_TAXONOMY_CATEGORY);
        verifyNoInteractions(mRsApiRestClient);
    }
    // endregion

    // region push
    @Test
    public void selfHostedPushUsesRsApiClientWithApplicationPassword() {
        SiteModel site = selfHostedSiteWithApplicationPassword();
        TermModel term = sampleCategory(); // remoteTermId > 0 -> update

        mStore.onAction(TaxonomyActionBuilder.newPushTermAction(new RemoteTermPayload(term, site)));

        verify(mRsApiRestClient).updateTerm(site, term);
        verifyNoInteractions(mXmlRpcClient);
    }

    @Test
    public void selfHostedPushFallsBackToXmlRpcWithoutApplicationPassword() {
        SiteModel site = selfHostedSiteWithoutApplicationPassword();
        TermModel term = sampleCategory();

        mStore.onAction(TaxonomyActionBuilder.newPushTermAction(new RemoteTermPayload(term, site)));

        verify(mXmlRpcClient).pushTerm(term, site);
        verifyNoInteractions(mRsApiRestClient);
    }
    // endregion

    // region delete
    @Test
    public void selfHostedDeleteUsesRsApiClientWithApplicationPassword() {
        SiteModel site = selfHostedSiteWithApplicationPassword();
        TermModel term = sampleCategory();

        mStore.onAction(TaxonomyActionBuilder.newDeleteTermAction(new RemoteTermPayload(term, site)));

        verify(mRsApiRestClient).deleteTerm(site, term);
        verifyNoInteractions(mXmlRpcClient);
    }

    @Test
    public void selfHostedDeleteFallsBackToXmlRpcWithoutApplicationPassword() {
        SiteModel site = selfHostedSiteWithoutApplicationPassword();
        TermModel term = sampleCategory();

        mStore.onAction(TaxonomyActionBuilder.newDeleteTermAction(new RemoteTermPayload(term, site)));

        verify(mXmlRpcClient).deleteTerm(term, site);
        verifyNoInteractions(mRsApiRestClient);
    }
    // endregion

    @Test
    public void wpComSiteUsesWpComRestClient() {
        SiteModel site = new SiteModel();
        site.setId(7);
        site.setIsWPCom(true);

        mStore.onAction(TaxonomyActionBuilder.newFetchCategoriesAction(site));

        verify(mRestClient).fetchTerms(site, DEFAULT_TAXONOMY_CATEGORY);
        verifyNoInteractions(mRsApiRestClient);
        verifyNoInteractions(mXmlRpcClient);
    }
}
