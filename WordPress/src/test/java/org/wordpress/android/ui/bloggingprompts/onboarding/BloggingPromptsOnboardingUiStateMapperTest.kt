package org.wordpress.android.ui.bloggingprompts.onboarding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.utils.UiString.UiStringRes

@ExperimentalCoroutinesApi
class BloggingPromptsOnboardingUiStateMapperTest : BaseUnitTest() {
    private lateinit var classToTest: BloggingPromptsOnboardingUiStateMapper

    private val expectedRespondentsEnhancements = listOf(
        AvatarItem(""),
        AvatarItem(""),
        AvatarItem(""),
        TrailingLabelTextItem(
            UiStringRes(
                R.string.my_site_blogging_prompt_card_view_answers
            ),
            R.color.primary_emphasis_medium_selector
        )
    )

    private val primaryButtonListener: () -> Unit = {}
    private val secondaryButtonListener: () -> Unit = {}

    private fun expectedOnboardingDialogReadyState() = Ready(
        promptRes = R.string.blogging_prompts_onboarding_card_prompt,
        respondents =  expectedRespondentsEnhancements,
        contentTopRes = R.string.blogging_prompts_onboarding_content_top,
        contentBottomRes = R.string.blogging_prompts_onboarding_content_bottom,
        contentNoteTitle = R.string.blogging_prompts_onboarding_content_note_title,
        contentNoteContent = R.string.blogging_prompts_onboarding_content_note_content,
        primaryButtonLabel = R.string.blogging_prompts_onboarding_try_it_now,
        isPrimaryButtonVisible = true,
        onPrimaryButtonClick = primaryButtonListener,
        secondaryButtonLabel = R.string.blogging_prompts_onboarding_remind_me,
        isSecondaryButtonVisible = true,
        onSecondaryButtonClick = secondaryButtonListener
    )

    private fun expectedInformationDialogReadyState() = Ready(
        promptRes = R.string.blogging_prompts_onboarding_card_prompt,
        respondents = expectedRespondentsEnhancements,
        contentTopRes = R.string.blogging_prompts_onboarding_content_top,
        contentBottomRes = R.string.blogging_prompts_onboarding_content_bottom,
        contentNoteTitle = R.string.blogging_prompts_onboarding_content_note_title,
        contentNoteContent = R.string.blogging_prompts_onboarding_content_note_content,
        primaryButtonLabel = R.string.blogging_prompts_onboarding_got_it,
        isPrimaryButtonVisible = true,
        onPrimaryButtonClick = primaryButtonListener,
        secondaryButtonLabel = R.string.blogging_prompts_onboarding_remind_me,
        isSecondaryButtonVisible = false,
        onSecondaryButtonClick = secondaryButtonListener
    )

    @Before
    fun setUp() {
        classToTest = BloggingPromptsOnboardingUiStateMapper()
    }


    @Test
    fun `Should return correct Ready state for ONBOARDING type dialog`() {
        val actual = classToTest.mapReady(ONBOARDING, primaryButtonListener, secondaryButtonListener)
        val expected = expectedOnboardingDialogReadyState()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state for INFORMATION type dialog`() {
        val actual = classToTest.mapReady(INFORMATION, primaryButtonListener, secondaryButtonListener)
        val expected = expectedInformationDialogReadyState()
        assertThat(actual).isEqualTo(expected)
    }
}
