package org.wordpress.android.ui.bloganuary

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.bloganuary.learnmore.BloganuaryNudgeLearnMoreOverlayAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BloganuaryNudgeAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val cardsTracker: CardsTracker,
) {
    fun trackMySiteCardLearnMoreTapped(isPromptsEnabled: Boolean) = analyticsTracker.track(
        Stat.BLOGANUARY_NUDGE_MY_SITE_CARD_LEARN_MORE_TAPPED,
        mapOf(PROPERTY_PROMPTS_ENABLED to isPromptsEnabled.toString())
    )

    fun trackMySiteCardMoreMenuTapped() = cardsTracker.trackCardMoreMenuClicked(
        CardsTracker.Type.BLOGANUARY_NUDGE.label
    )

    fun trackMySiteCardMoreMenuItemTapped(item: BloganuaryNudgeCardMenuItem) = cardsTracker
        .trackCardMoreMenuItemClicked(
            CardsTracker.Type.BLOGANUARY_NUDGE.label,
            item.label
        )

    fun trackLearnMoreOverlayShown(isPromptsEnabled: Boolean) = analyticsTracker.track(
        Stat.BLOGANUARY_NUDGE_LEARN_MORE_MODAL_SHOWN,
        mapOf(PROPERTY_PROMPTS_ENABLED to isPromptsEnabled.toString())
    )

    fun trackLearnMoreOverlayDismissed() = analyticsTracker.track(
        Stat.BLOGANUARY_NUDGE_LEARN_MORE_MODAL_DISMISSED
    )

    fun trackLearnMoreOverlayActionTapped(action: BloganuaryNudgeLearnMoreOverlayAction) = analyticsTracker.track(
        Stat.BLOGANUARY_NUDGE_LEARN_MORE_MODAL_ACTION_TAPPED,
        mapOf(PROPERTY_ACTION to action.analyticsLabel)
    )

    enum class BloganuaryNudgeCardMenuItem(val label: String) {
        HIDE_THIS("hide_this"),
    }

    companion object {
        private const val PROPERTY_PROMPTS_ENABLED = "prompts_enabled"
        private const val PROPERTY_ACTION = "action"
    }
}
