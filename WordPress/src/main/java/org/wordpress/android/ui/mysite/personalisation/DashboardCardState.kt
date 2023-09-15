package org.wordpress.android.ui.mysite.personalisation

import androidx.annotation.StringRes

data class DashboardCardState(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val enabled: Boolean = false,
    val cardType: CardType
)


enum class CardType(val order: Int) {
    STATS(0),
    DRAFT_POSTS(1),
    SCHEDULED_POSTS(2),
    PAGES(3),
    ACTIVITY_LOG(4),
    BLAZE(5),
    BLOGGING_PROMPTS(6),
    NEXT_STEPS(7)
}
