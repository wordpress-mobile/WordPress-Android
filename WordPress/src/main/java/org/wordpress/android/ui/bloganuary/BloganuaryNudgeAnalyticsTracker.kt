package org.wordpress.android.ui.bloganuary

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BloganuaryNudgeAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val cardsTracker: CardsTracker,
) {
    fun trackMySiteCardLearnMoreTapped(isPromptsEnabled: Boolean) = analyticsTracker.track(
        Stat.BLOGANUARY_NUDGE_MY_SITE_CARD_LEARN_MORE_TAPPED,
        mapOf("prompts_enabled" to isPromptsEnabled.toString())
    )

    fun trackMySiteCardMoreMenuTapped() = cardsTracker.trackCardMoreMenuClicked(
        CardsTracker.Type.BLOGANUARY_NUDGE.label
    )

    fun trackMySiteCardMoreMenuItemTapped(item: MySiteCardMenuItemType) = cardsTracker.trackCardMoreMenuItemClicked(
        CardsTracker.Type.BLOGANUARY_NUDGE.label,
        item.label
    )

    enum class MySiteCardMenuItemType(val label: String) {
        HIDE_THIS("hide_this"),
    }
}
