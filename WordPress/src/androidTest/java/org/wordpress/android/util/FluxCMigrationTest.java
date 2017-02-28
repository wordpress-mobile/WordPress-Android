package org.wordpress.android.util;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.TestUtils;
import org.wordpress.android.TestWellSqlConfig;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.List;

public class FluxCMigrationTest extends InstrumentationTestCase {
    private Context mTestContext;
    private Context mRenamingTargetAppContext;

    @Override
    protected void setUp() throws Exception {
        mRenamingTargetAppContext = new RenamingDelegatingContext(
                getInstrumentation().getTargetContext().getApplicationContext(), "test_");
        mTestContext = getInstrumentation().getContext();

        WellSqlConfig config = new TestWellSqlConfig(mRenamingTargetAppContext);
        WellSql.init(config);
        config.reset();

        super.setUp();
    }

    public void testSelfHostedSiteMigration() throws Exception {
        TestUtils.loadDBFromDump(mRenamingTargetAppContext, mTestContext, "FluxC-migration.sql");

        List<SiteModel> sites = WPLegacyMigrationUtils.getSelfHostedSitesFromDeprecatedDB(mRenamingTargetAppContext);
        AppLog.d(AppLog.T.DB, "Found " + sites.size() + " site to migrate");

        // Expect two self-hosted sites (one normal, one blank except for username, password, and XML-RPC URL)
        assertEquals(2, sites.size());
    }
}
