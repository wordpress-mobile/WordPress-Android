package org.wordpress.android.viewmodel.quickstart

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.quickstart.QuickStartTaskState

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class QuickStartViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var store: QuickStartStore
    private val siteId = 1L
    private lateinit var viewModel: QuickStartViewModel
    private var mQuickStartDetailStateList: List<QuickStartTaskState>? = null

    @Before
    fun setUp() {
        viewModel = QuickStartViewModel(store)
        viewModel.quickStartTaskStateStates.observeForever { mQuickStartDetailStateList = it }
    }

    @After
    fun tearDown() {
        mQuickStartDetailStateList = null
    }

    @Test
    fun testStartingViewModel() {
        viewModel.start(siteId)

        Assert.assertNotNull(mQuickStartDetailStateList)
        assertEquals(QuickStartTask.getAllTasks().size, mQuickStartDetailStateList?.size)
        assertEquals(0, mQuickStartDetailStateList?.filter { it.isTaskCompleted }?.size)
    }

    @Test
    fun testSetDoneTask() {
        viewModel.start(siteId)

        whenever(store.hasDoneTask(siteId, ENABLE_POST_SHARING)).thenReturn(true)
        viewModel.completeTask(ENABLE_POST_SHARING, true)

        assertEquals(1, mQuickStartDetailStateList?.filter {
            it.task == ENABLE_POST_SHARING &&
                    it.isTaskCompleted
        }?.size)
    }

    @Test
    fun testSkipAllTasks() {
        viewModel.start(siteId)

        whenever(store.hasDoneTask(eq(siteId), any())).thenReturn(true)

        viewModel.skipAllTasks()

        assertEquals(0, mQuickStartDetailStateList?.filter { !it.isTaskCompleted }?.size)
    }
}
