package org.wordpress.android.ui.bloggingprompts.onboarding.usecase

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.wordpress.android.ui.prefs.AppPrefsWrapper

class SaveFirstBloggingPromptsOnboardingUseCaseTest {
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val classToTest = SaveFirstBloggingPromptsOnboardingUseCase(appPrefsWrapper)

    @Test
    fun `Should save first blogging prompts onboarding when execute is called with TRUE`() {
        classToTest.execute(true)
        verify(appPrefsWrapper).saveFirstBloggingPromptsOnboarding(true)
    }

    @Test
    fun `Should save first blogging prompts onboarding when execute is called with FALSE`() {
        classToTest.execute(false)
        verify(appPrefsWrapper).saveFirstBloggingPromptsOnboarding(false)
    }
}
