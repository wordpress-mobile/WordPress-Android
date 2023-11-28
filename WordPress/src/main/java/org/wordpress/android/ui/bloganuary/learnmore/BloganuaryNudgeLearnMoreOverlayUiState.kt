package org.wordpress.android.ui.bloganuary.learnmore

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

data class BloganuaryNudgeLearnMoreOverlayUiState(
    val noteText: UiString,
    val action: BloganuaryNudgeLearnMoreOverlayAction,
)

enum class BloganuaryNudgeLearnMoreOverlayAction(
    @StringRes val textRes: Int,
    val analyticsLabel: String,
) {
    DISMISS(
        textRes = R.string.bloganuary_dashboard_nudge_overlay_action_dismiss,
        analyticsLabel = "dismiss",
    ),
    TURN_ON_PROMPTS(
        textRes = R.string.bloganuary_dashboard_nudge_overlay_action_turn_on_prompts,
        analyticsLabel = "turn_on_prompts",
    ),
}
