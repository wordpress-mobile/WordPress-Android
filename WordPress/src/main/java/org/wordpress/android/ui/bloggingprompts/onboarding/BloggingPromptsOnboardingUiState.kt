package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.annotation.StringRes

sealed class BloggingPromptsOnboardingUiState {
    data class Ready(
        @StringRes val promptRes: Int,
        @StringRes val answersRes: Int,
        val answersCount: Int,
        @StringRes val contentTopRes: Int,
        @StringRes val contentBottomRes: Int,
        @StringRes val contentNoteTitle: Int,
        @StringRes val contentNoteContent: Int
    ) : BloggingPromptsOnboardingUiState()
}
