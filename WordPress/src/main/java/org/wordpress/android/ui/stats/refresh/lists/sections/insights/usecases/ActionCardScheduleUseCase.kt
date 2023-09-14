package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SchedulePost
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemActionCard
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class ActionCardScheduleUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val actionCardHandler: ActionCardHandler,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : StatelessUseCase<Boolean>(InsightType.ACTION_SCHEDULE, mainDispatcher, backgroundDispatcher, listOf()) {
    override suspend fun loadCachedData() = true

    override suspend fun fetchRemoteData(forced: Boolean): State<Boolean> = State.Data(true)

    override fun buildLoadingItem(): List<BlockListItem> = listOf()

    override fun buildUiModel(domainModel: Boolean): List<BlockListItem> {
        return listOf(
            ListItemActionCard(
                titleResource = R.string.stats_action_card_schedule_post_title,
                text = R.string.stats_action_card_schedule_post_message,
                positiveButtonText = R.string.stats_action_card_schedule_post_button_label,
                positiveAction = ListItemInteraction.create(this::onSchedule),
                negativeButtonText = R.string.stats_management_dismiss_insights_news_card,
                negativeAction = ListItemInteraction.create(this::onDismiss)
            )
        )
    }

    private fun onSchedule() {
        analyticsTrackerWrapper.track(Stat.STATS_INSIGHTS_ACTION_SCHEDULE_POST_CONFIRMED)
        navigateTo(SchedulePost)
        actionCardHandler.dismiss(InsightType.ACTION_SCHEDULE)
    }

    private fun onDismiss() {
        analyticsTrackerWrapper.track(Stat.STATS_INSIGHTS_ACTION_SCHEDULE_POST_DISMISSED)
        actionCardHandler.dismiss(InsightType.ACTION_SCHEDULE)
    }
}
