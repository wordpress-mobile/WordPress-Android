package org.wordpress.android.util

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.widgets.WPSnackbarWrapper

private const val TEST_MESSAGE_TEMPLATE = "This is test message number "
private const val SNACKBAR_DURATION_MS = 500L

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SnackbarSequencerConcurrentTest : BaseUnitTest() {
    @Mock
    lateinit var wpSnackbarWrapper: WPSnackbarWrapper

    @Mock
    lateinit var wpSnackbar: Snackbar

    @Mock
    lateinit var view: View

    @Mock
    lateinit var activity: Activity

    private val uiHelper: UiHelpers = UiHelpers()
    private lateinit var sequencer: SnackbarSequencer

    @Before
    fun setUp() {
        whenever(activity.isFinishing).thenReturn(false)
        whenever(view.context).thenReturn(activity)
        whenever(wpSnackbarWrapper.make(any(), any(), any())).thenReturn(wpSnackbar)

        sequencer = SnackbarSequencer(
            uiHelper,
            wpSnackbarWrapper,
            testDispatcher()
        )
    }

    @Test
    fun `snackbars are shown in sequence with the correct duration`() = test {
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
    fun `snackbars are not shown until previous duration elapsed`() = test {
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
    fun `snackbars beyond capacity are not shown`() = test {
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
