package org.wordpress.android.ui.sitecreation.verticals

import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker

@RunWith(MockitoJUnitRunner::class)
class SiteCreationIntentsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var analyticsTracker: SiteCreationTracker
    @Mock lateinit var dispatcher: CoroutineDispatcher
    @Mock private lateinit var resources: Resources

    private lateinit var viewModel: SiteCreationIntentsViewModel

    @Before
    fun setUp() {
        viewModel = SiteCreationIntentsViewModel(analyticsTracker, dispatcher)
        whenever(resources.getStringArray(any())).thenReturn(emptyArray())
    }

    @Test
    fun `when the screen starts an analytics event is emitted`() {
        viewModel.start()
        verify(analyticsTracker).trackSiteIntentQuestionViewed()
    }

    @Test
    fun `when the skip button is pressed an analytics event is emitted`() {
        viewModel.onSkipPressed()
        verify(analyticsTracker).trackSiteIntentQuestionSkipped()
    }

    @Test
    fun `when the user scroll beyond a threshold the title becomes visible`() {
        viewModel.initializeFromResources(resources)
        viewModel.onAppBarOffsetChanged(9, 10)
        assertThat(viewModel.uiState.value?.isAppBarTitleVisible).isEqualTo(true)
    }

    @Test
    fun `when the user scroll below a threshold the title remains hidden`() {
        viewModel.initializeFromResources(resources)
        viewModel.onAppBarOffsetChanged(11, 10)
        assertThat(viewModel.uiState.value?.isAppBarTitleVisible).isEqualTo(false)
    }

    @Test
    fun `when the search input is focused an analytics event is emitted and the ui is updated`() {
        viewModel.initializeFromResources(resources)
        viewModel.onSearchInputFocused()
        verify(analyticsTracker).trackSiteIntentQuestionSearchFocused()
        assertThat(viewModel.uiState.value?.isAppBarTitleVisible).isEqualTo(true)
        assertThat(viewModel.uiState.value?.isHeaderVisible).isEqualTo(false)
    }
}
