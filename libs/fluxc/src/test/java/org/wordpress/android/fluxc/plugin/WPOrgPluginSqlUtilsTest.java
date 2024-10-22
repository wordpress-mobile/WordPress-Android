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
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class WPOrgPluginSqlUtilsTest {
    private Random mRandom = new Random(System.currentTimeMillis());

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, WPOrgPluginModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertNullWPOrgPlugin() {
        Assert.assertEquals(0, PluginSqlUtils.insertOrUpdateWPOrgPlugin(null));
    }

    @Test
    public void testInsertWPOrgPlugin() {
        String slug = randomString("slug");
        String displayName = randomString("displayName");

        // Assert no plugin exist with the slug
        Assert.assertNull(PluginSqlUtils.getWPOrgPluginBySlug(slug));

        // Create wporg plugin
        WPOrgPluginModel plugin = new WPOrgPluginModel();
        plugin.setSlug(slug);
        plugin.setDisplayName(displayName);

        // Insert the plugin and assert that it was successful
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateWPOrgPlugin(plugin));
        WPOrgPluginModel insertedPlugin = PluginSqlUtils.getWPOrgPluginBySlug(slug);
        Assert.assertNotNull(insertedPlugin);
        Assert.assertEquals(insertedPlugin.getDisplayName(), displayName);
    }

    @Test
    public void testUpdateWPOrgPlugin() {
        String slug = randomString("slug");
        String displayName = randomString("displayName");

        WPOrgPluginModel plugin = new WPOrgPluginModel();
        plugin.setSlug(slug);
        plugin.setDisplayName(displayName);

        // Insert a wporg plugin and assert the state
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateWPOrgPlugin(plugin));
        WPOrgPluginModel insertedPlugin = PluginSqlUtils.getWPOrgPluginBySlug(slug);
        Assert.assertNotNull(insertedPlugin);
        Assert.assertEquals(insertedPlugin.getDisplayName(), displayName);

        // Update the name of the plugin and try insertOrUpdate and make sure the plugin is updated
        String displayName2 = randomString("displayName2-");
        Assert.assertTrue(!displayName.equals(displayName2));
        insertedPlugin.setDisplayName(displayName2);
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateWPOrgPlugin(insertedPlugin));
        WPOrgPluginModel updatedPlugin = PluginSqlUtils.getWPOrgPluginBySlug(slug);
        Assert.assertNotNull(updatedPlugin);
        Assert.assertEquals(insertedPlugin.getSlug(), updatedPlugin.getSlug());
        Assert.assertEquals(updatedPlugin.getDisplayName(), displayName2);
    }

    @Test
    public void testInsertWPOrgPluginList() {
        int numberOfPlugins = 10;
        List<WPOrgPluginModel> plugins = new ArrayList<>();
        List<String> slugList = new ArrayList<>();
        for (int i = 0; i < numberOfPlugins; i++) {
            String slug = randomString("slug") + i;
            slugList.add(slug);
            WPOrgPluginModel wpOrgPluginModel = new WPOrgPluginModel();
            wpOrgPluginModel.setSlug(slug);
            plugins.add(wpOrgPluginModel);
        }
        Assert.assertEquals(numberOfPlugins, PluginSqlUtils.insertOrUpdateWPOrgPluginList(plugins));

        for (String slug : slugList) {
            WPOrgPluginModel wpOrgPluginModel = PluginSqlUtils.getWPOrgPluginBySlug(slug);
            Assert.assertNotNull(wpOrgPluginModel);
        }
    }

    @Test
    public void testUpdateWPOrgPluginList() {
        int numberOfPlugins = 2;
        List<WPOrgPluginModel> plugins = new ArrayList<>();
        List<String> slugList = new ArrayList<>();
        List<String> displayNameList = new ArrayList<>();
        for (int i = 0; i < numberOfPlugins; i++) {
            String slug = randomString("slug") + i;
            String displayName = randomString("name") + i;
            slugList.add(slug);
            displayNameList.add(displayName);
            WPOrgPluginModel wpOrgPluginModel = new WPOrgPluginModel();
            wpOrgPluginModel.setSlug(slug);
            wpOrgPluginModel.setDisplayName(displayName);
            plugins.add(wpOrgPluginModel);
        }
        // Insert plugins
        Assert.assertEquals(numberOfPlugins, PluginSqlUtils.insertOrUpdateWPOrgPluginList(plugins));

        List<String> updatedNameList = new ArrayList<>();
        List<WPOrgPluginModel> updatedPlugins = new ArrayList<>();
        for (int i = 0; i < slugList.size(); i++) {
            String slug = slugList.get(i);
            String newDisplayName = randomString("newDisplayName" + i + "-");
            updatedNameList.add(newDisplayName);
            WPOrgPluginModel wpOrgPluginModel = PluginSqlUtils.getWPOrgPluginBySlug(slug);
            Assert.assertNotNull(wpOrgPluginModel);
            // Update plugin name
            wpOrgPluginModel.setDisplayName(newDisplayName);
            updatedPlugins.add(wpOrgPluginModel);
        }
        // Update plugins
        Assert.assertEquals(numberOfPlugins, PluginSqlUtils.insertOrUpdateWPOrgPluginList(updatedPlugins));

        // Assert the plugins are updated
        for (int i = 0; i < numberOfPlugins; i++) {
            String slug = slugList.get(i);
            String previousName = displayNameList.get(i);
            String expectedName = updatedNameList.get(i);
            WPOrgPluginModel wpOrgPluginModel = PluginSqlUtils.getWPOrgPluginBySlug(slug);
            Assert.assertNotNull(wpOrgPluginModel);
            Assert.assertFalse(StringUtils.equals(wpOrgPluginModel.getDisplayName(), previousName));
            Assert.assertTrue(StringUtils.equals(wpOrgPluginModel.getDisplayName(), expectedName));
        }
    }

    private String randomString(String prefix) {
        return prefix + "-" + mRandom.nextInt();
    }
}
