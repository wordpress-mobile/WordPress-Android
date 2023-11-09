package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R

enum class BloggingPromptAttribution(
    @StringRes val contentRes: Int,
    @DrawableRes val iconRes: Int,
) {
    NO_ATTRIBUTION(-1, -1),
    DAY_ONE(
        R.string.my_site_blogging_prompt_card_attribution_dayone,
        R.drawable.ic_dayone_24dp,
    ),
    BLOGANUARY(
        R.string.my_site_blogging_prompt_card_attribution_bloganuary,
        R.drawable.ic_bloganuary_24dp,
    );

    companion object {
        fun fromString(value: String): BloggingPromptAttribution = when (value) {
            "dayone" -> DAY_ONE
            "bloganuary" -> BLOGANUARY
            else -> NO_ATTRIBUTION
        }
    }
}
