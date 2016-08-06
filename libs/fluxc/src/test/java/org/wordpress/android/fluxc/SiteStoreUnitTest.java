package org.wordpress.android.fluxc;

import android.content.Context;

import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        assertFalse(mSiteStore.hasDotOrgSite());
        assertFalse(mSiteStore.hasJetpackSite());

        assertEquals(1, mSiteStore.getSitesCount());
        assertEquals(1, mSiteStore.getDotComSitesCount());

        // Test counts with one .COM and one self-hosted site
        SiteModel dotOrgSite = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(dotOrgSite);

        assertTrue(mSiteStore.hasSite());
        assertTrue(mSiteStore.hasDotComSite());
        assertTrue(mSiteStore.hasDotOrgSite());
        assertFalse(mSiteStore.hasJetpackSite());

        assertEquals(2, mSiteStore.getSitesCount());
        assertEquals(1, mSiteStore.getDotComSitesCount());
        assertEquals(1, mSiteStore.getDotOrgSitesCount());

        // Test counts with one .COM, one self-hosted and one Jetpack site
        SiteModel jetpackSite = generateJetpackSite();
        SiteSqlUtils.insertOrUpdateSite(jetpackSite);

        assertTrue(mSiteStore.hasSite());
        assertTrue(mSiteStore.hasDotComSite());
        assertTrue(mSiteStore.hasDotOrgSite());
        assertTrue(mSiteStore.hasJetpackSite());

        assertEquals(3, mSiteStore.getSitesCount());
        assertEquals(1, mSiteStore.getDotComSitesCount());
        assertEquals(2, mSiteStore.getDotOrgSitesCount());
        assertEquals(1, mSiteStore.getJetpackSitesCount());
    }

    @Test
    public void testHasSiteWithSiteIdAndXmlRpcUrl() {
        assertFalse(mSiteStore.hasDotOrgSiteWithSiteIdAndXmlRpcUrl(124, ""));

        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(selfHostedSite);

        assertTrue(mSiteStore.hasDotOrgSiteWithSiteIdAndXmlRpcUrl(selfHostedSite.getDotOrgSiteId(),
                selfHostedSite.getXmlRpcUrl()));

        SiteModel jetpackSite = generateJetpackSite();
        SiteSqlUtils.insertOrUpdateSite(jetpackSite);

        assertTrue(mSiteStore.hasDotOrgSiteWithSiteIdAndXmlRpcUrl(jetpackSite.getDotOrgSiteId(),
                jetpackSite.getXmlRpcUrl()));
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
        assertTrue(mSiteStore.hasDotComOrJetpackSiteWithSiteId(jetpackSite.getDotOrgSiteId()));
    }

    @Test
    public void testDotComSiteVisibility() {
        // Should not cause any errors
        mSiteStore.isDotComSiteVisibleByLocalId(45);
        SiteSqlUtils.setSiteVisibility(null, true);

        SiteModel selfHostedNonJPSite = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(selfHostedNonJPSite);

        // Attempt to use with id of self-hosted site
        SiteSqlUtils.setSiteVisibility(selfHostedNonJPSite, false);
        // The self-hosted site should not be affected
        assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.getId()).isVisible());


        SiteModel dotComSite = generateDotComSite();
        SiteSqlUtils.insertOrUpdateSite(dotComSite);

        // Attempt to use with legitimate .com site
        SiteSqlUtils.setSiteVisibility(selfHostedNonJPSite, false);
        assertFalse(mSiteStore.getSiteByLocalId(dotComSite.getId()).isVisible());
        assertFalse(mSiteStore.isDotComSiteVisibleByLocalId(dotComSite.getId()));
    }

    @Test
    public void testSetAllDotComSitesVisibility() {
        SiteModel selfHostedNonJPSite = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(selfHostedNonJPSite);

        // Attempt to use with id of self-hosted site
        for (SiteModel site : mSiteStore.getDotComSites()) {
            SiteSqlUtils.setSiteVisibility(site, false);
        }
        // The self-hosted site should not be affected
        assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.getId()).isVisible());

        SiteModel dotComSite1 = generateDotComSite();
        SiteModel dotComSite2 = generateDotComSite();
        dotComSite2.setId(44);
        dotComSite2.setSiteId(284);

        SiteSqlUtils.insertOrUpdateSite(dotComSite1);
        SiteSqlUtils.insertOrUpdateSite(dotComSite2);

        // Attempt to use with legitimate .com site
        for (SiteModel site : mSiteStore.getDotComSites()) {
            SiteSqlUtils.setSiteVisibility(site, false);
        }
        assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.getId()).isVisible());
        assertFalse(mSiteStore.getSiteByLocalId(dotComSite1.getId()).isVisible());
        assertFalse(mSiteStore.getSiteByLocalId(dotComSite2.getId()).isVisible());
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

    @Test
    public void testGetIdForIdMethods() {
        assertEquals(0, mSiteStore.getLocalIdForRemoteSiteId(555));
        assertEquals(0, mSiteStore.getLocalIdForDotOrgSiteIdAndXmlRpcUrl(2626, ""));
        assertEquals(0, mSiteStore.getSiteIdForLocalId(5577));

        SiteModel dotOrgSite = generateSelfHostedNonJPSite();
        SiteModel dotComSite = generateDotComSite();
        SiteModel jetpackSite = generateJetpackSite();
        SiteSqlUtils.insertOrUpdateSite(dotOrgSite);
        SiteSqlUtils.insertOrUpdateSite(dotComSite);
        SiteSqlUtils.insertOrUpdateSite(jetpackSite);

        assertEquals(dotOrgSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(dotOrgSite.getDotOrgSiteId()));
        assertEquals(dotComSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(dotComSite.getSiteId()));

        // Should be able to look up a Jetpack site by .com and by .org id (assuming it's been set)
        assertEquals(jetpackSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(jetpackSite.getSiteId()));
        assertEquals(jetpackSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(jetpackSite.getDotOrgSiteId()));

        assertEquals(dotOrgSite.getId(), mSiteStore.getLocalIdForDotOrgSiteIdAndXmlRpcUrl(
                dotOrgSite.getDotOrgSiteId(), dotOrgSite.getXmlRpcUrl()));
        assertEquals(jetpackSite.getId(), mSiteStore.getLocalIdForDotOrgSiteIdAndXmlRpcUrl(
                jetpackSite.getDotOrgSiteId(), jetpackSite.getXmlRpcUrl()));

        assertEquals(dotOrgSite.getDotOrgSiteId(), mSiteStore.getSiteIdForLocalId(dotOrgSite.getId()));
        assertEquals(dotComSite.getSiteId(), mSiteStore.getSiteIdForLocalId(dotComSite.getId()));
        assertEquals(jetpackSite.getSiteId(), mSiteStore.getSiteIdForLocalId(jetpackSite.getId()));
    }

    @Test
    public void testGetSiteBySiteId() {
        assertNull(mSiteStore.getSiteBySiteId(555));

        SiteModel dotOrgSite = generateSelfHostedNonJPSite();
        SiteModel dotComSite = generateDotComSite();
        SiteModel jetpackSite = generateJetpackSite();
        SiteSqlUtils.insertOrUpdateSite(dotOrgSite);
        SiteSqlUtils.insertOrUpdateSite(dotComSite);
        SiteSqlUtils.insertOrUpdateSite(jetpackSite);

        assertNotNull(mSiteStore.getSiteBySiteId(dotComSite.getSiteId()));
        assertNotNull(mSiteStore.getSiteBySiteId(jetpackSite.getSiteId()));
        assertNull(mSiteStore.getSiteBySiteId(dotOrgSite.getSiteId()));
    }

    @Test
    public void testDeleteSite() {
        SiteModel dotComSite = generateDotComSite();

        // Should not cause any errors
        SiteSqlUtils.deleteSite(dotComSite);

        SiteSqlUtils.insertOrUpdateSite(dotComSite);
        int affectedRows = SiteSqlUtils.deleteSite(dotComSite);

        assertEquals(1, affectedRows);
        assertEquals(0, mSiteStore.getSitesCount());
    }

    @Test
    public void testGetWPComSites() {
        SiteModel dotComSite = generateDotComSite();
        SiteModel jetpackSiteOverDotOrg = generateJetpackSite();
        SiteModel jetpackSiteOverRestOnly = generateJetpackSiteOverRestOnly();

        SiteSqlUtils.insertOrUpdateSite(dotComSite);
        SiteSqlUtils.insertOrUpdateSite(jetpackSiteOverDotOrg);
        SiteSqlUtils.insertOrUpdateSite(jetpackSiteOverRestOnly);

        List<SiteModel> wpComSites = SiteSqlUtils.getAllWPComSites();

        assertEquals(2, wpComSites.size());
        for (SiteModel site : wpComSites) {
            assertNotEquals(jetpackSiteOverDotOrg.getId(), site.getId());
        }
    }

    @Test
    public void testSearchSitesByNameMatching() {
        SiteModel dotComSite1 = generateDotComSite();
        dotComSite1.setName("Doctor Emmet Brown Homepage");
        SiteModel dotComSite2 = generateDotComSite();
        dotComSite2.setName("Shield Eyes from light");
        SiteModel dotComSite3 = generateDotComSite();
        dotComSite3.setName("I remember when this was all farmland as far as the eye could see");

        SiteSqlUtils.insertOrUpdateSite(dotComSite1);
        SiteSqlUtils.insertOrUpdateSite(dotComSite2);
        SiteSqlUtils.insertOrUpdateSite(dotComSite3);

        List<SiteModel> matchingSites = SiteSqlUtils.getAllSitesMatchingUrlOrName("eye");
        assertEquals(2, matchingSites.size());

        matchingSites = SiteSqlUtils.getAllSitesMatchingUrlOrName("EYE");
        assertEquals(2, matchingSites.size());
    }

    @Test
    public void testSearchSitesByNameOrUrlMatching() {
        SiteModel dotComSite1 = generateDotComSite();
        dotComSite1.setName("Doctor Emmet Brown Homepage");
        SiteModel dotComSite2 = generateDotComSite();
        dotComSite2.setUrl("shieldeyesfromlight.wordpress.com");
        SiteModel dotOrgSite = generateSelfHostedNonJPSite();
        dotOrgSite.setName("I remember when this was all farmland as far as the eye could see.");

        SiteSqlUtils.insertOrUpdateSite(dotComSite1);
        SiteSqlUtils.insertOrUpdateSite(dotComSite2);
        SiteSqlUtils.insertOrUpdateSite(dotOrgSite);

        List<SiteModel> matchingSites = SiteSqlUtils.getAllSitesMatchingUrlOrName("eye");
        assertEquals(2, matchingSites.size());

        matchingSites = SiteSqlUtils.getAllSitesMatchingUrlOrName("EYE");
        assertEquals(2, matchingSites.size());
    }

    @Test
    public void testSearchDotComSitesByNameOrUrlMatching() {
        SiteModel dotComSite1 = generateDotComSite();
        dotComSite1.setName("Doctor Emmet Brown Homepage");
        SiteModel dotComSite2 = generateDotComSite();
        dotComSite2.setUrl("shieldeyesfromlight.wordpress.com");
        SiteModel dotOrgSite = generateSelfHostedNonJPSite();
        dotOrgSite.setName("I remember when this was all farmland as far as the eye could see.");

        SiteSqlUtils.insertOrUpdateSite(dotComSite1);
        SiteSqlUtils.insertOrUpdateSite(dotComSite2);
        SiteSqlUtils.insertOrUpdateSite(dotOrgSite);

        List<SiteModel> matchingSites = SiteSqlUtils.getAllSitesMatchingUrlOrNameWith(
                SiteModelTable.IS_WPCOM, true, "eye");
        assertEquals(1, matchingSites.size());

        matchingSites = SiteSqlUtils.getAllSitesMatchingUrlOrNameWith(
                SiteModelTable.IS_WPCOM, true, "EYE");
        assertEquals(1, matchingSites.size());
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
        example.setDotOrgSiteId(6);
        example.setIsWPCom(false);
        example.setIsJetpack(false);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://some.url/xmlrpc.php");
        return example;
    }

    public SiteModel generateJetpackSite() {
        SiteModel example = new SiteModel();
        example.setId(3);
        example.setSiteId(982);
        example.setDotOrgSiteId(8);
        example.setIsWPCom(false);
        example.setIsJetpack(true);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://jetpack.url/xmlrpc.php");
        return example;
    }

    public SiteModel generateJetpackSiteOverRestOnly() {
        SiteModel example = new SiteModel();
        example.setId(4);
        example.setSiteId(5623);
        example.setIsWPCom(false);
        example.setIsJetpack(true);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://jetpack.url/xmlrpc.php");
        return example;
    }


}
