package org.wordpress.android.ui.bloggingprompts.onboarding

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.R

class BloggingPromptsOnboardingUiStateMapperTest {

    private val classToTest = BloggingPromptsOnboardingUiStateMapper()

    @Test
    fun `Should return correct Ready state string resource for promptRes`() {
        val actual = classToTest.mapReady().promptRes
        val expected = R.string.blogging_prompts_onboarding_card_prompt
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state string resource for answersRes`() {
        val actual = classToTest.mapReady().answersRes
        val expected = R.string.my_site_blogging_prompt_card_number_of_answers
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state answers count`() {
        val actual = classToTest.mapReady().answersCount
        val expected = 19
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state string resource for contentTopRes`() {
        val actual = classToTest.mapReady().contentTopRes
        val expected = R.string.blogging_prompts_onboarding_content_top
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state string resource for contentBottomRes`() {
        val actual = classToTest.mapReady().contentBottomRes
        val expected = R.string.blogging_prompts_onboarding_content_bottom
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state string resource for contentNoteTitle`() {
        val actual = classToTest.mapReady().contentNoteTitle
        val expected = R.string.blogging_prompts_onboarding_content_note_title
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct Ready state string resource for contentNoteContent`() {
        val actual = classToTest.mapReady().contentNoteContent
        val expected = R.string.blogging_prompts_onboarding_content_note_content
        assertThat(actual).isEqualTo(expected)
    }
}
