package org.wordpress.android.ui.bloggingprompts.onboarding.usecase

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject

class GetIsFirstBloggingPromptsOnboardingUseCase @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper
) {
    fun execute(): Boolean = appPrefsWrapper.getIsFirstBloggingPromptsOnboarding()
}
