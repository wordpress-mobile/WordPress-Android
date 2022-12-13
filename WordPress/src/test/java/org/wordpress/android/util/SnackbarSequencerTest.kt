package org.wordpress.android.util

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.widgets.WPSnackbarWrapper
import java.lang.ref.WeakReference

private const val TEST_MESSAGE = "This is a test message"

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SnackbarSequencerTest : BaseUnitTest() {
    @Mock lateinit var wpSnackbarWrapper: WPSnackbarWrapper
    @Mock lateinit var wpSnackbar: Snackbar
    @Mock lateinit var view: View
    @Mock lateinit var activity: Activity

    private val uiHelper: UiHelpers = UiHelpers()
    private lateinit var sequencer: SnackbarSequencer

    private lateinit var item: SnackbarItem

    @Before
    fun setUp() {
        whenever(activity.isFinishing).thenReturn(false)
        whenever(view.context).thenReturn(activity)
        whenever(wpSnackbarWrapper.make(any(), any(), any())).thenReturn(wpSnackbar)

        sequencer = SnackbarSequencer(uiHelper, wpSnackbarWrapper, TEST_DISPATCHER)

        item = SnackbarItem(
                Info(
                        view = view,
                        textRes = UiStringText(TEST_MESSAGE),
                        duration = Snackbar.LENGTH_LONG
                )
        )
    }

    @Test
    fun `snackbar is shown when activity is alive`() {
        // Given
        whenever(activity.isFinishing).thenReturn(false)

        // When
        sequencer.enqueue(item)

        // Then
        val messageCaptor = argumentCaptor<CharSequence>()
        verify(wpSnackbarWrapper).make(any(), messageCaptor.capture(), any())
        val capturedMessage = messageCaptor.firstValue

        assertThat(capturedMessage).isEqualTo(TEST_MESSAGE)
        verify(wpSnackbar, times(1)).show()
    }

    @Test
    fun `snackbar is not shown when activity is not alive`() {
        // Given
        whenever(activity.isFinishing).thenReturn(true)

        // When
        sequencer.enqueue(item)

        // Then
        verify(wpSnackbar, never()).show()
    }

    @Test
    fun `snackbar is not shown when view is null`() {
        // Given
        val spiedItem = spy(item)
        val aliveSnackbarInfo = mock<Info>()
        val deadSnackbarInfo = mock<Info>()

        whenever(aliveSnackbarInfo.view).thenReturn(WeakReference(view))
        whenever(deadSnackbarInfo.view).thenReturn(WeakReference(null))
        whenever(spiedItem.info).thenReturn(aliveSnackbarInfo, deadSnackbarInfo)

        // When
        sequencer.enqueue(spiedItem)

        // Then
        verify(wpSnackbar, never()).show()
    }
}
