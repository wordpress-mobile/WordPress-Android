package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.R
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class BloggingPromptsOnboardingUiStateMapper @Inject constructor() {

    fun mapReady(): Ready = Ready(
            prompt = UiStringRes(R.string.blogging_prompts_onboarding_card_prompt),
            answeredUsers = emptyList(),
            numberOfAnswers = 19,
            isAnswered = false
    )
}
