package org.wordpress.android.ui.sitecreation.verticals

import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
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
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class SiteCreationIntentsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var analyticsTracker: SiteCreationTracker
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var dispatcher: CoroutineDispatcher
    @Mock private lateinit var resources: Resources

    private lateinit var viewModel: SiteCreationIntentsViewModel
    private lateinit var searchResultsProvider: VerticalsSearchResultsProvider

    @Before
    fun setUp() {
        whenever(resources.getStringArray(any())).thenReturn(emptyArray())
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
        searchResultsProvider = VerticalsSearchResultsProvider(localeManagerWrapper)
        viewModel = SiteCreationIntentsViewModel(analyticsTracker, searchResultsProvider, dispatcher)
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
    fun `when the back button is pressed an analytics event is emitted`() {
        viewModel.onBackPressed()
        verify(analyticsTracker).trackSiteIntentQuestionCanceled()
    }

    @Test
    fun `when an item is tapped an analytics event is emitted`() {
        val slug = "test1"
        viewModel.initializeFromResources(resources)
        viewModel.intentSelected(slug, slug)
        verify(analyticsTracker).trackSiteIntentQuestionVerticalSelected(slug)
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

    @Test
    fun `when the continue button is pressed the analytics event is emitted with the search input value`() {
        val valueOfSearchInput = "test vertical"
        viewModel.initializeFromResources(resources)
        viewModel.onSearchTextChanged(valueOfSearchInput)
        viewModel.onContinuePressed()
        verify(analyticsTracker).trackSiteIntentQuestionContinuePressed(eq(valueOfSearchInput))
    }

    @Test
    fun `when the user types in the search field the results are filtered`() {
        val valueOfSearchInput = "test1"
        val matchingItem = "Test1"
        whenever(resources.getStringArray(any())).thenReturn(arrayOf(matchingItem, "Test2", "Test3"))
        viewModel.initializeFromResources(resources)
        viewModel.onSearchTextChanged(valueOfSearchInput)
        assertThat(viewModel.uiState.value?.content?.items?.size).isEqualTo(1)
        assertThat(viewModel.uiState.value?.content?.items?.firstOrNull()?.verticalText).isEqualTo("Test1")
    }

    @Test
    fun `when there are matching search results the continue button is hidden`() {
        val valueOfSearchInput = "test1"
        val matchingItem = "Test1"
        whenever(resources.getStringArray(any())).thenReturn(arrayOf(matchingItem, "Test2", "Test3"))
        viewModel.initializeFromResources(resources)
        viewModel.onSearchTextChanged(valueOfSearchInput)
        assertThat(viewModel.uiState.value?.isContinueButtonVisible).isEqualTo(false)
    }

    @Test
    fun `when there are no search results the continue button is visible`() {
        val valueOfSearchInput = "test1"
        whenever(resources.getStringArray(any())).thenReturn(arrayOf("Test2", "Test3"))
        viewModel.initializeFromResources(resources)
        viewModel.onSearchTextChanged(valueOfSearchInput)
        assertThat(viewModel.uiState.value?.content?.items?.size).isEqualTo(0)
        assertThat(viewModel.uiState.value?.isContinueButtonVisible).isEqualTo(true)
    }
}
