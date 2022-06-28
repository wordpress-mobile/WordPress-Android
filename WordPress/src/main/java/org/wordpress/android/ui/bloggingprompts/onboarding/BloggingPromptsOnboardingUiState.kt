package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.annotation.StringRes
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem

sealed class BloggingPromptsOnboardingUiState {
    data class Ready(
        @StringRes val promptRes: Int,
        val respondents: List<TrainOfAvatarsItem>,
        @StringRes val contentTopRes: Int,
        @StringRes val contentBottomRes: Int,
        @StringRes val contentNoteTitle: Int,
        @StringRes val contentNoteContent: Int,
        @StringRes val primaryButtonLabel: Int,
        val isPrimaryButtonVisible: Boolean,
        val onPrimaryButtonClick: () -> Unit,
        @StringRes val secondaryButtonLabel: Int,
        val isSecondaryButtonVisible: Boolean,
        val onSecondaryButtonClick: () -> Unit
    ) : BloggingPromptsOnboardingUiState()
}
