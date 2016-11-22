package org.wordpress.android.fluxc.taxonomy;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.taxonomy.TaxonomyXMLRPCClient;
import org.wordpress.android.fluxc.persistence.TaxonomySqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.TaxonomyStore;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class TaxonomyStoreUnitTest {
    private TaxonomyStore mTaxonomyStore = new TaxonomyStore(new Dispatcher(), Mockito.mock(TaxonomyRestClient.class),
            Mockito.mock(TaxonomyXMLRPCClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, TermModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertNullTerm() {
        assertEquals(0, TaxonomySqlUtils.insertOrUpdateTerm(null));

        assertEquals(0, TaxonomyTestUtils.getTermsCount());
    }

    @Test
    public void testSimpleInsertionAndRetrieval() {
        TermModel termModel = new TermModel();
        termModel.setRemoteTermId(42);
        TaxonomySqlUtils.insertOrUpdateTerm(termModel);

        assertEquals(1, TaxonomyTestUtils.getTermsCount());
        assertEquals(termModel, TaxonomyTestUtils.getTerms().get(0));
    }

    @Test
    public void testCategoryInsertionAndRetrieval() {
        SiteModel site = new SiteModel();
        site.setId(6);

        TermModel category = TaxonomyTestUtils.generateSampleCategory();
        TaxonomySqlUtils.insertOrUpdateTerm(category);

        assertEquals(1, TaxonomyTestUtils.getTermsCount());
        assertEquals(category, TaxonomyTestUtils.getTerms().get(0));

        assertEquals(1, TaxonomySqlUtils.getTermsForSite(site, TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY).size());
        assertEquals(1, mTaxonomyStore.getCategoriesForSite(site).size());

        assertEquals(0, TaxonomySqlUtils.getTermsForSite(site, TaxonomyStore.DEFAULT_TAXONOMY_TAG).size());
        assertEquals(0, mTaxonomyStore.getTagsForSite(site).size());

        assertEquals(0, TaxonomySqlUtils.getTermsForSite(site, "author").size());
        assertEquals(0, mTaxonomyStore.getTermsForSite(site, "author").size());
    }

    @Test
    public void testTagInsertionAndRetrieval() {
        SiteModel site = new SiteModel();
        site.setId(6);

        TermModel tag = TaxonomyTestUtils.generateSampleTag();
        TaxonomySqlUtils.insertOrUpdateTerm(tag);

        assertEquals(1, TaxonomyTestUtils.getTermsCount());
        assertEquals(tag, TaxonomyTestUtils.getTerms().get(0));

        assertEquals(1, TaxonomySqlUtils.getTermsForSite(site, TaxonomyStore.DEFAULT_TAXONOMY_TAG).size());
        assertEquals(1, mTaxonomyStore.getTagsForSite(site).size());

        assertEquals(0, TaxonomySqlUtils.getTermsForSite(site, TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY).size());
        assertEquals(0, mTaxonomyStore.getCategoriesForSite(site).size());

        assertEquals(0, TaxonomySqlUtils.getTermsForSite(site, "author").size());
        assertEquals(0, mTaxonomyStore.getTermsForSite(site, "author").size());
    }

    @Test
    public void testCustomTaxonomyTermInsertionAndRetrieval() {
        SiteModel site = new SiteModel();
        site.setId(6);

        TermModel author = TaxonomyTestUtils.generateSampleAuthor();
        TaxonomySqlUtils.insertOrUpdateTerm(author);

        assertEquals(1, TaxonomyTestUtils.getTermsCount());
        assertEquals(author, TaxonomyTestUtils.getTerms().get(0));

        assertEquals(1, TaxonomySqlUtils.getTermsForSite(site, "author").size());
        assertEquals(1, mTaxonomyStore.getTermsForSite(site, "author").size());

        assertEquals(0, TaxonomySqlUtils.getTermsForSite(site, TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY).size());
        assertEquals(0, mTaxonomyStore.getCategoriesForSite(site).size());

        assertEquals(0, TaxonomySqlUtils.getTermsForSite(site, TaxonomyStore.DEFAULT_TAXONOMY_TAG).size());
        assertEquals(0, mTaxonomyStore.getTagsForSite(site).size());
    }

    @Test
    public void testGetTermByRemoteId() {
        SiteModel site = new SiteModel();
        site.setId(6);

        TermModel category = TaxonomyTestUtils.generateSampleCategory();
        TaxonomySqlUtils.insertOrUpdateTerm(category);

        assertEquals(category, mTaxonomyStore.getCategoryByRemoteId(site, category.getRemoteTermId()));
    }
}
