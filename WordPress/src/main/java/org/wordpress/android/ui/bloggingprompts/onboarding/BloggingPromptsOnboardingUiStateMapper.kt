package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.R
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import javax.inject.Inject

class BloggingPromptsOnboardingUiStateMapper @Inject constructor() {

    fun mapReady(): Ready = Ready(
            promptRes = R.string.blogging_prompts_onboarding_card_prompt,
            answersRes = R.string.my_site_blogging_prompt_card_number_of_answers,
            answersCount = ANSWER_COUNT,
            contentTopRes = R.string.blogging_prompts_onboarding_content_top,
            contentBottomRes = R.string.blogging_prompts_onboarding_content_bottom,
            contentNoteTitle = R.string.blogging_prompts_onboarding_content_note_title,
            contentNoteContent = R.string.blogging_prompts_onboarding_content_note_content
    )
}

private const val ANSWER_COUNT = 19
