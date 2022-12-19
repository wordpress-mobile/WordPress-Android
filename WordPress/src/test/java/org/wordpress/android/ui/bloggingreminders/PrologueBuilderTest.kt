package org.wordpress.android.ui.bloggingreminders

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction.Companion
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class PrologueBuilderTest {
    @Mock lateinit var bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig
    private lateinit var prologueBuilder: PrologueBuilder
    private var confirmed = false

    private val onConfirm: (Boolean) -> Unit = {
        confirmed = true
    }

    @Before
    fun setUp() {
        prologueBuilder = PrologueBuilder(bloggingPromptsFeatureConfig)
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        confirmed = false
    }

    @Test
    fun `builds correct UI model`() {
        val uiModel = prologueBuilder.buildUiItems()

        assertUiModel(uiModel)
    }

    @Test
    fun `builds correct UI model for settings`() {
        val uiModel = prologueBuilder.buildUiItemsForSettings()

        assertUiModelForSettings(uiModel)
    }

    @Test
    fun `builds correct UI model when prompts feature is on`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)

        val uiModel = prologueBuilder.buildUiItems()

        assertBloggingPromptsModel(uiModel)
    }

    @Test
    fun `builds correct UI model for settings when prompts feature is on`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)

        val uiModel = prologueBuilder.buildUiItemsForSettings()

        assertBloggingPromptsModel(uiModel) // currently it's same as regular UI model
    }

    @Test
    fun `builds primary button`() {
        val isFirstTimeFlow = true
        val primaryButton = prologueBuilder.buildPrimaryButton(isFirstTimeFlow, onConfirm)

        assertThat(primaryButton).isEqualTo(
                PrimaryButton(
                        UiStringRes(R.string.set_your_blogging_reminders_button),
                        true,
                        Companion.create(isFirstTimeFlow, onConfirm)
                )
        )
    }

    @Test
    fun `click on primary button confirms selection`() {
        val primaryButton = prologueBuilder.buildPrimaryButton(true, onConfirm)

        primaryButton.onClick.click()

        assertThat(confirmed).isTrue()
    }

    private fun assertUiModel(
        uiModel: List<BloggingRemindersItem>
    ) {
        assertThat(uiModel[0]).isEqualTo(Illustration(R.drawable.img_illustration_celebration_150dp))
        assertThat(uiModel[1]).isEqualTo(Title(UiStringRes(R.string.set_your_blogging_reminders_title)))
        assertThat(uiModel[2]).isEqualTo(
                HighEmphasisText(
                        UiStringRes(R.string.post_publishing_set_up_blogging_reminders_message)
                )
        )
    }

    private fun assertUiModelForSettings(
        uiModel: List<BloggingRemindersItem>
    ) {
        assertThat(uiModel[0]).isEqualTo(Illustration(R.drawable.img_illustration_celebration_150dp))
        assertThat(uiModel[1]).isEqualTo(Title(UiStringRes(R.string.set_your_blogging_reminders_title)))
        assertThat(uiModel[2]).isEqualTo(
                HighEmphasisText(
                        UiStringRes(R.string.set_up_blogging_reminders_message)
                )
        )
    }

    private fun assertBloggingPromptsModel(
        uiModel: List<BloggingRemindersItem>
    ) {
        assertThat(uiModel[0]).isEqualTo(Illustration(R.drawable.img_illustration_celebration_150dp))
        assertThat(uiModel[1]).isEqualTo(Title(UiStringRes(R.string.set_your_blogging_prompts_title)))
        assertThat(uiModel[2]).isEqualTo(
                HighEmphasisText(
                        UiStringRes(R.string.post_publishing_set_up_blogging_prompts_message)
                )
        )
    }
}
