package org.wordpress.android.viewmodel.quickstart

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.FOLLOW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.SHARE_SITE
import org.wordpress.android.ui.quickstart.QuickStartDetailModel

@RunWith(MockitoJUnitRunner::class)
class QuickStartViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var store: QuickStartStore
    private val siteId = 1L
    private lateinit var viewModel: QuickStartViewModel
    private var quickStartDetailModelList: List<QuickStartDetailModel>? = null

    @Before
    fun setUp() {
        viewModel = QuickStartViewModel(store)
        viewModel.quickStartTasks.observeForever { quickStartDetailModelList = it }
    }

    @After
    fun tearDown() {
        quickStartDetailModelList = null
    }

    @Test
    fun testStart() {
        whenever(store.hasDoneTask(siteId, FOLLOW_SITE)).thenReturn(true)
        viewModel.start(siteId)

        Assert.assertNotNull(quickStartDetailModelList)
        assertEquals(1, quickStartDetailModelList?.filter { it.task == FOLLOW_SITE && it.isTaskCompleted }?.size)
    }


    @Test
    fun testSetDoneTask() {
        viewModel.start(siteId)

        Assert.assertNotNull(quickStartDetailModelList)
        assertEquals(0, quickStartDetailModelList?.filter { it.task != CREATE_SITE && it.isTaskCompleted }?.size)

        whenever(store.hasDoneTask(siteId, SHARE_SITE)).thenReturn(true)

        viewModel.setDoneTask(SHARE_SITE,true)

        assertEquals(1, quickStartDetailModelList?.filter { it.task == SHARE_SITE && it.isTaskCompleted }?.size)
    }

    @Test
    fun testSkipAllTasks() {
        viewModel.start(siteId)

        Assert.assertNotNull(quickStartDetailModelList)
        assertEquals(0, quickStartDetailModelList?.filter { it.task != CREATE_SITE && it.isTaskCompleted }?.size)

        whenever(store.hasDoneTask(eq(siteId), any())).thenReturn(true)

        viewModel.skipAllTasks()

        assertEquals(1, quickStartDetailModelList?.filter { it.task == SHARE_SITE && it.isTaskCompleted }?.size)
    }

    @Test
    fun testCreateSiteTaskDoneByDefault() {
        viewModel.start(siteId)

        Assert.assertNotNull(quickStartDetailModelList)
        assertEquals(0, quickStartDetailModelList?.filter { it.task != CREATE_SITE && it.isTaskCompleted }?.size)
        assertEquals(1, quickStartDetailModelList?.filter { it.task == CREATE_SITE && it.isTaskCompleted }?.size)
    }
}
