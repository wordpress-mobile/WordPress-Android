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
        String name = randomString("name");

        // Assert no plugin exist with the slug
        Assert.assertNull(PluginSqlUtils.getWPOrgPluginBySlug(slug));

        // Create wporg plugin
        WPOrgPluginModel plugin = new WPOrgPluginModel();
        plugin.setSlug(slug);
        plugin.setName(name);

        // Insert the plugin and assert that it was successful
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateWPOrgPlugin(plugin));
        WPOrgPluginModel insertedPlugin = PluginSqlUtils.getWPOrgPluginBySlug(slug);
        Assert.assertNotNull(insertedPlugin);
        Assert.assertEquals(insertedPlugin.getName(), name);
    }

    private String randomString(String prefix) {
        return prefix + "-" + mRandom.nextInt();
    }
}
