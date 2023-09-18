package org.wordpress.android.ui.mysite.personalization

import androidx.annotation.StringRes

data class DashboardCardState(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val enabled: Boolean = false,
    val cardType: CardType
)


enum class CardType(val order: Int, val trackingName:String) {
    STATS(0, "todays_stats"),
    DRAFT_POSTS(1, "draft_posts"),
    SCHEDULED_POSTS(2,"scheduled_posts"),
    PAGES(3, "pages"),
    ACTIVITY_LOG(4, "activity_log"),
    BLAZE(5, "blaze"),
    BLOGGING_PROMPTS(6, "blogging_prompts"),
    NEXT_STEPS(7, "next_steps")
}
