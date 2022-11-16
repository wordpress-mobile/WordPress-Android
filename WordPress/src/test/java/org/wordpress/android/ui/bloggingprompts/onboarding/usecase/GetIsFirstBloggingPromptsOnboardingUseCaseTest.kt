package org.wordpress.android.ui.bloggingprompts.onboarding.usecase

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.ui.prefs.AppPrefsWrapper

class GetIsFirstBloggingPromptsOnboardingUseCaseTest {
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val classToTest = GetIsFirstBloggingPromptsOnboardingUseCase(appPrefsWrapper)

    @Test
    fun `Should get is first blogging prompts onboarding when execute is called`() {
        classToTest.execute()
        verify(appPrefsWrapper).getIsFirstBloggingPromptsOnboarding()
    }
}
