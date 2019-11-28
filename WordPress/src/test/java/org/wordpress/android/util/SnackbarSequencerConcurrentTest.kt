package org.wordpress.android.util

import android.app.Activity
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import org.wordpress.android.widgets.WPSnackbar
import org.wordpress.android.widgets.WPSnackbarWrapper
import java.util.Collections.max

private val TEST_MESSAGES = listOf(
    "This is test message 1",
    "This is test message 2",
    "This is test message 3"
)

private val TEST_DURATIONS = listOf(
    5000,
    10000,
    15000
)

const val SNACKBAR_DURATION_MARGIN = 100L

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SnackbarSequencerConcurrentTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    @Mock lateinit var wpSnackbarWrapper: WPSnackbarWrapper
    @Mock lateinit var wpSnackbar: WPSnackbar
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
        val items = getItems()
        val checkPoints = getCheckPoints(items)

        // When
        for (item in items) {
            sequencer.enqueue(item)
        }

        // Then
        verify(wpSnackbar, times(1)).show()
        advanceTimeBy(checkPoints[0])
        verify(wpSnackbar, times(2)).show()
        advanceTimeBy(checkPoints[1])
        verify(wpSnackbar, times(3)).show()
        advanceTimeBy(checkPoints[2])

        // Nothing else shown
        verify(wpSnackbar, times(3)).show()
        verifyNoMoreInteractions(wpSnackbar)
    }

    @Test
    fun `snackbars are not shown until previous duration elapsed`() = runBlockingTest(coroutineContext) {
        // Given
        val items = getItems()
        val checkPoints = getCheckPoints(items)

        // When
        for (item in items) {
            sequencer.enqueue(item)
        }

        // Then
        verify(wpSnackbar, times(1)).show()
        advanceTimeBy(checkPoints[0] - 2 * SNACKBAR_DURATION_MARGIN)
        verify(wpSnackbar, times(1)).show()
        advanceTimeBy(2 * SNACKBAR_DURATION_MARGIN)
        verify(wpSnackbar, times(2)).show()
        advanceTimeBy(checkPoints[1])
        verify(wpSnackbar, times(3)).show()
        advanceTimeBy(checkPoints[2])

        // Nothing else shown
        verify(wpSnackbar, times(3)).show()
        verifyNoMoreInteractions(wpSnackbar)
    }

    @Test
    fun `snackbars beyond capacity are not shown`() = runBlockingTest(coroutineContext) {
        // Given
        val items = mutableListOf<SnackbarItem>()

        items.addAll(getItems())
        items.addAll(getItems())
        items.addAll(getItems())

        // When
        for (item in items) {
            sequencer.enqueue(item)
        }

        // Then
        advanceTimeBy((max(TEST_DURATIONS) * items.size).toLong())
        verify(wpSnackbar, times(QUEUE_SIZE_LIMIT + 1)).show()
        verifyNoMoreInteractions(wpSnackbar)
    }

    private fun getCheckPoints(items: List<SnackbarItem>): List<Long> {
        return listOf(
            SNACKBAR_DURATION_MARGIN + items[0].getSnackbarDurationMs(),
            items[1].getSnackbarDurationMs(),
            items[2].getSnackbarDurationMs()
        )
    }

    private fun getItems(): List<SnackbarItem> {
        return listOf(
            SnackbarItem(
                    Info(
                        view = view,
                        textRes = UiStringText(TEST_MESSAGES[0]),
                        duration = TEST_DURATIONS[0]
                    )
            ),
            SnackbarItem(
                    Info(
                        view = view,
                        textRes = UiStringText(TEST_MESSAGES[1]),
                        duration = TEST_DURATIONS[1]
                    )
            ),
            SnackbarItem(
                    Info(
                        view = view,
                        textRes = UiStringText(TEST_MESSAGES[2]),
                        duration = TEST_DURATIONS[2]
                    )
            )
        )
    }
}
