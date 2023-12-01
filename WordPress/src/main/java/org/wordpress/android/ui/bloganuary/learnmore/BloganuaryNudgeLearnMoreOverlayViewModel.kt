package org.wordpress.android.ui.bloganuary.learnmore

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class BloganuaryNudgeLearnMoreOverlayViewModel @Inject constructor() : ViewModel() {
    private val _dismissDialog = SingleLiveEvent<Unit>()
    val dismissDialog = _dismissDialog as LiveData<Unit>

    fun getUiState(isPromptsEnabled: Boolean): BloganuaryNudgeLearnMoreOverlayUiState {
        val noteText: UiString
        val action: BloganuaryNudgeLearnMoreOverlayAction

        if (isPromptsEnabled) {
            noteText = UiStringRes(R.string.bloganuary_dashboard_nudge_overlay_note_prompts_enabled)
            action = BloganuaryNudgeLearnMoreOverlayAction.DISMISS
        } else {
            noteText = UiStringRes(R.string.bloganuary_dashboard_nudge_overlay_note_prompts_disabled)
            action = BloganuaryNudgeLearnMoreOverlayAction.TURN_ON_PROMPTS
        }

        return BloganuaryNudgeLearnMoreOverlayUiState(
            noteText = noteText,
            action = action,
        )
    }

    fun onDialogShown() {
        // TODO thomashortadev add analytics
    }

    fun onActionClick(action: BloganuaryNudgeLearnMoreOverlayAction) {
        // TODO thomashortadev add analytics
        when (action) {
            BloganuaryNudgeLearnMoreOverlayAction.DISMISS -> {
                _dismissDialog.value = Unit
            }

            BloganuaryNudgeLearnMoreOverlayAction.TURN_ON_PROMPTS -> {
                // TODO thomashortadev add action to turn on prompts
                _dismissDialog.value = Unit
            }
        }
    }

    fun onCloseClick() {
        _dismissDialog.value = Unit
    }

    fun onDialogDismissed() {
        // TODO thomashortadev add analytics
    }
}
