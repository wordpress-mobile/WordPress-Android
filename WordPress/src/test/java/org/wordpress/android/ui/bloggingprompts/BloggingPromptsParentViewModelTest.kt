package org.wordpress.android.ui.bloggingprompts

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel

class BloggingPromptsParentViewModelTest : BaseUnitTest() {
    private val analyticsTracker: BloggingPromptsAnalyticsTracker = mock()
    private val savedStateHandle: BloggingPromptsSiteProvider = mock()
    private val site: SiteModel = mock()

    private val viewModel = BloggingPromptsParentViewModel(
            handle = savedStateHandle,
            analyticsTracker = analyticsTracker
    )

    @Test
    fun `Should save siteModel when started`() {
        viewModel.start(site)

        verify(savedStateHandle).setSite(site)
    }

    @Test
    fun `Should track screen accessed when page opened`() {
        whenever(savedStateHandle.getSite()).thenReturn(site)

        val promptSection = promptsSections[0]
        viewModel.onOpen(promptSection)

        verify(analyticsTracker).trackScreenAccessed(site, promptSection)
    }

    @Test
    fun `Should track tab selected when changing tabs`() {
        whenever(savedStateHandle.getSite()).thenReturn(site)

        val promptSection = promptsSections[2]
        viewModel.onSectionSelected(promptSection)

        verify(analyticsTracker).trackTabSelected(site, promptSection)
    }
}
