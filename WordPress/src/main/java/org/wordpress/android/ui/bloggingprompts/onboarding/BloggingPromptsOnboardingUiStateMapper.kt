package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class BloggingPromptsOnboardingUiStateMapper @Inject constructor() {
    @Suppress("MagicNumber")
    fun mapReady(
        dialogType: DialogType,
        onPrimaryButtonClick: () -> Unit,
        onSecondaryButtonClick: () -> Unit
    ): Ready {
        val dummyRespondent = ""

        val dummyRespondents = listOf(
            dummyRespondent,
            dummyRespondent,
            dummyRespondent
        )

        val trailingLabel = UiStringRes(
                R.string.my_site_blogging_prompt_card_view_answers
            )

        val avatarsTrain = dummyRespondents.take(3).map { respondent -> AvatarItem(respondent) }
            .toMutableList<TrainOfAvatarsItem>()
            .also { list ->
                val labelColor = R.color.primary_emphasis_medium_selector
                list.add(TrailingLabelTextItem(trailingLabel, labelColor))
            }

        val primaryButtonLabel = when (dialogType) {
            ONBOARDING -> R.string.blogging_prompts_onboarding_try_it_now
            INFORMATION -> R.string.blogging_prompts_onboarding_got_it
        }

        return Ready(
            promptRes = R.string.blogging_prompts_onboarding_card_prompt,
            respondents = avatarsTrain,
            contentTopRes = R.string.blogging_prompts_onboarding_content_top,
            contentBottomRes = R.string.blogging_prompts_onboarding_content_bottom,
            contentNoteTitle = R.string.blogging_prompts_onboarding_content_note_title,
            contentNoteContent = R.string.blogging_prompts_onboarding_content_note_content,
            primaryButtonLabel = primaryButtonLabel,
            isPrimaryButtonVisible = true,
            onPrimaryButtonClick = onPrimaryButtonClick,
            secondaryButtonLabel = R.string.blogging_prompts_onboarding_remind_me,
            isSecondaryButtonVisible = dialogType == ONBOARDING,
            onSecondaryButtonClick = onSecondaryButtonClick
        )
    }
}
