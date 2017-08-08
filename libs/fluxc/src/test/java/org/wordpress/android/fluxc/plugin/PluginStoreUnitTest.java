package org.wordpress.android.fluxc.plugin;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginInfoClient;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.site.SiteUtils;
import org.wordpress.android.fluxc.store.PluginStore;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.wordpress.android.fluxc.plugin.PluginUtils.generatePlugins;

@RunWith(RobolectricTestRunner.class)
public class PluginStoreUnitTest {
    private PluginStore mPluginStore = new PluginStore(new Dispatcher(),
            Mockito.mock(PluginRestClient.class), Mockito.mock(PluginInfoClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testGetPlugins() throws DuplicateSiteException {
        SiteModel site = SiteUtils.generateJetpackSiteOverRestOnly();
        SiteSqlUtils.insertOrUpdateSite(site);

        // Set 3 plugins
        PluginSqlUtils.insertOrReplacePlugins(site, generatePlugins("akismet", "hello", "jetpack"));
        List<PluginModel> plugins = mPluginStore.getPlugins(site);
        assertEquals(3, plugins.size());

        // Set 1 plugin
        PluginSqlUtils.insertOrReplacePlugins(site, generatePlugins("jetpack"));
        plugins = mPluginStore.getPlugins(site);
        assertEquals("jetpack", plugins.get(0).getName());
    }
}
