package org.wordpress.android.util

import android.app.Activity
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.material.snackbar.Snackbar
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.widgets.WPSnackbarWrapper

private const val TEST_MESSAGE_TEMPLATE = "This is test message number "
private const val SNACKBAR_DURATION_MS = 500L

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SnackbarSequencerConcurrentTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    @Mock lateinit var wpSnackbarWrapper: WPSnackbarWrapper
    @Mock lateinit var wpSnackbar: Snackbar
    @Mock lateinit var view: View
    @Mock lateinit var activity: Activity

    private val uiHelper: UiHelpers = UiHelpers()
    private lateinit var sequencer: SnackbarSequencer

    private val job = Job()
    private val dispatcher = TestCoroutineDispatcher()
    private val coroutineContext = job + dispatcher

    @Before
    fun setUp() {
        whenever(activity.isFinishing).thenReturn(false)
        whenever(view.context).thenReturn(activity)
        whenever(wpSnackbarWrapper.make(any(), any(), any())).thenReturn(wpSnackbar)

        sequencer = SnackbarSequencer(uiHelper, wpSnackbarWrapper, dispatcher)
    }

    @Test
    fun `snackbars are shown in sequence with the correct duration`() = runBlockingTest(coroutineContext) {
        // Given
        val items = getItems(2)

        // When
        for (item in items) {
            sequencer.enqueue(item)
        }

        // Then
        verify(wpSnackbar, times(1)).show()
        // Probably not strictly necessary but we add a +1 to the duration just to be explicit that
        // we want to sample after the snackbar duration.
        advanceTimeBy(SNACKBAR_DURATION_MS + 1)
        verify(wpSnackbar, times(2)).show()
        verifyNoMoreInteractions(wpSnackbar)
    }

    @Test
    fun `snackbars are not shown until previous duration elapsed`() = runBlockingTest(coroutineContext) {
        // Given
        val items = getItems(2)

        // When
        for (item in items) {
            sequencer.enqueue(item)
        }

        // Then
        verify(wpSnackbar, times(1)).show()
        // We offset the duration with -1 to be explicit that
        // we want to sample before the snackbar duration.
        // Note also that the advanceTimeBy function adds on top of the previous time so
        // adding +2 brings us just after the first duration
        advanceTimeBy(SNACKBAR_DURATION_MS - 1)
        verify(wpSnackbar, times(1)).show()
        advanceTimeBy(2)
        verify(wpSnackbar, times(2)).show()
        verifyNoMoreInteractions(wpSnackbar)
    }

    @Test
    fun `snackbars beyond capacity are not shown`() = runBlockingTest(coroutineContext) {
        // Given
        val items = getItems(10)

        // When
        for (item in items) {
            sequencer.enqueue(item)
        }

        // Then
        advanceTimeBy(SNACKBAR_DURATION_MS * items.size)
        verify(wpSnackbar, times(QUEUE_SIZE_LIMIT + 1)).show()
        verifyNoMoreInteractions(wpSnackbar)
    }

    private fun getItems(numItems: Int): List<SnackbarItem> {
        return List(numItems) { index ->
            SnackbarItem(
                    Info(
                        view = view,
                        textRes = UiStringText(TEST_MESSAGE_TEMPLATE + index),
                        duration = SNACKBAR_DURATION_MS.toInt()
                    )
            )
        }
    }
}
