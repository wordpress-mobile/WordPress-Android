package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SetBloggingReminders
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemActionCard
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class ActionCardReminderUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val actionCardHandler: ActionCardHandler,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : StatelessUseCase<Boolean>(InsightType.ACTION_REMINDER, mainDispatcher, backgroundDispatcher, listOf()) {
    override suspend fun loadCachedData() = true

    override suspend fun fetchRemoteData(forced: Boolean): State<Boolean> = State.Data(true)

    override fun buildLoadingItem(): List<BlockListItem> = listOf()

    override fun buildUiModel(domainModel: Boolean): List<BlockListItem> {
        return listOf(
            ListItemActionCard(
                titleResource = string.stats_action_card_blogging_reminders_title,
                text = string.stats_action_card_blogging_reminders_message,
                positiveButtonText = string.stats_action_card_blogging_reminders_button_label,
                positiveAction = ListItemInteraction.create(this::onSetReminders),
                negativeButtonText = string.stats_management_dismiss_insights_news_card,
                negativeAction = ListItemInteraction.create(this::onDismiss)
            )
        )
    }

    private fun onSetReminders() {
        analyticsTrackerWrapper.track(Stat.STATS_INSIGHTS_ACTION_BLOGGING_REMINDERS_CONFIRMED)
        navigateTo(SetBloggingReminders)
        actionCardHandler.dismiss(InsightType.ACTION_REMINDER)
    }

    private fun onDismiss() {
        analyticsTrackerWrapper.track(Stat.STATS_INSIGHTS_ACTION_BLOGGING_REMINDERS_DISMISSED)
        actionCardHandler.dismiss(InsightType.ACTION_REMINDER)
    }
}
