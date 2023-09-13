package org.wordpress.android.ui.mysite.personalisation

import androidx.annotation.StringRes

data class DashboardCardState(
    @StringRes val title: Int,
    @StringRes val description: Int? = null,
    val enabled: Boolean = false,
    val cardType: CardType
)


enum class CardType(val order: Int) {
    STATS(0),
    DRAFT_POSTS(1),
    SCHEDULED_POSTS(2),
    BLAZE(3),
    BLOGGING_PROMPTS(4),
    NEXT_STEPS(5),
    PAGES(6),
    ACTIVITY_LOG(7)
}
