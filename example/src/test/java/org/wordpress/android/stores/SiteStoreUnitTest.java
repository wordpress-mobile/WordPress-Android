package org.wordpress.android.stores;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.stores.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.stores.persistence.SiteSqlUtils;
import org.wordpress.android.stores.persistence.WellSqlConfig;
import org.wordpress.android.stores.store.SiteStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SiteStoreUnitTest {
    private SiteStore mSiteStore = new SiteStore(new Dispatcher(), Mockito.mock(SiteRestClient.class),
            Mockito.mock(SiteXMLRPCClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, SiteModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testSimpleInsertionAndRetrieval() {
        SiteModel siteModel = new SiteModel();
        siteModel.setSiteId(42);
        WellSql.insert(siteModel).execute();

        assertEquals(1, mSiteStore.getSitesCount());

        assertEquals(42, mSiteStore.getSites().get(0).getSiteId());
    }

    @Test
    public void testInsertOrUpdateSite() {
        SiteModel site = generateDotComSite();
        SiteSqlUtils.insertOrUpdateSite(site);

        assertTrue(mSiteStore.hasSiteWithLocalId(site.getId()));
        assertEquals(site.getSiteId(), mSiteStore.getSiteByLocalId(site.getId()).getSiteId());

    }

    @Test
    public void testHasSiteAndgetCountMethods() {
        assertFalse(mSiteStore.hasSite());
        assertTrue(mSiteStore.getSites().isEmpty());

        // Test counts with .COM site
        SiteModel dotComSite = generateDotComSite();
        SiteSqlUtils.insertOrUpdateSite(dotComSite);

        assertTrue(mSiteStore.hasSite());
        assertTrue(mSiteStore.hasDotComSite());
        assertFalse(mSiteStore.hasSelfHostedSite());
        assertFalse(mSiteStore.hasJetpackSite());

        assertEquals(1, mSiteStore.getSitesCount());
        assertEquals(1, mSiteStore.getDotComSitesCount());

        // Test counts with one .COM and one self-hosted site
        SiteModel dotOrgSite = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(dotOrgSite);

        assertTrue(mSiteStore.hasSite());
        assertTrue(mSiteStore.hasDotComSite());
        assertTrue(mSiteStore.hasSelfHostedSite());
        assertFalse(mSiteStore.hasJetpackSite());

        assertEquals(2, mSiteStore.getSitesCount());
        assertEquals(1, mSiteStore.getDotComSitesCount());
        assertEquals(1, mSiteStore.getSelfHostedSitesCount());

        // Test counts with one .COM, one self-hosted and one Jetpack site
        SiteModel jetpackSite = generateJetpackSite();
        SiteSqlUtils.insertOrUpdateSite(jetpackSite);

        assertTrue(mSiteStore.hasSite());
        assertTrue(mSiteStore.hasDotComSite());
        assertTrue(mSiteStore.hasSelfHostedSite());
        assertTrue(mSiteStore.hasJetpackSite());

        assertEquals(3, mSiteStore.getSitesCount());
        assertEquals(1, mSiteStore.getDotComSitesCount());
        assertEquals(2, mSiteStore.getSelfHostedSitesCount());
        assertEquals(1, mSiteStore.getJetpackSitesCount());
    }

    @Test
    public void testHasSiteWithSiteIdAndXmlRpcUrl() {
        assertFalse(mSiteStore.hasSiteWithSiteIdAndXmlRpcUrl(124, ""));

        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(selfHostedSite);

        assertTrue(mSiteStore.hasSiteWithSiteIdAndXmlRpcUrl(selfHostedSite.getSiteId(), selfHostedSite.getXMLRpcUrl()));

        SiteModel jetpackSite = generateJetpackSite();
        SiteSqlUtils.insertOrUpdateSite(jetpackSite);

        assertTrue(mSiteStore.hasSiteWithSiteIdAndXmlRpcUrl(jetpackSite.getSiteId(), jetpackSite.getXMLRpcUrl()));
    }

    @Test
    public void testHasDotComOrJetpackSiteWithSiteId() {
        assertFalse(mSiteStore.hasDotComOrJetpackSiteWithSiteId(673));

        SiteModel dotComSite = generateDotComSite();
        SiteSqlUtils.insertOrUpdateSite(dotComSite);

        assertTrue(mSiteStore.hasDotComOrJetpackSiteWithSiteId(dotComSite.getSiteId()));

        SiteModel jetpackSite = generateJetpackSite();
        SiteSqlUtils.insertOrUpdateSite(jetpackSite);

        // hasDotComOrJetpackSiteWithSiteId() should be able to locate a Jetpack site with either the site id or the
        // .COM site id
        assertTrue(mSiteStore.hasDotComOrJetpackSiteWithSiteId(jetpackSite.getSiteId()));
        assertTrue(mSiteStore.hasDotComOrJetpackSiteWithSiteId(jetpackSite.getDotComIdForJetpack()));
    }

    @Test
    public void testIsCurrentUserAdminOfSiteId() {
        assertFalse(mSiteStore.isCurrentUserAdminOfSiteId(0));

        SiteModel site = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(site);

        assertFalse(mSiteStore.isCurrentUserAdminOfSiteId(site.getSiteId()));

        site.setIsAdmin(true);
        assertTrue(site.isAdmin());
        SiteSqlUtils.insertOrUpdateSite(site);

        assertTrue(mSiteStore.getSiteByLocalId(site.getId()).isAdmin());
        assertTrue(mSiteStore.isCurrentUserAdminOfSiteId(site.getSiteId()));
    }

    public SiteModel generateDotComSite() {
        SiteModel example = new SiteModel();
        example.setId(1);
        example.setSiteId(556);
        example.setIsWPCom(true);
        example.setIsVisible(true);
        return example;
    }

    public SiteModel generateSelfHostedNonJPSite() {
        SiteModel example = new SiteModel();
        example.setId(2);
        example.setSiteId(6);
        example.setIsWPCom(false);
        example.setIsJetpack(false);
        example.setXMLRpcUrl("http://some.url/xmlrpc.php");
        return example;
    }

    public SiteModel generateJetpackSite() {
        SiteModel example = new SiteModel();
        example.setId(3);
        example.setSiteId(8);
        example.setDotComIdForJetpack(982);
        example.setIsWPCom(false);
        example.setIsJetpack(true);
        example.setXMLRpcUrl("http://jetpack.url/xmlrpc.php");
        return example;
    }
}
