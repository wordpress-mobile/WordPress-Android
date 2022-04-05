package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class BloggingPromptCardBuilder @Inject constructor() {
    fun build(params: BloggingPromptCardBuilderParams) = params.bloggingPrompt?.let {

        val respondents = params.bloggingPrompt.respondents

        val trailingLabel = UiStringResWithParams(
                R.string.my_site_blogging_prompt_card_number_of_answers,
                listOf(UiStringText(respondents.size.toString()))
        )

        val avatarsTrain = respondents.map { respondent ->
            AvatarItem(
                    respondent.userId,
                    respondent.avatarUrl
            )
        }.toMutableList<TrainOfAvatarsItem>().also { list -> list.add(TrailingLabelTextItem(trailingLabel)) }

        BloggingPromptCardWithData(
                prompt = UiStringText(it.text),
                respondents = avatarsTrain,
                numberOfAnswers = it.numberOfAnswers,
                isAnswered = false,
                onShareClick = params.onShareClick
        )
    }
}
