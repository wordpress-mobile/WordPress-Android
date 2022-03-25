package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class BloggingPromptCardBuilder @Inject constructor() {
    fun build(params: BloggingPromptCardBuilderParams) = params.bloggingPrompt?.let {
        BloggingPromptCardWithData(
                prompt = UiStringText(it.text),
                answeredUsers = emptyList(),
                numberOfAnswers = it.numberOfAnswers,
                isAnswered = false
        )
    }
}
