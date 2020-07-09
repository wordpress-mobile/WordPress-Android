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
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.VIEW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
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

        // making sure done tasks are retrieved in a correct order
        val completedCustomizeTasks = quickStartStore.getCompletedTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(3, completedCustomizeTasks.size)
        assertEquals(CREATE_SITE, completedCustomizeTasks[0])
        assertEquals(CHOOSE_THEME, completedCustomizeTasks[1])
        assertEquals(VIEW_SITE, completedCustomizeTasks[2])

        val completedGrowTasks = quickStartStore.getCompletedTasksByType(testLocalSiteId, GROW)
        assertEquals(2, completedGrowTasks.size)
        assertEquals(FOLLOW_SITE, completedGrowTasks[0])
        assertEquals(EXPLORE_PLANS, completedGrowTasks[1])

        // making sure undone tasks are retrieved in a correct order
        val uncompletedCustomizeTasks = quickStartStore.getUncompletedTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(4, uncompletedCustomizeTasks.size)
        assertEquals(UPDATE_SITE_TITLE, uncompletedCustomizeTasks[0])
        assertEquals(UPLOAD_SITE_ICON, uncompletedCustomizeTasks[1])
        assertEquals(CUSTOMIZE_SITE, uncompletedCustomizeTasks[2])
        assertEquals(CREATE_NEW_PAGE, uncompletedCustomizeTasks[3])

        val uncompletedGrowTasks = quickStartStore.getUncompletedTasksByType(testLocalSiteId, GROW)
        assertEquals(3, uncompletedGrowTasks.size)
        assertEquals(ENABLE_POST_SHARING, uncompletedGrowTasks[0])
        assertEquals(PUBLISH_POST, uncompletedGrowTasks[1])
        assertEquals(CHECK_STATS, uncompletedGrowTasks[2])
    }

    @Test
    fun orderOfShownTasks() = test {
        // marking tasks as shown in random order
        quickStartStore.setShownTask(testLocalSiteId, UPLOAD_SITE_ICON, true)
        quickStartStore.setShownTask(testLocalSiteId, CHECK_STATS, true)
        quickStartStore.setShownTask(testLocalSiteId, ENABLE_POST_SHARING, true)
        quickStartStore.setShownTask(testLocalSiteId, CREATE_NEW_PAGE, true)
        quickStartStore.setShownTask(testLocalSiteId, PUBLISH_POST, true)

        // making sure shown tasks are retrieved in a correct order
        val shownCustomizeTasks = quickStartStore.getShownTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(2, shownCustomizeTasks.size)
        assertEquals(UPLOAD_SITE_ICON, shownCustomizeTasks[0])
        assertEquals(CREATE_NEW_PAGE, shownCustomizeTasks[1])

        val shownGrowTasks = quickStartStore.getShownTasksByType(testLocalSiteId, GROW)
        assertEquals(3, shownGrowTasks.size)
        assertEquals(ENABLE_POST_SHARING, shownGrowTasks[0])
        assertEquals(PUBLISH_POST, shownGrowTasks[1])
        assertEquals(CHECK_STATS, shownGrowTasks[2])

        // making sure unshown tasks are retrieved in a correct order
        val unshownCustomizeTasks = quickStartStore.getUnshownTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(5, unshownCustomizeTasks.size)
        assertEquals(CREATE_SITE, unshownCustomizeTasks[0])
        assertEquals(UPDATE_SITE_TITLE, unshownCustomizeTasks[1])
        assertEquals(CHOOSE_THEME, unshownCustomizeTasks[2])
        assertEquals(CUSTOMIZE_SITE, unshownCustomizeTasks[3])
        assertEquals(VIEW_SITE, unshownCustomizeTasks[4])

        val unshownGrowTasks = quickStartStore.getUnshownTasksByType(testLocalSiteId, GROW)
        assertEquals(2, unshownGrowTasks.size)
        assertEquals(FOLLOW_SITE, unshownGrowTasks[0])
        assertEquals(EXPLORE_PLANS, unshownGrowTasks[1])
    }
}
