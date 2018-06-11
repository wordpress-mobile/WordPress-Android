package org.wordpress.android.fluxc.store

import com.yarolegovich.wellsql.WellSql
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.QuickStartModel
import org.wordpress.android.fluxc.persistence.QuickStartSqlUtils
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class QuickStartStoreTest {
    private val TEST_LOCAL_SITE_ID: Long = 72
    private lateinit var quickStartStore: QuickStartStore

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(appContext, QuickStartModel::class.java)
        WellSql.init(config)
        config.reset()

        quickStartStore = QuickStartStore(QuickStartSqlUtils(), Dispatcher())
    }

    @Test
    fun testDoneCount() {
        assertEquals(0, quickStartStore.getDoneCount(TEST_LOCAL_SITE_ID))

        quickStartStore.setDoneTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.FOLLOW_SITE, true)
        assertEquals(1, quickStartStore.getDoneCount(TEST_LOCAL_SITE_ID))

        quickStartStore.setDoneTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.FOLLOW_SITE, false)
        assertEquals(0, quickStartStore.getDoneCount(TEST_LOCAL_SITE_ID))
    }

    @Test
    fun testShownCount() {
        assertEquals(0, quickStartStore.getShownCount(TEST_LOCAL_SITE_ID))

        quickStartStore.setShownTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CHOOSE_THEME, true)
        assertEquals(1, quickStartStore.getShownCount(TEST_LOCAL_SITE_ID))

        quickStartStore.setShownTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CHOOSE_THEME, false)
        assertEquals(0, quickStartStore.getShownCount(TEST_LOCAL_SITE_ID))
    }

    @Test
    fun testTaskDoneStatus() {
        assertFalse(quickStartStore.hasDoneTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CUSTOMIZE_SITE))

        quickStartStore.setDoneTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CUSTOMIZE_SITE, true)
        assertTrue(quickStartStore.hasDoneTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CUSTOMIZE_SITE))

        quickStartStore.setDoneTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CUSTOMIZE_SITE, false)
        assertFalse(quickStartStore.hasDoneTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CUSTOMIZE_SITE))
    }

    @Test
    fun testTaskShownStatus() {
        assertFalse(quickStartStore.hasShownTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CREATE_SITE))

        quickStartStore.setShownTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CREATE_SITE, true)
        assertTrue(quickStartStore.hasShownTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CREATE_SITE))

        quickStartStore.setShownTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CREATE_SITE, false)
        assertFalse(quickStartStore.hasShownTask(TEST_LOCAL_SITE_ID, QuickStartStore.QuickStartTask.CREATE_SITE))
    }
}
