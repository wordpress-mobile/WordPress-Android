package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel

enum class BloggingPromptAttribution(
    val value: String,
    @StringRes val contentRes: Int,
    @DrawableRes val iconRes: Int,
    val isContainerClickable: Boolean
) {
    NO_ATTRIBUTION("", -1, -1, false),
    DAY_ONE(
        "dayone",
        R.string.my_site_blogging_prompt_card_attribution_day_one,
        R.drawable.ic_dayone_24dp,
        true
    ),
    BLOGANUARY(
        "bloganuary",
        R.string.my_site_blogging_prompt_card_attribution_bloganuary,
        R.drawable.ic_bloganuary_24dp,
        false
    );

    companion object {
        private fun fromString(value: String): BloggingPromptAttribution = entries
            .firstOrNull { it.value == value }
            ?: NO_ATTRIBUTION

        /**
         * Returns the [BloggingPromptAttribution] for the given [BloggingPromptModel], prioritizing the content of the
         * [BloggingPromptModel.bloganuaryId] field before the actual [BloggingPromptModel.attribution] field for
         * detecting [BLOGANUARY] attribution.
         *
         * This is a workaround for the fact that the [BloggingPromptModel.attribution] field is not going to be used
         * for the BLOGANUARY campaign at this point, but might be in the future.
         */
        fun fromPrompt(
            prompt: BloggingPromptModel
        ): BloggingPromptAttribution = if (!prompt.bloganuaryId.isNullOrBlank()) {
            BLOGANUARY
        } else {
            fromString(prompt.attribution)
        }
    }
}
