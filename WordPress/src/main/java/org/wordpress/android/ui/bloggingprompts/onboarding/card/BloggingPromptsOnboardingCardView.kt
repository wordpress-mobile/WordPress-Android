package org.wordpress.android.ui.bloggingprompts.onboarding.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteBloggingPromptCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.getText
import org.wordpress.android.util.extensions.orEmpty

class BloggingPromptsOnboardingCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var binding: MySiteBloggingPromptCardBinding = MySiteBloggingPromptCardBinding.inflate(
            LayoutInflater.from(context)
    )

    fun bind(card: BloggingPromptCardWithData) {
        setupPromptContent(card.prompt)
        setupNumberOfAnswers(card.numberOfAnswers)
    }

    private fun setupPromptContent(prompt: UiString) {
        binding.promptContent.text = prompt.getText(context).orEmpty()
    }

    private fun setupNumberOfAnswers(numberOfAnswers: Int) {
        val numberOfAnswersLabel = context.getString(
                R.string.my_site_blogging_prompt_card_number_of_answers, numberOfAnswers
        )
        binding.numberOfAnswers.text = numberOfAnswersLabel
    }
}
