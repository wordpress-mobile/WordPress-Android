package org.wordpress.android.fluxc.quickstart

import com.yarolegovich.wellsql.WellSql
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import org.wordpress.android.fluxc.persistence.QuickStartSqlUtils
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHECK_STATS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHOOSE_THEME
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_NEW_PAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CUSTOMIZE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EXPLORE_PLANS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.FOLLOW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.VIEW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROWTH
import org.wordpress.android.fluxc.test

@RunWith(RobolectricTestRunner::class)
class QuickStartStoreTest {
    private val testLocalSiteId: Long = 72
    private lateinit var quickStartStore: QuickStartStore

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(QuickStartTaskModel::class.java), ""
        )
        WellSql.init(config)
        config.reset()

        quickStartStore = QuickStartStore(QuickStartSqlUtils(), Dispatcher())
    }

    @Test
    fun orderOfDoneTasks() = test {
        // marking tasks as done in random order
        quickStartStore.setDoneTask(testLocalSiteId, CHOOSE_THEME, true)
        quickStartStore.setDoneTask(testLocalSiteId, EXPLORE_PLANS, true)
        quickStartStore.setDoneTask(testLocalSiteId, VIEW_SITE, true)
        quickStartStore.setDoneTask(testLocalSiteId, FOLLOW_SITE, true)
        quickStartStore.setDoneTask(testLocalSiteId, CREATE_SITE, true)

        // making sure tasks are retrieved in a correct order
        val completedCustomizeTasks = quickStartStore.getCompletedTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(3, completedCustomizeTasks.size)
        assertEquals(CREATE_SITE, completedCustomizeTasks[0])
        assertEquals(CHOOSE_THEME, completedCustomizeTasks[1])
        assertEquals(VIEW_SITE, completedCustomizeTasks[2])

        val completedGrowthTasks = quickStartStore.getCompletedTasksByType(testLocalSiteId, GROWTH)
        assertEquals(2, completedGrowthTasks.size)
        assertEquals(FOLLOW_SITE, completedGrowthTasks[0])
        assertEquals(EXPLORE_PLANS, completedGrowthTasks[1])

        val uncompletedCustomizeTasks = quickStartStore.getUncompletedTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(3, uncompletedCustomizeTasks.size)
        assertEquals(UPLOAD_SITE_ICON, uncompletedCustomizeTasks[0])
        assertEquals(CUSTOMIZE_SITE, uncompletedCustomizeTasks[1])
        assertEquals(CREATE_NEW_PAGE, uncompletedCustomizeTasks[2])

        val uncompletedGrowthTasks = quickStartStore.getUncompletedTasksByType(testLocalSiteId, GROWTH)
        assertEquals(3, uncompletedGrowthTasks.size)
        assertEquals(ENABLE_POST_SHARING, uncompletedGrowthTasks[0])
        assertEquals(PUBLISH_POST, uncompletedGrowthTasks[1])
        assertEquals(CHECK_STATS, uncompletedGrowthTasks[2])
    }

    @Test
    fun orderOfShownTasks() = test {
        // marking tasks as done in random order
        quickStartStore.setShownTask(testLocalSiteId, UPLOAD_SITE_ICON, true)
        quickStartStore.setShownTask(testLocalSiteId, CHECK_STATS, true)
        quickStartStore.setShownTask(testLocalSiteId, ENABLE_POST_SHARING, true)
        quickStartStore.setShownTask(testLocalSiteId, CREATE_NEW_PAGE, true)
        quickStartStore.setShownTask(testLocalSiteId, PUBLISH_POST, true)

        // making sure tasks are retrieved in a correct order
        val shownCustomizeTasks = quickStartStore.getShownTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(2, shownCustomizeTasks.size)
        assertEquals(UPLOAD_SITE_ICON, shownCustomizeTasks[0])
        assertEquals(CREATE_NEW_PAGE, shownCustomizeTasks[1])

        val shownGrowthTasks = quickStartStore.getShownTasksByType(testLocalSiteId, GROWTH)
        assertEquals(3, shownGrowthTasks.size)
        assertEquals(ENABLE_POST_SHARING, shownGrowthTasks[0])
        assertEquals(PUBLISH_POST, shownGrowthTasks[1])
        assertEquals(CHECK_STATS, shownGrowthTasks[2])

        val unshownCustomizeTasks = quickStartStore.getUnshownTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(4, unshownCustomizeTasks.size)
        assertEquals(CREATE_SITE, unshownCustomizeTasks[0])
        assertEquals(CHOOSE_THEME, unshownCustomizeTasks[1])
        assertEquals(CUSTOMIZE_SITE, unshownCustomizeTasks[2])
        assertEquals(VIEW_SITE, unshownCustomizeTasks[3])

        val unshownGrowthTasks = quickStartStore.getUnshownTasksByType(testLocalSiteId, GROWTH)
        assertEquals(2, unshownGrowthTasks.size)
        assertEquals(FOLLOW_SITE, unshownGrowthTasks[0])
        assertEquals(EXPLORE_PLANS, unshownGrowthTasks[1])
    }
}
