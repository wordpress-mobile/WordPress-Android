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
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.SHARE_SITE
import org.wordpress.android.ui.quickstart.QuickStartTaskState

@RunWith(MockitoJUnitRunner::class)
class QuickStartViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var store: QuickStartStore
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
        assertEquals(QuickStartTask.values().size, mQuickStartDetailStateList?.size)
        assertEquals(0, mQuickStartDetailStateList?.filter { it.isTaskCompleted }?.size)
    }

    @Test
    fun testSetDoneTask() {
        viewModel.start(siteId)

        whenever(store.hasDoneTask(siteId, SHARE_SITE)).thenReturn(true)
        viewModel.completeTask(SHARE_SITE, true)

        assertEquals(1, mQuickStartDetailStateList?.filter { it.task == SHARE_SITE && it.isTaskCompleted }?.size)
    }

    @Test
    fun testSkipAllTasks() {
        viewModel.start(siteId)

        whenever(store.hasDoneTask(eq(siteId), any())).thenReturn(true)

        viewModel.skipAllTasks()

        assertEquals(0, mQuickStartDetailStateList?.filter { !it.isTaskCompleted }?.size)
    }
}
