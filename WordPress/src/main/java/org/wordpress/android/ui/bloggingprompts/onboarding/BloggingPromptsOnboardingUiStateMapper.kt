package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.R
import org.wordpress.android.models.bloggingprompts.BloggingPromptRespondent
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.utils.UiString.UiStringResPlural
import javax.inject.Inject

class BloggingPromptsOnboardingUiStateMapper @Inject constructor() {
    @Suppress("MagicNumber")
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

        val trailingLabel = UiStringResPlural(
                R.plurals.my_site_blogging_prompt_card_number_of_answers,
                dummyRespondents.size
        )

        val avatarsTrain = dummyRespondents.map { respondent ->
            AvatarItem(
                    respondent.userId,
                    respondent.avatarUrl
            )
        }
                .toMutableList<TrainOfAvatarsItem>()
                .also { list -> list.add(TrailingLabelTextItem(trailingLabel, R.attr.colorPrimary)) }

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
