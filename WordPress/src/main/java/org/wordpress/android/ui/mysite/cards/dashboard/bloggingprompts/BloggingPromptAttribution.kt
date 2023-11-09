package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R

enum class BloggingPromptAttribution(
    val value: String,
    @StringRes val contentRes: Int,
    @DrawableRes val iconRes: Int,
) {
    NO_ATTRIBUTION("", -1, -1),
    DAY_ONE(
        "dayone",
        R.string.my_site_blogging_prompt_card_attribution_dayone,
        R.drawable.ic_dayone_24dp,
    ),
    BLOGANUARY(
        "bloganuary",
        R.string.my_site_blogging_prompt_card_attribution_bloganuary,
        R.drawable.ic_bloganuary_24dp,
    );

    companion object {
        fun fromString(value: String): BloggingPromptAttribution = entries
            .firstOrNull { it.value == value }
            ?: NO_ATTRIBUTION
    }
}
