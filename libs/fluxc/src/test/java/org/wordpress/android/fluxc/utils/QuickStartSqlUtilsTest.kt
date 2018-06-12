package org.wordpress.android.fluxc.utils

import com.yarolegovich.wellsql.WellSql
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.QuickStartModel
import org.wordpress.android.fluxc.persistence.QuickStartSqlUtils
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class QuickStartSqlUtilsTest {
    private val testLocalSiteId: Long = 72
    private lateinit var quickStartSqlUtils: QuickStartSqlUtils

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(appContext, QuickStartModel::class.java)
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
    fun testShownCount() {
        assertEquals(0, quickStartSqlUtils.getShownCount(testLocalSiteId))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CHOOSE_THEME, true)
        assertEquals(1, quickStartSqlUtils.getShownCount(testLocalSiteId))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CHOOSE_THEME, false)
        assertEquals(0, quickStartSqlUtils.getShownCount(testLocalSiteId))
    }

    @Test
    fun testTaskDoneStatus() {
        assertFalse(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.CUSTOMIZE_SITE))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.CUSTOMIZE_SITE, true)
        assertTrue(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.CUSTOMIZE_SITE))

        quickStartSqlUtils.setDoneTask(testLocalSiteId, QuickStartTask.CUSTOMIZE_SITE, false)
        assertFalse(quickStartSqlUtils.hasDoneTask(testLocalSiteId, QuickStartTask.CUSTOMIZE_SITE))
    }

    @Test
    fun testTaskShownStatus() {
        assertFalse(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE, true)
        assertTrue(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))

        quickStartSqlUtils.setShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE, false)
        assertFalse(quickStartSqlUtils.hasShownTask(testLocalSiteId, QuickStartTask.CREATE_SITE))
    }
}
