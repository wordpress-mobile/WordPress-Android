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
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class SitePluginSqlUtilsTest {
    private static final int TEST_LOCAL_SITE_ID = 1;
    private static final int SMALL_TEST_POOL = 10;

    private final Random mRandom = new Random(System.currentTimeMillis());

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
        Assert.assertEquals(0, PluginSqlUtils.insertOrUpdateSitePlugin(site, null));
        Assert.assertTrue(PluginSqlUtils.getSitePlugins(site).isEmpty());
    }

    @Test
    public void testInsertSitePlugin() {
        // Create site and plugin
        SiteModel site = getTestSite();
        String slug = randomString("slug");
        SitePluginModel plugin = getTestPluginBySlug(slug);

        // Insert the plugin and assert that it was successful
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin));
        List<SitePluginModel> sitePlugins = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(1, sitePlugins.size());

        // Assert that the inserted plugin is not null and has the correct slug
        SitePluginModel insertedPlugin = sitePlugins.get(0);
        Assert.assertNotNull(insertedPlugin);
        Assert.assertEquals(plugin.getSlug(), insertedPlugin.getSlug());
        Assert.assertEquals(site.getId(), insertedPlugin.getLocalSiteId());
    }

    @Test
    public void testUpdateSitePlugin() {
        SiteModel site = getTestSite();
        String slug = randomString("slug");
        String displayName = randomString("displayName");

        // First install a plugin and retrieve the DB copy
        SitePluginModel plugin = getTestPluginBySlug(slug);
        plugin.setDisplayName(displayName);
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin));
        List<SitePluginModel> sitePlugins = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(1, sitePlugins.size());
        SitePluginModel insertedPlugin = sitePlugins.get(0);
        Assert.assertEquals(insertedPlugin.getDisplayName(), displayName);

        // Then, update the plugin's display name
        String newDisplayName = randomString("newDisplayName");
        insertedPlugin.setDisplayName(newDisplayName);
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, insertedPlugin));

        // Assert that we still have only one plugin in DB and it has the new display name
        List<SitePluginModel> updatedSitePluginList = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(1, updatedSitePluginList.size());
        SitePluginModel updatedPlugin = updatedSitePluginList.get(0);
        Assert.assertEquals(updatedPlugin.getDisplayName(), newDisplayName);

        // Verify that local id of the plugin didn't change
        Assert.assertEquals(insertedPlugin.getId(), updatedPlugin.getId());
    }

    // Inserts 10 plugins with known IDs then retrieves all site plugins and validates slugs
    @Test
    public void testGetSitePlugins() {
        SiteModel site = getTestSite();
        List<String> pluginSlugs = insertBasicTestPlugins(site, SMALL_TEST_POOL);
        List<SitePluginModel> sitePlugins = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(SMALL_TEST_POOL, sitePlugins.size());

        for (int i = 0; i < pluginSlugs.size(); i++) {
            SitePluginModel sitePlugin = sitePlugins.get(i);
            Assert.assertNotNull(sitePlugin);
            Assert.assertEquals(pluginSlugs.get(i), sitePlugin.getSlug());
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
        String newSitePluginSlug = randomString("newPluginSlug");
        SitePluginModel singleSitePlugin = getTestPluginBySlug(newSitePluginSlug);
        newSitePlugins.add(singleSitePlugin);
        PluginSqlUtils.insertOrReplaceSitePlugins(site, newSitePlugins);

        List<SitePluginModel> updatedSitePluginList = PluginSqlUtils.getSitePlugins(site);
        Assert.assertEquals(1, updatedSitePluginList.size());
        SitePluginModel onlyPluginFromUpdatedList = updatedSitePluginList.get(0);
        Assert.assertEquals(onlyPluginFromUpdatedList.getSlug(), newSitePluginSlug);
    }

    @Test
    public void testDeleteSitePlugin() {
        // Create site and plugin
        SiteModel site = getTestSite();
        String slug = randomString("slug");
        SitePluginModel plugin = getTestPluginBySlug(slug);

        // Insert the plugin and verify that site plugin size is 1
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin));
        Assert.assertEquals(1, PluginSqlUtils.getSitePlugins(site).size());

        // Delete the plugin and verify that site plugin list is empty
        Assert.assertEquals(1, PluginSqlUtils.deleteSitePlugin(site, slug));
        Assert.assertTrue(PluginSqlUtils.getSitePlugins(site).isEmpty());
    }

    @Test
    public void testDeleteSitePlugins() {
        // Create site and plugin
        SiteModel site = getTestSite();
        SitePluginModel plugin1 = getTestPluginBySlug(randomString("slug"));
        SitePluginModel plugin2 = getTestPluginBySlug(randomString("slug"));

        // Insert the plugins and verify that site plugin size is 2
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin1));
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin2));
        Assert.assertEquals(2, PluginSqlUtils.getSitePlugins(site).size());

        // Delete the plugins and verify that site plugin list is empty
        Assert.assertEquals(2, PluginSqlUtils.deleteSitePlugins(site));
        Assert.assertTrue(PluginSqlUtils.getSitePlugins(site).isEmpty());
    }

    @Test
    public void testGetSitePluginBySlug() {
        // Create site and 2 plugins
        SiteModel site = getTestSite();
        String pluginSlug1 = randomString("slug1");
        String pluginSlug2 = randomString("slug2");

        SitePluginModel plugin1 = getTestPluginBySlug(pluginSlug1);
        SitePluginModel plugin2 = getTestPluginBySlug(pluginSlug2);

        // Insert the plugins and verify that site plugin size is 2
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin1));
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin2));
        Assert.assertEquals(2, PluginSqlUtils.getSitePlugins(site).size());

        // Assert that getSitePluginBySlug retrieves the correct plugins
        SitePluginModel pluginBySlug1 = PluginSqlUtils.getSitePluginBySlug(site, pluginSlug1);
        Assert.assertNotNull(pluginBySlug1);
        Assert.assertEquals(pluginBySlug1.getSlug(), pluginSlug1);

        SitePluginModel pluginBySlug2 = PluginSqlUtils.getSitePluginBySlug(site, pluginSlug2);
        Assert.assertNotNull(pluginBySlug2);
        Assert.assertEquals(pluginBySlug2.getSlug(), pluginSlug2);
    }

    // Helper methods

    private SitePluginModel getTestPluginBySlug(String slug) {
        SitePluginModel plugin = new SitePluginModel();
        plugin.setLocalSiteId(TEST_LOCAL_SITE_ID);
        plugin.setSlug(slug);
        return plugin;
    }

    private List<String> insertBasicTestPlugins(SiteModel site, int numberOfPlugins) {
        List<String> pluginSlugs = new ArrayList<>();
        for (int i = 0; i < numberOfPlugins; i++) {
            String slug = randomString("slug" + i + "-");
            pluginSlugs.add(slug);
            SitePluginModel plugin = getTestPluginBySlug(slug);
            plugin.setLocalSiteId(site.getId());
            Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin));
        }
        return pluginSlugs;
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
