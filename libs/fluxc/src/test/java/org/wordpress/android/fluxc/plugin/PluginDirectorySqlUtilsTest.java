package org.wordpress.android.fluxc.plugin;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class PluginDirectorySqlUtilsTest {
    private Random mRandom = new Random(System.currentTimeMillis());

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertPluginDirectoryList() throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        int numberOfDirectories = 10;
        List<PluginDirectoryModel> pluginDirectoryList = new ArrayList<>();
        PluginDirectoryType directoryType = PluginDirectoryType.NEW;
        for (int i = 0; i < numberOfDirectories; i++) {
            PluginDirectoryModel directoryModel = new PluginDirectoryModel();
            directoryModel.setSlug(randomString("slug" + i));
            directoryModel.setDirectoryType(directoryType.toString());
            directoryModel.setPage(1);
            pluginDirectoryList.add(directoryModel);
        }
        PluginSqlUtils.insertPluginDirectoryList(pluginDirectoryList);
        Assert.assertEquals(numberOfDirectories, getPluginDirectoriesForType(directoryType).size());
    }

    @Test
    public void testInsertSinglePluginDirectoryModel() throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        String slug = randomString("slug");
        int page = 5;
        List<PluginDirectoryModel> pluginDirectoryList = new ArrayList<>();
        PluginDirectoryType directoryType = PluginDirectoryType.NEW;
        PluginDirectoryModel directoryModel = new PluginDirectoryModel();
        directoryModel.setSlug(slug);
        directoryModel.setDirectoryType(directoryType.toString());
        directoryModel.setPage(page);
        pluginDirectoryList.add(directoryModel);
        PluginSqlUtils.insertPluginDirectoryList(pluginDirectoryList);

        List<PluginDirectoryModel> directoryList = getPluginDirectoriesForType(directoryType);
        Assert.assertEquals(1, directoryList.size());
        Assert.assertEquals(directoryList.get(0).getPage(), page);
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
            directoryModel.setSlug(randomString("slug" + i));
            directoryModel.setDirectoryType(directoryType.toString());
            int page = mRandom.nextInt(maxPossiblePage);
            directoryModel.setPage(page);
            // Add PluginDirectoryModels one by one
            List<PluginDirectoryModel> pluginDirectoryList = new ArrayList<>();
            pluginDirectoryList.add(directoryModel);
            PluginSqlUtils.insertPluginDirectoryList(pluginDirectoryList);
            // Last requested page is the max value of the `page` column for that directory type
            lastRequestedPage = Math.max(lastRequestedPage, page);
            Assert.assertEquals(lastRequestedPage, PluginSqlUtils.getLastRequestedPageForDirectoryType(directoryType));
        }
    }

    @Test
    public void testGetWPOrgPluginsForDirectory() {
        List<String> slugList = randomSlugList();
        // Insert random 50 wporg plugins
        for (String slug : slugList) {
            WPOrgPluginModel wpOrgPluginModel = new WPOrgPluginModel();
            wpOrgPluginModel.setSlug(slug);
            Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateWPOrgPlugin(wpOrgPluginModel));
        }

        // A Plugin might be in both NEW and POPULAR list, in order to simulate that, we pick high numbers for the
        // plugin list sizes. Since we have 50 items in total, picking 30 and 40 will guarantee some duplicates
        int numberOfNewPlugins = 30;
        int numberOfPopularPlugins = 40;

        // Assert empty state - SQLite's WHERE IN query could crash if the empty list is not handled properly
        Assert.assertEquals(0, PluginSqlUtils.getWPOrgPluginsForDirectory(PluginDirectoryType.NEW).size());
        Assert.assertEquals(0, PluginSqlUtils.getWPOrgPluginsForDirectory(PluginDirectoryType.POPULAR).size());

        // Add plugin directory models for NEW type
        final List<String> slugListForNewPlugins = randomSlugsFromList(slugList, numberOfNewPlugins);
        List<PluginDirectoryModel> directoryListForNewPlugins = new ArrayList<>();
        for (String slug : slugListForNewPlugins) {
            PluginDirectoryModel directoryModel = new PluginDirectoryModel();
            directoryModel.setSlug(slug);
            directoryModel.setDirectoryType(PluginDirectoryType.NEW.toString());
            directoryListForNewPlugins.add(directoryModel);
        }
        PluginSqlUtils.insertPluginDirectoryList(directoryListForNewPlugins);

        // Add plugin directory models for POPULAR type
        final List<String> slugListForPopularPlugins = randomSlugsFromList(slugList, numberOfPopularPlugins);
        List<PluginDirectoryModel> directoryListForPopularPlugins = new ArrayList<>();
        for (String slug : slugListForPopularPlugins) {
            PluginDirectoryModel directoryModel = new PluginDirectoryModel();
            directoryModel.setSlug(slug);
            directoryModel.setDirectoryType(PluginDirectoryType.POPULAR.toString());
            directoryListForPopularPlugins.add(directoryModel);
        }
        PluginSqlUtils.insertPluginDirectoryList(directoryListForPopularPlugins);

        // Assert that getWPOrgPluginsForDirectory return the correct items

        List<WPOrgPluginModel> insertedNewPlugins = PluginSqlUtils.getWPOrgPluginsForDirectory(PluginDirectoryType.NEW);
        Assert.assertEquals(numberOfNewPlugins, insertedNewPlugins.size());
        // The results should be in the order the PluginDirectoryModels were inserted in
        for (int i = 0; i < numberOfNewPlugins; i++) {
            String slug = slugListForNewPlugins.get(i);
            WPOrgPluginModel wpOrgPluginModel = insertedNewPlugins.get(i);
            Assert.assertEquals(wpOrgPluginModel.getSlug(), slug);
        }

        List<WPOrgPluginModel> insertedPopularPlugins =
                PluginSqlUtils.getWPOrgPluginsForDirectory(PluginDirectoryType.POPULAR);
        Assert.assertEquals(numberOfPopularPlugins, insertedPopularPlugins.size());
        // The results should be in the order the PluginDirectoryModels were inserted in
        for (int i = 0; i < numberOfPopularPlugins; i++) {
            String slug = slugListForPopularPlugins.get(i);
            WPOrgPluginModel wpOrgPluginModel = insertedPopularPlugins.get(i);
            Assert.assertEquals(wpOrgPluginModel.getSlug(), slug);
        }
    }

    @SuppressWarnings("unchecked")
    private List<PluginDirectoryModel> getPluginDirectoriesForType(PluginDirectoryType directoryType)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Use reflection to assert PluginSqlUtils.getPluginDirectoriesForType
        Method getPluginDirectoriesForType = PluginSqlUtils.class.getDeclaredMethod("getPluginDirectoriesForType",
                PluginDirectoryType.class);
        getPluginDirectoriesForType.setAccessible(true);
        Object directoryList = getPluginDirectoriesForType.invoke(PluginSqlUtils.class, directoryType);
        Assert.assertTrue(directoryList instanceof List);
        return (List<PluginDirectoryModel>) directoryList;
    }

    private String randomString(String prefix) {
        return prefix + "-" + mRandom.nextInt();
    }

    private List<String> randomSlugList() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            list.add(randomString("slug" + i)); // ensure slugs are different
        }
        return list;
    }

    private List<String> randomSlugsFromList(List<String> slugList, int size) {
        Assert.assertTrue(slugList.size() > size);
        Collections.shuffle(new ArrayList<>(slugList)); // copy the list so it's order is not changed
        return slugList.subList(0, size);
    }
}
