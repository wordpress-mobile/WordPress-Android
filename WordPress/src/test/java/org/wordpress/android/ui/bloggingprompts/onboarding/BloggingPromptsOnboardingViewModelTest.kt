package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flow
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.bloggingprompts.BloggingPrompt
import org.wordpress.android.models.usecases.GetBloggingPromptUseCase
import org.wordpress.android.test
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor

class BloggingPromptsOnboardingViewModelTest : BaseUnitTest() {
    private val getBloggingPromptUseCase: GetBloggingPromptUseCase = mock()
    private val bloggingPromptsOnboardingViewModel = BloggingPromptsOnboardingViewModel(getBloggingPromptUseCase)
    private val actionObserver: Observer<BloggingPromptsOnboardingAction> = mock()

    @Before
    fun setup() {
        bloggingPromptsOnboardingViewModel.action.observeForever(actionObserver)
    }

    @Test
    fun `Should execute GetBloggingPromptUseCase when start is called`() = test {
        bloggingPromptsOnboardingViewModel.start()
        verify(getBloggingPromptUseCase).execute()
    }

    @Test
    fun `Should trigger OpenEditor action when onTryNow is called`() = test {
        val bloggingPrompt: BloggingPrompt = mock()
        whenever(getBloggingPromptUseCase.execute()).thenReturn(flow { emit(bloggingPrompt) })
        bloggingPromptsOnboardingViewModel.start()
        bloggingPromptsOnboardingViewModel.onTryNow()
        verify(actionObserver).onChanged(OpenEditor(bloggingPrompt))
    }
}
