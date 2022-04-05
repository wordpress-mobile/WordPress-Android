package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.models.bloggingprompts.BloggingPromptRespondent
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class BloggingPromptsOnboardingUiStateMapper @Inject constructor() {
    fun mapReady(): Ready {
        val dummyRespondents = listOf(
                BloggingPromptRespondent(
                        54279365,
                        "https://0.gravatar.com/avatar/cec64efa352617" +
                                "c35743d8ed233ab410?s=96&d=identicon&r=G"
                ),
                BloggingPromptRespondent(
                        54279365,
                        "https://0.gravatar.com/avatar/cec64efa352617" +
                                "c35743d8ed233ab410?s=96&d=identicon&r=G"
                ),
                BloggingPromptRespondent(
                        54279365,
                        "https://0.gravatar.com/avatar/cec64efa352617" +
                                "c35743d8ed233ab410?s=96&d=identicon&r=G"
                )
        )

        val trailingLabel = UiStringResWithParams(
                string.my_site_blogging_prompt_card_number_of_answers,
                listOf(UiStringText(dummyRespondents.size.toString()))
        )

        val avatarsTrain = dummyRespondents.map { respondent ->
            AvatarItem(
                    respondent.userId,
                    respondent.avatarUrl
            )
        }.toMutableList<TrainOfAvatarsItem>().also { list -> list.add(TrailingLabelTextItem(trailingLabel)) }

        return Ready(
                promptRes = R.string.blogging_prompts_onboarding_card_prompt,
                respondents = avatarsTrain,
                contentTopRes = R.string.blogging_prompts_onboarding_content_top,
                contentBottomRes = R.string.blogging_prompts_onboarding_content_bottom,
                contentNoteTitle = R.string.blogging_prompts_onboarding_content_note_title,
                contentNoteContent = R.string.blogging_prompts_onboarding_content_note_content
        )
    }
}
