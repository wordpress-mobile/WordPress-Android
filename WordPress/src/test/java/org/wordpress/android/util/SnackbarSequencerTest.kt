package org.wordpress.android.util

import android.app.Activity
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.material.snackbar.Snackbar
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.widgets.WPSnackbar
import org.wordpress.android.widgets.WPSnackbarWrapper
import java.lang.ref.WeakReference

private const val TEST_MESSAGE = "This is a test message"

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SnackbarSequencerTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()
    @Rule @JvmField var thrown2: ExpectedException = ExpectedException.none()

    @Mock lateinit var wpSnackbarWrapper: WPSnackbarWrapper
    @Mock lateinit var wpSnackbar: WPSnackbar
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
