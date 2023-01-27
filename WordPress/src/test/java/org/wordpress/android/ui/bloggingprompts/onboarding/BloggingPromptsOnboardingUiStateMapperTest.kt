package org.wordpress.android.ui.bloggingprompts.onboarding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.utils.UiString.UiStringPluralRes
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.config.BloggingPromptsEnhancementsFeatureConfig

@ExperimentalCoroutinesApi
class BloggingPromptsOnboardingUiStateMapperTest : BaseUnitTest() {
    @Mock
    lateinit var bloggingPromptsEnhancementsFeatureConfig: BloggingPromptsEnhancementsFeatureConfig

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
            R.attr.colorOnSurface
        )
    )

    private val expectedRespondentsEnhancements = listOf(
        AvatarItem(""),
        AvatarItem(""),
        AvatarItem(""),
        TrailingLabelTextItem(
            UiStringRes(
                string.my_site_blogging_prompt_card_view_answers
            ),
            R.attr.colorOnSurface
        )
    )

    private val primaryButtonListener: () -> Unit = {}
    private val secondaryButtonListener: () -> Unit = {}

    private fun expectedOnboardingDialogReadyState(enhancementsEnabled: Boolean) = Ready(
        promptRes = string.blogging_prompts_onboarding_card_prompt,
        respondents = if (enhancementsEnabled) expectedRespondentsEnhancements else expectedRespondents,
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

    private fun expectedInformationDialogReadyState(enhancementsEnabled: Boolean) = Ready(
        promptRes = string.blogging_prompts_onboarding_card_prompt,
        respondents = if (enhancementsEnabled) expectedRespondentsEnhancements else expectedRespondents,
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

    @Before
    fun setUp() {
        classToTest = BloggingPromptsOnboardingUiStateMapper(bloggingPromptsEnhancementsFeatureConfig)
    }

    @Test
    fun `Should return correct Ready state for ONBOARDING type dialog when enhancements are turned off`() {
        val enhancementsEnabled = false
        whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(enhancementsEnabled)

        val actual = classToTest.mapReady(ONBOARDING, primaryButtonListener, secondaryButtonListener)
        val expected = expectedOnboardingDialogReadyState(enhancementsEnabled)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state for INFORMATION type dialog when enhancements are turned off`() {
        val enhancementsEnabled = false
        whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(enhancementsEnabled)

        val actual = classToTest.mapReady(INFORMATION, primaryButtonListener, secondaryButtonListener)
        val expected = expectedInformationDialogReadyState(enhancementsEnabled)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state for ONBOARDING type dialog when enhancements are turned on`() {
        val enhancementsEnabled = true
        whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(enhancementsEnabled)

        val actual = classToTest.mapReady(ONBOARDING, primaryButtonListener, secondaryButtonListener)
        val expected = expectedOnboardingDialogReadyState(enhancementsEnabled)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state for INFORMATION type dialog when enhancements are turned on`() {
        val enhancementsEnabled = true
        whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(enhancementsEnabled)

        val actual = classToTest.mapReady(INFORMATION, primaryButtonListener, secondaryButtonListener)
        val expected = expectedInformationDialogReadyState(enhancementsEnabled)
        assertThat(actual).isEqualTo(expected)
    }
}
