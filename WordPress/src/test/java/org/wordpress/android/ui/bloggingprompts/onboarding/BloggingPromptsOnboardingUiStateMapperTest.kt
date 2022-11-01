package org.wordpress.android.ui.bloggingprompts.onboarding

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.utils.UiString.UiStringPluralRes

class BloggingPromptsOnboardingUiStateMapperTest {
    private val classToTest = BloggingPromptsOnboardingUiStateMapper()

    private val expectedRespondents = listOf(
            AvatarItem(""),
            AvatarItem(""),
            AvatarItem(""),
            TrailingLabelTextItem(
                    UiStringPluralRes(
                            0,
                            R.string.my_site_blogging_prompt_card_number_of_answers_one,
                            R.string.my_site_blogging_prompt_card_number_of_answers_other,
                            3
                    ),
                    R.attr.colorOnSurface
            )
    )

    private val primaryButtonListener: () -> Unit = {}
    private val secondaryButtonListener: () -> Unit = {}

    private val expectedOnboardingDialogReadyState = Ready(
            promptRes = string.blogging_prompts_onboarding_card_prompt,
            respondents = expectedRespondents,
            contentTopRes = string.blogging_prompts_onboarding_content_top,
            contentBottomRes = string.blogging_prompts_onboarding_content_bottom,
            contentNoteTitle = string.blogging_prompts_onboarding_content_note_title,
            contentNoteContent = string.blogging_prompts_onboarding_content_note_content,
            primaryButtonLabel = string.blogging_prompts_onboarding_try_it_now,
            isPrimaryButtonVisible = true,
            onPrimaryButtonClick = primaryButtonListener,
            secondaryButtonLabel = string.blogging_prompts_onboarding_remind_me,
            isSecondaryButtonVisible = true,
            onSecondaryButtonClick = secondaryButtonListener
    )

    private val expectedInformationDialogReadyState = Ready(
            promptRes = string.blogging_prompts_onboarding_card_prompt,
            respondents = expectedRespondents,
            contentTopRes = string.blogging_prompts_onboarding_content_top,
            contentBottomRes = string.blogging_prompts_onboarding_content_bottom,
            contentNoteTitle = string.blogging_prompts_onboarding_content_note_title,
            contentNoteContent = string.blogging_prompts_onboarding_content_note_content,
            primaryButtonLabel = string.blogging_prompts_onboarding_got_it,
            isPrimaryButtonVisible = true,
            onPrimaryButtonClick = primaryButtonListener,
            secondaryButtonLabel = string.blogging_prompts_onboarding_remind_me,
            isSecondaryButtonVisible = false,
            onSecondaryButtonClick = secondaryButtonListener
    )

    @Test
    fun `Should return correct Ready state for ONBOARDING type dialog`() {
        val actual = classToTest.mapReady(ONBOARDING, primaryButtonListener, secondaryButtonListener)
        val expected = expectedOnboardingDialogReadyState
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state for INFORMATION type dialog`() {
        val actual = classToTest.mapReady(INFORMATION, primaryButtonListener, secondaryButtonListener)
        val expected = expectedInformationDialogReadyState
        assertThat(actual).isEqualTo(expected)
    }
}
