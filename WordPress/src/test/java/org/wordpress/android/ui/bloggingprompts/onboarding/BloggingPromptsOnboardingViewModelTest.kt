package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.bloggingprompts.BloggingPrompt
import org.wordpress.android.test
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor

class BloggingPromptsOnboardingViewModelTest : BaseUnitTest() {
    private val uiStateMapper: BloggingPromptsOnboardingUiStateMapper = mock()
    private val siteStore: SiteStore = mock()
    private val bloggingPromptsOnboardingViewModel = BloggingPromptsOnboardingViewModel(siteStore, uiStateMapper)
    private val actionObserver: Observer<BloggingPromptsOnboardingAction> = mock()

    @Before
    fun setup() {
        bloggingPromptsOnboardingViewModel.action.observeForever(actionObserver)
    }

    @Test
    fun `Should execute GetBloggingPromptUseCase when start is called`() = test {
        bloggingPromptsOnboardingViewModel.start()
        verify(uiStateMapper).mapReady()
    }

    @Test
    fun `Should trigger OpenEditor action when onTryNow is called`() = test {
        val bloggingPrompt: BloggingPrompt = mock()
        bloggingPromptsOnboardingViewModel.start()
        bloggingPromptsOnboardingViewModel.onTryNowClick()
        verify(actionObserver).onChanged(OpenEditor(bloggingPrompt))
    }
}
