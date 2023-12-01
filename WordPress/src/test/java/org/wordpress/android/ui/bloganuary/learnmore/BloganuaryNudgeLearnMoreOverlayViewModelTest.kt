package org.wordpress.android.ui.bloganuary.learnmore

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.bloganuary.learnmore.BloganuaryNudgeLearnMoreOverlayViewModel.DismissEvent
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.utils.UiString.UiStringRes

@OptIn(ExperimentalCoroutinesApi::class)
class BloganuaryNudgeLearnMoreOverlayViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var promptsSettingsHelper: BloggingPromptsSettingsHelper

    lateinit var viewModel: BloganuaryNudgeLearnMoreOverlayViewModel

    @Before
    fun setUp() {
        viewModel = BloganuaryNudgeLearnMoreOverlayViewModel(promptsSettingsHelper)
    }

    @Test
    fun `getUiState should return correct UiState when prompts are enabled`() {
        val uiState = viewModel.getUiState(isPromptsEnabled = true)

        assertThat(uiState).isEqualTo(
            BloganuaryNudgeLearnMoreOverlayUiState(
                noteText = UiStringRes(R.string.bloganuary_dashboard_nudge_overlay_note_prompts_enabled),
                action = BloganuaryNudgeLearnMoreOverlayAction.DISMISS,
            )
        )
    }

    @Test
    fun `getUiState should return correct UiState when prompts are disabled`() {
        val uiState = viewModel.getUiState(isPromptsEnabled = false)

        assertThat(uiState).isEqualTo(
            BloganuaryNudgeLearnMoreOverlayUiState(
                noteText = UiStringRes(R.string.bloganuary_dashboard_nudge_overlay_note_prompts_disabled),
                action = BloganuaryNudgeLearnMoreOverlayAction.TURN_ON_PROMPTS,
            )
        )
    }

    @Test
    fun `onActionClick should dismiss dialog when action is DISMISS`() {
        viewModel.onActionClick(BloganuaryNudgeLearnMoreOverlayAction.DISMISS)

        assertThat(viewModel.dismissDialog.value).isEqualTo(DismissEvent())
    }

    @Test
    fun `onActionClick should turn on blogging prompts when action is TURN_ON_PROMPTS`() = test {
        viewModel.onActionClick(BloganuaryNudgeLearnMoreOverlayAction.TURN_ON_PROMPTS)

        verify(promptsSettingsHelper).updatePromptsCardEnabledForCurrentSite(true)
    }

    @Test
    fun `onActionClick should dismiss dialog requesting refresh when action is TURN_ON_PROMPTS`() {
        viewModel.onActionClick(BloganuaryNudgeLearnMoreOverlayAction.TURN_ON_PROMPTS)

        assertThat(viewModel.dismissDialog.value).isEqualTo(DismissEvent(refreshDashboard = true))
    }

    @Test
    fun `onCloseClick should dismiss dialog`() {
        viewModel.onCloseClick()

        assertThat(viewModel.dismissDialog.value).isEqualTo(DismissEvent())
    }

    // region Analytics
    @Test
    @Ignore("WIP")
    fun `onDialogShown should track analytics`() {
        viewModel.onDialogShown()

        // TODO thomashortadev assert analytics event is tracked
    }

    @Test
    @Ignore("WIP")
    fun `onActionClick should track analytics`() {
        BloganuaryNudgeLearnMoreOverlayAction.entries.forEach {
            viewModel.onActionClick(it)
            // TODO thomashortadev assert analytics event is tracked
        }
    }

    @Test
    @Ignore("WIP")
    fun `onDialogDismissed should track analytics`() {
        viewModel.onDialogDismissed()

        // TODO thomashortadev assert analytics event is tracked
    }
    // endregion
}
