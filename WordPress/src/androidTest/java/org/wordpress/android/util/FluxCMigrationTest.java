package org.wordpress.android.util;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import com.yarolegovich.wellsql.WellSql;

import org.mockito.Mockito;
import org.wordpress.android.TestUtils;
import org.wordpress.android.TestWellSqlConfig;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.SiteStore;

import java.util.List;

public class FluxCMigrationTest extends InstrumentationTestCase {
    private Context mTestContext;
    private Context mRenamingTargetAppContext;

    private SiteStore mSiteStore;

    @Override
    protected void setUp() throws Exception {
        // Needed for Mockito
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        mRenamingTargetAppContext = new RenamingDelegatingContext(
                getInstrumentation().getTargetContext().getApplicationContext(), "test_");
        mTestContext = getInstrumentation().getContext();

        mSiteStore = new SiteStore(new Dispatcher(), Mockito.mock(SiteRestClient.class),
                Mockito.mock(SiteXMLRPCClient.class));

        WellSqlConfig config = new TestWellSqlConfig(mRenamingTargetAppContext);
        WellSql.init(config);
        config.reset();

        super.setUp();
    }

    public void testSelfHostedSiteMigration() {
        TestUtils.loadDBFromDump(mRenamingTargetAppContext, mTestContext, "FluxC-migration.sql");

        List<SiteModel> sites = WPLegacyMigrationUtils.getSelfHostedSitesFromDeprecatedDB(mRenamingTargetAppContext);
        AppLog.d(AppLog.T.DB, "Found " + sites.size() + " site to migrate");

        // Expect two self-hosted sites (one normal, one blank except for username, password, and XML-RPC URL)
        assertEquals(2, sites.size());
    }
}
