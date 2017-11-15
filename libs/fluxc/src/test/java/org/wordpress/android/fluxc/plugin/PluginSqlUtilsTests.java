package org.wordpress.android.fluxc.plugin;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class PluginSqlUtilsTests {
    private static final int TEST_LOCAL_SITE_ID = 1;

    private Random mRandom = new Random(System.currentTimeMillis());

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, PluginModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertNullSitePlugin() {
        SiteModel site = getTestSiteWithLocalId(TEST_LOCAL_SITE_ID);
        Assert.assertEquals(0, PluginSqlUtils.insertOrUpdateSitePlugin(site, null));
        Assert.assertTrue(PluginSqlUtils.getSitePlugins(site).isEmpty());
    }

    @Test
    public void testInsertSitePlugin() {
        SiteModel site = getTestSiteWithLocalId(TEST_LOCAL_SITE_ID);
        String name = randomString("name");
        String slug = randomString("slug");

        PluginModel plugin = getTestPlugin(name, slug);
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin));
        List<PluginModel> sitePlugins = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(1, sitePlugins.size());
        PluginModel insertedPlugin = sitePlugins.get(0);
        Assert.assertNotNull(insertedPlugin);
        Assert.assertEquals(plugin.getName(), insertedPlugin.getName());
        Assert.assertEquals(plugin.getSlug(), insertedPlugin.getSlug());
    }

    private PluginModel getTestPlugin(String name, String slug) {
        PluginModel plugin = new PluginModel();
        plugin.setLocalSiteId(TEST_LOCAL_SITE_ID);
        plugin.setName(name);
        plugin.setSlug(slug);
        return plugin;
    }

    private String randomString(String prefix) {
        return prefix + "-" + mRandom.nextInt();
    }

    private SiteModel getTestSiteWithLocalId(int localSiteId) {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(localSiteId);
        return siteModel;
    }
}
