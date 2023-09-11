package org.wordpress.android.ui.bloggingprompts.onboarding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.utils.UiString.UiStringPluralRes
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.config.BloggingPromptsSocialFeature
import com.google.android.material.R as MaterialR

@ExperimentalCoroutinesApi
class BloggingPromptsOnboardingUiStateMapperTest : BaseUnitTest() {
    @Mock
    lateinit var bloggingPromptsSocialFeature: BloggingPromptsSocialFeature

    private lateinit var classToTest: BloggingPromptsOnboardingUiStateMapper

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
            MaterialR.attr.colorOnSurface
        )
    )

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

    private fun expectedOnboardingDialogReadyState(enhancementsEnabled: Boolean) = Ready(
        promptRes = R.string.blogging_prompts_onboarding_card_prompt,
        respondents = if (enhancementsEnabled) expectedRespondentsEnhancements else expectedRespondents,
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

    private fun expectedInformationDialogReadyState(enhancementsEnabled: Boolean) = Ready(
        promptRes = R.string.blogging_prompts_onboarding_card_prompt,
        respondents = if (enhancementsEnabled) expectedRespondentsEnhancements else expectedRespondents,
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
        classToTest = BloggingPromptsOnboardingUiStateMapper(bloggingPromptsSocialFeature)
    }

    @Test
    fun `Should return correct Ready state for ONBOARDING type dialog when enhancements are turned off`() {
        val socialEnabled = false
        whenever(bloggingPromptsSocialFeature.isEnabled()).thenReturn(socialEnabled)

        val actual = classToTest.mapReady(ONBOARDING, primaryButtonListener, secondaryButtonListener)
        val expected = expectedOnboardingDialogReadyState(socialEnabled)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state for INFORMATION type dialog when enhancements are turned off`() {
        val socialEnabled = false
        whenever(bloggingPromptsSocialFeature.isEnabled()).thenReturn(socialEnabled)

        val actual = classToTest.mapReady(INFORMATION, primaryButtonListener, secondaryButtonListener)
        val expected = expectedInformationDialogReadyState(socialEnabled)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state for ONBOARDING type dialog when enhancements are turned on`() {
        val socialEnabled = true
        whenever(bloggingPromptsSocialFeature.isEnabled()).thenReturn(socialEnabled)

        val actual = classToTest.mapReady(ONBOARDING, primaryButtonListener, secondaryButtonListener)
        val expected = expectedOnboardingDialogReadyState(socialEnabled)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state for INFORMATION type dialog when enhancements are turned on`() {
        val socialEnabled = true
        whenever(bloggingPromptsSocialFeature.isEnabled()).thenReturn(socialEnabled)

        val actual = classToTest.mapReady(INFORMATION, primaryButtonListener, secondaryButtonListener)
        val expected = expectedInformationDialogReadyState(socialEnabled)
        assertThat(actual).isEqualTo(expected)
    }
}
