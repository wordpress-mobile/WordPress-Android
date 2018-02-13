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
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class PluginDirectorySqlUtilsTest {
    private Random mRandom = new Random(System.currentTimeMillis());

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, PluginDirectoryModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void insertPluginDirectoryList() throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        int numberOfDirectories = 10;
        List<PluginDirectoryModel> pluginDirectoryList = new ArrayList<>();
        PluginDirectoryType directoryType = PluginDirectoryType.NEW;
        for (int i = 0; i < numberOfDirectories; i++) {
            PluginDirectoryModel directoryModel = new PluginDirectoryModel();
            directoryModel.setSlug(randomSlug());
            directoryModel.setDirectoryType(directoryType.toString());
            directoryModel.setPage(1);
            pluginDirectoryList.add(directoryModel);
        }
        Assert.assertEquals(numberOfDirectories, PluginSqlUtils.insertOrUpdatePluginDirectoryList(pluginDirectoryList));

        // Use reflection to assert PluginSqlUtils.getPluginDirectoriesForType
        Method getPluginDirectoriesForType = PluginSqlUtils.class.getDeclaredMethod("getPluginDirectoriesForType",
                PluginDirectoryType.class);
        getPluginDirectoriesForType.setAccessible(true);
        Object directoryList = getPluginDirectoriesForType.invoke(PluginSqlUtils.class, directoryType);
        Assert.assertTrue(directoryList instanceof List);
        Assert.assertEquals(numberOfDirectories, ((List) directoryList).size());
    }

    @Test
    public void testInsertSinglePluginDirectoryModel() throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        String slug = randomSlug();
        int page = 5;
        List<PluginDirectoryModel> pluginDirectoryList = new ArrayList<>();
        String directoryType = PluginDirectoryType.NEW.toString();
        PluginDirectoryModel directoryModel = new PluginDirectoryModel();
        directoryModel.setSlug(slug);
        directoryModel.setDirectoryType(directoryType);
        directoryModel.setPage(page);
        pluginDirectoryList.add(directoryModel);
        Assert.assertEquals(1, PluginSqlUtils.insertOrUpdatePluginDirectoryList(pluginDirectoryList));

        // Use reflection to assert PluginSqlUtils.getPluginDirectoriesForType
        Method getPluginDirectoryModel = PluginSqlUtils.class.getDeclaredMethod("getPluginDirectoryModel",
                String.class, String.class);
        getPluginDirectoryModel.setAccessible(true);
        Object object = getPluginDirectoryModel.invoke(PluginSqlUtils.class, directoryType, slug);
        Assert.assertNotNull(object);
        Assert.assertTrue(object instanceof PluginDirectoryModel);
        PluginDirectoryModel insertedDirectoryModel = (PluginDirectoryModel) object;
        Assert.assertEquals(insertedDirectoryModel.getPage(), page);
    }

    @Test
    public void testGetLastRequestedPageForDirectoryType() {
        int numberOfTimesToTry = 10;
        int lastRequestedPage = 0;
        int maxPossiblePage = 100;
        PluginDirectoryType directoryType = PluginDirectoryType.NEW;
        // We insert a PluginDirectoryModel in each iteration with a random page number and assert that the max
        // value of the page we have set so far is always the last requested page
        for (int i = 0; i < numberOfTimesToTry; i++) {
            PluginDirectoryModel directoryModel = new PluginDirectoryModel();
            directoryModel.setSlug(randomSlug());
            directoryModel.setDirectoryType(directoryType.toString());
            int page = mRandom.nextInt(maxPossiblePage);
            directoryModel.setPage(page);
            // Add PluginDirectoryModels one by one
            List<PluginDirectoryModel> pluginDirectoryList = new ArrayList<>();
            pluginDirectoryList.add(directoryModel);
            Assert.assertEquals(1, PluginSqlUtils.insertOrUpdatePluginDirectoryList(pluginDirectoryList));
            // Last requested page is the max value of the `page` column for that directory type
            lastRequestedPage = Math.max(lastRequestedPage, page);
            Assert.assertEquals(lastRequestedPage, PluginSqlUtils.getLastRequestedPageForDirectoryType(directoryType));
        }
    }

    private String randomSlug() {
        return "slug-" + mRandom.nextInt();
    }
}
