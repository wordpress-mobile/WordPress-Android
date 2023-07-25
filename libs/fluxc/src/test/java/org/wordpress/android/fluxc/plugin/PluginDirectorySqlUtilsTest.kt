package org.wordpress.android.fluxc.plugin

import com.yarolegovich.wellsql.WellSql
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType.NEW
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType.POPULAR
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel
import org.wordpress.android.fluxc.persistence.PluginSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import java.lang.reflect.InvocationTargetException
import java.util.Random
import kotlin.math.max

@RunWith(RobolectricTestRunner::class)
class PluginDirectorySqlUtilsTest {
    private val random = Random(System.currentTimeMillis())

    @Before fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()
    }

    @Test
    @Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )
    fun testInsertPluginDirectoryList() {
        val numberOfDirectories = 10
        val pluginDirectoryList = arrayListOf<PluginDirectoryModel>()
        val directoryType = NEW
        for (i in 0 until numberOfDirectories) {
            val directoryModel = PluginDirectoryModel()
            directoryModel.slug = randomString("slug$i")
            directoryModel.directoryType = directoryType.toString()
            directoryModel.page = 1
            pluginDirectoryList.add(directoryModel)
        }
        PluginSqlUtils.insertPluginDirectoryList(pluginDirectoryList)
        Assert.assertEquals(numberOfDirectories, getPluginDirectoriesForType(directoryType).size)
    }

    @Test
    @Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )
    fun testInsertSinglePluginDirectoryModel() {
        val slug = randomString("slug")
        val page = 5
        val pluginDirectoryList = arrayListOf<PluginDirectoryModel>()
        val directoryType = NEW
        val directoryModel = PluginDirectoryModel()
        directoryModel.slug = slug
        directoryModel.directoryType = directoryType.toString()
        directoryModel.page = page
        pluginDirectoryList.add(directoryModel)
        PluginSqlUtils.insertPluginDirectoryList(pluginDirectoryList)

        val directoryList = getPluginDirectoriesForType(directoryType)
        Assert.assertEquals(1, directoryList.size)
        Assert.assertEquals(directoryList.first().page, page)
    }

    @Test
    fun testGetLastRequestedPageForDirectoryType() {
        val numberOfTimesToTry = 10
        var lastRequestedPage = 0
        val maxPossiblePage = 100
        val directoryType = NEW
        // We insert a PluginDirectoryModel in each iteration with a random page number and assert
        // that the max value of the page we have set so far is always the last requested page
        for (i in 0 until numberOfTimesToTry) {
            val directoryModel = PluginDirectoryModel()
            directoryModel.slug = randomString("slug$i")
            directoryModel.directoryType = directoryType.toString()
            val page = random.nextInt(maxPossiblePage)
            directoryModel.page = page
            // Add PluginDirectoryModels one by one
            val pluginDirectoryList = arrayListOf<PluginDirectoryModel>()
            pluginDirectoryList.add(directoryModel)
            PluginSqlUtils.insertPluginDirectoryList(pluginDirectoryList)
            // Last requested page is the max value of the `page` column for that directory type
            lastRequestedPage = max(lastRequestedPage, page)
            Assert.assertEquals(
                lastRequestedPage,
                PluginSqlUtils.getLastRequestedPageForDirectoryType(directoryType)
            )
        }
    }

    @Test
    fun testGetWPOrgPluginsForDirectory() {
        val slugList = randomSlugList()
        // Insert random 50 wporg plugins
        slugList.forEach {
            val wpOrgPluginModel = WPOrgPluginModel()
            wpOrgPluginModel.slug = it
            Assert.assertEquals(1, PluginSqlUtils.insertOrUpdateWPOrgPlugin(wpOrgPluginModel))
        }

        // A Plugin might be in both NEW and POPULAR list, in order to simulate that, we pick high
        // numbers for the plugin list sizes. Since we have 50 items in total, picking 30 and 40
        // will guarantee some duplicates
        val numberOfNewPlugins = 30
        val numberOfPopularPlugins = 40

        // Assert empty state - SQLite's WHERE IN query could crash if the empty list is not handled properly
        Assert.assertEquals(0, PluginSqlUtils.getWPOrgPluginsForDirectory(NEW).size)
        Assert.assertEquals(0, PluginSqlUtils.getWPOrgPluginsForDirectory(POPULAR).size)

        // Add plugin directory models for NEW type
        val slugListForNewPlugins = randomSlugsFromList(slugList, numberOfNewPlugins)
        val directoryListForNewPlugins = arrayListOf<PluginDirectoryModel>()
        slugListForNewPlugins.forEach {
            val directoryModel = PluginDirectoryModel()
            directoryModel.slug = it
            directoryModel.directoryType = NEW.toString()
            directoryListForNewPlugins.add(directoryModel)
        }
        PluginSqlUtils.insertPluginDirectoryList(directoryListForNewPlugins)

        // Add plugin directory models for POPULAR type
        val slugListForPopularPlugins = randomSlugsFromList(slugList, numberOfPopularPlugins)
        val directoryListForPopularPlugins: MutableList<PluginDirectoryModel> = ArrayList()
        slugListForPopularPlugins.forEach {
            val directoryModel = PluginDirectoryModel()
            directoryModel.slug = it
            directoryModel.directoryType = POPULAR.toString()
            directoryListForPopularPlugins.add(directoryModel)
        }
        PluginSqlUtils.insertPluginDirectoryList(directoryListForPopularPlugins)

        // Assert that getWPOrgPluginsForDirectory return the correct items

        val insertedNewPlugins = PluginSqlUtils.getWPOrgPluginsForDirectory(NEW)
        Assert.assertEquals(numberOfNewPlugins, insertedNewPlugins.size)
        // The results should be in the order the PluginDirectoryModels were inserted in
        for (i in 0 until numberOfNewPlugins) {
            val slug = slugListForNewPlugins[i]
            val wpOrgPluginModel = insertedNewPlugins[i]
            Assert.assertEquals(wpOrgPluginModel.slug, slug)
        }
        val insertedPopularPlugins = PluginSqlUtils.getWPOrgPluginsForDirectory(POPULAR)
        Assert.assertEquals(numberOfPopularPlugins, insertedPopularPlugins.size)
        // The results should be in the order the PluginDirectoryModels were inserted in
        for (i in 0 until numberOfPopularPlugins) {
            val slug = slugListForPopularPlugins[i]
            val wpOrgPluginModel = insertedPopularPlugins[i]
            Assert.assertEquals(wpOrgPluginModel.slug, slug)
        }
    }

    @Test
    fun testTooManyVariablesForGetWPOrgPluginsForDirectory() {
        val numberOfNewPlugins = 1000

        val slugList: MutableList<String> = ArrayList()
        for (i in 0 until numberOfNewPlugins) {
            slugList.add(randomString("slug$i")) // ensure slugs are different
        }
        // Insert random wporg plugins
        slugList.forEach {
            val wpOrgPluginModel = WPOrgPluginModel()
            wpOrgPluginModel.slug = it
            PluginSqlUtils.insertOrUpdateWPOrgPlugin(wpOrgPluginModel)
        }

        // Add plugin directory models for NEW type
        val directoryListForNewPlugins = arrayListOf<PluginDirectoryModel>()
        slugList.forEach {
            val directoryModel = PluginDirectoryModel()
            directoryModel.slug = it
            directoryModel.directoryType = NEW.toString()
            directoryListForNewPlugins.add(directoryModel)
        }
        PluginSqlUtils.insertPluginDirectoryList(directoryListForNewPlugins)
        val insertedNewPlugins = PluginSqlUtils.getWPOrgPluginsForDirectory(NEW)
        Assert.assertEquals(numberOfNewPlugins, insertedNewPlugins.size)
    }

    @Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )
    private fun getPluginDirectoriesForType(
        directoryType: PluginDirectoryType
    ): List<PluginDirectoryModel> {
        // Use reflection to assert PluginSqlUtils.getPluginDirectoriesForType
        val getPluginDirectoriesForType = PluginSqlUtils::class.java.getDeclaredMethod(
            "getPluginDirectoriesForType",
            PluginDirectoryType::class.java
        )
        getPluginDirectoriesForType.isAccessible = true
        val directoryList = getPluginDirectoriesForType.invoke(
            PluginSqlUtils::class.java,
            directoryType
        )
        Assert.assertTrue(directoryList is List<*>)
        @Suppress("UNCHECKED_CAST")
        return (directoryList as List<PluginDirectoryModel>)
    }

    private fun randomString(prefix: String): String = prefix + "-" + random.nextInt()

    private fun randomSlugList(): List<String?> {
        val list = arrayListOf<String?>()
        for (i in 0..49) {
            list.add(randomString("slug$i")) // ensure slugs are different
        }
        return list
    }

    private fun randomSlugsFromList(slugList: List<String?>, size: Int): List<String?> {
        Assert.assertTrue(slugList.size > size)
        ArrayList(slugList).shuffle() // copy the list so it's order is not changed
        return slugList.subList(0, size)
    }
}
