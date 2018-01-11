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
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class PluginSqlUtilsTest {
    private static final int TEST_LOCAL_SITE_ID = 1;
    private static final int SMALL_TEST_POOL = 10;

    private Random mRandom = new Random(System.currentTimeMillis());

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, SitePluginModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertNullSitePlugin() {
        SiteModel site = getTestSite();
        Assert.assertEquals(0, PluginSqlUtils.insertOrUpdateSitePlugin(null));
        Assert.assertTrue(PluginSqlUtils.getSitePlugins(site).isEmpty());
    }

    @Test
    public void testInsertSitePlugin() {
        // Create site and plugin
        SiteModel site = getTestSite();
        String name = randomString("name");
        SitePluginModel plugin = getTestPlugin(name);

        // Insert the plugin and assert that it was successful
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(plugin));
        List<SitePluginModel> sitePlugins = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(1, sitePlugins.size());

        // Assert that the inserted plugin is not null and has the correct name
        SitePluginModel insertedPlugin = sitePlugins.get(0);
        Assert.assertNotNull(insertedPlugin);
        Assert.assertEquals(plugin.getName(), insertedPlugin.getName());
        Assert.assertEquals(site.getId(), insertedPlugin.getLocalSiteId());
    }

    @Test
    public void testUpdateSitePlugin() {
        SiteModel site = getTestSite();
        String name = randomString("name");
        String displayName = randomString("displayName");

        // First install a plugin and retrieve the DB copy
        SitePluginModel plugin = getTestPlugin(name);
        plugin.setDisplayName(displayName);
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(plugin));
        List<SitePluginModel> sitePlugins = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(1, sitePlugins.size());
        SitePluginModel insertedPlugin = sitePlugins.get(0);
        Assert.assertEquals(insertedPlugin.getDisplayName(), displayName);

        // Then, update the plugin's display name
        String newDisplayName = randomString("newDisplayName");
        insertedPlugin.setDisplayName(newDisplayName);
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(insertedPlugin));

        // Assert that we still have only one plugin in DB and it has the new display name
        List<SitePluginModel> updatedSitePluginList = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(1, updatedSitePluginList.size());
        SitePluginModel updatedPlugin = updatedSitePluginList.get(0);
        Assert.assertEquals(updatedPlugin.getDisplayName(), newDisplayName);

        // Verify that local id of the plugin didn't change
        Assert.assertEquals(insertedPlugin.getId(), updatedPlugin.getId());
    }

    // Inserts 10 plugins with known IDs then retrieves all site plugins and validates names
    @Test
    public void testGetSitePlugins() {
        SiteModel site = getTestSite();
        List<String> pluginNames = insertBasicTestPlugins(site, SMALL_TEST_POOL);
        List<SitePluginModel> sitePlugins = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(SMALL_TEST_POOL, sitePlugins.size());

        for (int i = 0; i < pluginNames.size(); i++) {
            SitePluginModel sitePlugin = sitePlugins.get(i);
            Assert.assertNotNull(sitePlugin);
            Assert.assertEquals(pluginNames.get(i), sitePlugin.getName());
        }
    }

    @Test
    public void testReplaceSitePlugins() {
        // First insert small set of basic plugins and assert that
        SiteModel site = getTestSite();
        insertBasicTestPlugins(site, SMALL_TEST_POOL);
        List<SitePluginModel> sitePlugins = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(sitePlugins.size(), SMALL_TEST_POOL);

        // Create a single plugin and update the site plugin list and assert that now we have a single plugin
        List<SitePluginModel> newSitePlugins = new ArrayList<>();
        String newSitePluginName = randomString("newPluginName");
        SitePluginModel singleSitePlugin = getTestPlugin(newSitePluginName);
        newSitePlugins.add(singleSitePlugin);
        PluginSqlUtils.insertOrReplaceSitePlugins(site, newSitePlugins);

        List<SitePluginModel> updatedSitePluginList = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(1, updatedSitePluginList.size());
        SitePluginModel onlyPluginFromUpdatedList = updatedSitePluginList.get(0);
        Assert.assertEquals(onlyPluginFromUpdatedList.getName(), newSitePluginName);
    }

    @Test
    public void testDeleteSitePlugin() {
        // Create site and plugin
        SiteModel site = getTestSite();
        String name = randomString("name");
        SitePluginModel plugin = getTestPlugin(name);

        // Insert the plugin and verify that site plugin size is 1
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(plugin));
        Assert.assertEquals(1, PluginSqlUtils.getSitePlugins(site).size());

        // Delete the plugin and verify that site plugin list is empty
        Assert.assertEquals(1, PluginSqlUtils.deleteSitePlugin(site, plugin));
        Assert.assertTrue(PluginSqlUtils.getSitePlugins(site).isEmpty());
    }

    @Test
    public void testGetSitePluginByName() {
        // Create site and 2 plugins
        SiteModel site = getTestSite();
        String pluginName1 = randomString("name1");
        String pluginName2 = randomString("name2");
        String displayName1 = randomString("displayName1");
        String displayName2 = randomString("displayName2");

        SitePluginModel plugin1 = getTestPlugin(pluginName1);
        plugin1.setDisplayName(displayName1);
        SitePluginModel plugin2 = getTestPlugin(pluginName2);
        plugin2.setDisplayName(displayName2);

        // Insert the plugins and verify that site plugin size is 2
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(plugin1));
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(plugin2));
        Assert.assertEquals(2, PluginSqlUtils.getSitePlugins(site).size());

        // Assert that getSitePluginByName retrieves the correct plugins
        SitePluginModel pluginByName1 = PluginSqlUtils.getSitePluginByName(site, pluginName1);
        Assert.assertNotNull(pluginByName1);
        Assert.assertEquals(pluginByName1.getName(), pluginName1);
        Assert.assertEquals(pluginByName1.getDisplayName(), displayName1);

        SitePluginModel pluginByName2 = PluginSqlUtils.getSitePluginByName(site, pluginName2);
        Assert.assertNotNull(pluginByName2);
        Assert.assertEquals(pluginByName2.getName(), pluginName2);
        Assert.assertEquals(pluginByName2.getDisplayName(), displayName2);
    }

    // Helper methods

    private SitePluginModel getTestPlugin(String name) {
        SitePluginModel plugin = new SitePluginModel();
        plugin.setLocalSiteId(TEST_LOCAL_SITE_ID);
        plugin.setName(name);
        return plugin;
    }

    private List<String> insertBasicTestPlugins(SiteModel site, int numberOfPlugins) {
        List<String> pluginNames = new ArrayList<>();
        for (int i = 0; i < numberOfPlugins; i++) {
            String name = randomString("name");
            pluginNames.add(name);
            SitePluginModel plugin = getTestPlugin(name);
            plugin.setLocalSiteId(site.getId());
            Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(plugin));
        }
        return pluginNames;
    }

    private String randomString(String prefix) {
        return prefix + "-" + mRandom.nextInt();
    }

    private SiteModel getTestSite() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(TEST_LOCAL_SITE_ID);
        return siteModel;
    }
}
