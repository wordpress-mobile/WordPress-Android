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
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.CHECK_STATS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.FOLLOW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.REVIEW_PAGES
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.VIEW_SITE
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
        quickStartStore.setDoneTask(testLocalSiteId, VIEW_SITE, true)
        quickStartStore.setDoneTask(testLocalSiteId, FOLLOW_SITE, true)
        quickStartStore.setDoneTask(testLocalSiteId, CREATE_SITE, true)

        // making sure done tasks are retrieved in a correct order
        val completedCustomizeTasks = quickStartStore.getCompletedTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(2, completedCustomizeTasks.size)
        assertEquals(CREATE_SITE, completedCustomizeTasks[0])
        assertEquals(VIEW_SITE, completedCustomizeTasks[1])

        val completedGrowTasks = quickStartStore.getCompletedTasksByType(testLocalSiteId, GROW)
        assertEquals(1, completedGrowTasks.size)
        assertEquals(FOLLOW_SITE, completedGrowTasks[0])

        // making sure undone tasks are retrieved in a correct order
        val uncompletedCustomizeTasks = quickStartStore.getUncompletedTasksByType(testLocalSiteId, CUSTOMIZE)
        assertEquals(3, uncompletedCustomizeTasks.size)
        assertEquals(UPDATE_SITE_TITLE, uncompletedCustomizeTasks[0])
        assertEquals(UPLOAD_SITE_ICON, uncompletedCustomizeTasks[1])
        assertEquals(REVIEW_PAGES, uncompletedCustomizeTasks[2])

        val uncompletedGrowTasks = quickStartStore.getUncompletedTasksByType(testLocalSiteId, GROW)
        assertEquals(3, uncompletedGrowTasks.size)
        assertEquals(ENABLE_POST_SHARING, uncompletedGrowTasks[0])
        assertEquals(PUBLISH_POST, uncompletedGrowTasks[1])
        assertEquals(CHECK_STATS, uncompletedGrowTasks[2])
    }
}
