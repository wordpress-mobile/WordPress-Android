package org.wordpress.android.fluxc.utils

import com.yarolegovich.wellsql.WellSql
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.QuickStartStatusModel
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import org.wordpress.android.fluxc.persistence.QuickStartSqlUtils
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class QuickStartSqlUtilsTest {
    private val testLocalSiteId: Long = 72
    private lateinit var quickStartSqlUtils: QuickStartSqlUtils

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(QuickStartTaskModel::class.java, QuickStartStatusModel::class.java), ""
        )
        WellSql.init(config)
        config.reset()

        quickStartSqlUtils = QuickStartSqlUtils()
    }

    @Test
    fun testDoneCount() {
        assertEquals(0, quickStartSqlUtils.getDoneCount(testLocalSiteId))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.FOLLOW_SITE, true)
        assertEquals(1, quickStartSqlUtils.getDoneCount(testLocalSiteId))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.FOLLOW_SITE, false)
        assertEquals(0, quickStartSqlUtils.getDoneCount(testLocalSiteId))
    }

    @Test
    fun testDoneCountByType() {
        assertEquals(0, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, CUSTOMIZE))
        assertEquals(0, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, GROW))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.CREATE_SITE, true)
        assertEquals(1, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, CUSTOMIZE))
        assertEquals(0, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, GROW))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.CREATE_SITE, false)
        assertEquals(0, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, CUSTOMIZE))
        assertEquals(0, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, GROW))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.PUBLISH_POST, true)
        assertEquals(1, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, GROW))
        assertEquals(0, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, CUSTOMIZE))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.PUBLISH_POST, false)
        assertEquals(0, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, GROW))
        assertEquals(0, quickStartSqlUtils.getDoneCountByType(testLocalSiteId, CUSTOMIZE))
    }

    @Test
    fun testShownCount() {
        assertEquals(0, quickStartSqlUtils.getShownCount(testLocalSiteId))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.UNKNOWN, true)
        assertEquals(1, quickStartSqlUtils.getShownCount(testLocalSiteId))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.UNKNOWN, false)
        assertEquals(0, quickStartSqlUtils.getShownCount(testLocalSiteId))
    }

    @Test
    fun testShownCountByType() {
        assertEquals(0, quickStartSqlUtils.getShownCountByType(testLocalSiteId, CUSTOMIZE))
        assertEquals(0, quickStartSqlUtils.getShownCountByType(testLocalSiteId, GROW))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.UPLOAD_SITE_ICON, true)
        assertEquals(1, quickStartSqlUtils.getShownCountByType(testLocalSiteId, CUSTOMIZE))
        assertEquals(0, quickStartSqlUtils.getShownCountByType(testLocalSiteId, GROW))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.UPLOAD_SITE_ICON, false)
        assertEquals(0, quickStartSqlUtils.getShownCountByType(testLocalSiteId, CUSTOMIZE))
        assertEquals(0, quickStartSqlUtils.getShownCountByType(testLocalSiteId, GROW))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CHECK_STATS, true)
        assertEquals(1, quickStartSqlUtils.getShownCountByType(testLocalSiteId, GROW))
        assertEquals(0, quickStartSqlUtils.getShownCountByType(testLocalSiteId, CUSTOMIZE))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CHECK_STATS, false)
        assertEquals(0, quickStartSqlUtils.getShownCountByType(testLocalSiteId, GROW))
        assertEquals(0, quickStartSqlUtils.getShownCountByType(testLocalSiteId, CUSTOMIZE))
    }

    @Test
    fun testTaskDoneStatus() {
        assertFalse(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.UNKNOWN))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.UNKNOWN, true)
        assertTrue(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.UNKNOWN))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.UNKNOWN, false)
        assertFalse(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.UNKNOWN))
    }

    @Test
    fun testTaskShownStatus() {
        assertFalse(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE, true)
        assertTrue(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE, false)
        assertFalse(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))
    }

    @Test
    fun testTaskMultipleStatuses() {
        assertFalse(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))
        assertFalse(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.CREATE_SITE))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE, true)
        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.CREATE_SITE, true)
        assertTrue(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))
        assertTrue(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.CREATE_SITE))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE, false)
        assertFalse(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))
        assertTrue(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.CREATE_SITE))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.CREATE_SITE, false)
        assertFalse(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))
        assertFalse(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.CREATE_SITE))
    }

    @Test
    fun testQuickStartCompletedStatus() {
        assertFalse(quickStartSqlUtils.getQuickStartCompleted(testLocalSiteId))

        quickStartSqlUtils.setQuickStartCompleted(testLocalSiteId, true)
        assertTrue(quickStartSqlUtils.getQuickStartCompleted(testLocalSiteId))

        quickStartSqlUtils.setQuickStartCompleted(testLocalSiteId, false)
        assertFalse(quickStartSqlUtils.getQuickStartCompleted(testLocalSiteId))
    }

    @Test
    fun testQuickStartNotificationReceivedStatus() {
        assertFalse(quickStartSqlUtils.getQuickStartNotificationReceived(testLocalSiteId))

        quickStartSqlUtils.setQuickStartNotificationReceived(testLocalSiteId, true)
        assertTrue(quickStartSqlUtils.getQuickStartNotificationReceived(testLocalSiteId))

        quickStartSqlUtils.setQuickStartNotificationReceived(testLocalSiteId, false)
        assertFalse(quickStartSqlUtils.getQuickStartNotificationReceived(testLocalSiteId))
    }
}
