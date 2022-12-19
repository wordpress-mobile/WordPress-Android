package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.utils.UiString.UiStringPluralRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class BloggingPromptCardBuilder @Inject constructor() {
    fun build(params: BloggingPromptCardBuilderParams) = params.bloggingPrompt?.let {
        val trailingLabel = UiStringPluralRes(
                0,
                R.string.my_site_blogging_prompt_card_number_of_answers_one,
                R.string.my_site_blogging_prompt_card_number_of_answers_other,
                params.bloggingPrompt.respondentsCount
        )

        val avatarsTrain = params.bloggingPrompt.respondentsAvatarUrls.map { respondent -> AvatarItem(respondent) }
                .toMutableList<TrainOfAvatarsItem>()
                .also { list -> list.add(TrailingLabelTextItem(trailingLabel, R.attr.colorOnSurface)) }

        BloggingPromptCardWithData(
                prompt = UiStringText(it.text),
                respondents = avatarsTrain,
                numberOfAnswers = params.bloggingPrompt.respondentsCount,
                isAnswered = params.bloggingPrompt.isAnswered,
                promptId = params.bloggingPrompt.id,
                attribution = BloggingPromptAttribution.fromString(params.bloggingPrompt.attribution),
                showViewMoreAction = params.showViewMoreAction,
                onShareClick = params.onShareClick,
                onAnswerClick = params.onAnswerClick,
                onSkipClick = params.onSkipClick,
                onViewMoreClick = params.onViewMoreClick,
        )
    }
}
