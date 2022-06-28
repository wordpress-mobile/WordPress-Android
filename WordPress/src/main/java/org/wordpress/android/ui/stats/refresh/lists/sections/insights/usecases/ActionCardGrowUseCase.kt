package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.CheckCourse
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemActionCard
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class ActionCardGrowUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val actionCardHandler: ActionCardHandler,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : StatelessUseCase<Boolean>(InsightType.ACTION_GROW, mainDispatcher, backgroundDispatcher, listOf()) {
    override suspend fun loadCachedData() = true

    override suspend fun fetchRemoteData(forced: Boolean): State<Boolean> = State.Data(true)

    override fun buildLoadingItem(): List<BlockListItem> = listOf()

    override fun buildUiModel(domainModel: Boolean): List<BlockListItem> {
        return listOf(
                ListItemActionCard(
                        titleResource = string.stats_action_card_grow_audience_title,
                        text = string.stats_action_card_grow_audience_message,
                        positiveButtonText = string.stats_action_card_grow_audience_button_label,
                        positiveAction = ListItemInteraction.create(this::onCheckCourse),
                        negativeButtonText = string.stats_management_dismiss_insights_news_card,
                        negativeAction = ListItemInteraction.create(this::onDismiss)
                )
        )
    }

    private fun onCheckCourse() {
        analyticsTrackerWrapper.track(Stat.STATS_INSIGHTS_ACTION_GROW_AUDIENCE_CONFIRMED)
        navigateTo(CheckCourse)
        actionCardHandler.dismiss(InsightType.ACTION_GROW)
    }

    private fun onDismiss() {
        analyticsTrackerWrapper.track(Stat.STATS_INSIGHTS_ACTION_GROW_AUDIENCE_DISMISSED)
        actionCardHandler.dismiss(InsightType.ACTION_GROW)
    }
}
