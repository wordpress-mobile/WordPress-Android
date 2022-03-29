package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData.AnsweredUser
import org.wordpress.android.ui.utils.UiString

sealed class BloggingPromptsOnboardingUiState {
    data class Ready(
        val prompt: UiString,
        val answeredUsers: List<AnsweredUser>,
        val numberOfAnswers: Int,
        val isAnswered: Boolean
    ) : BloggingPromptsOnboardingUiState()
}
