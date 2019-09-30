package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.StatsStore.ManagementType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewInsightsManagement
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LinkButton
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.utils.NewsCardHandler
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class ManagementControlUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val newsCardHandler: NewsCardHandler,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : StatelessUseCase<Boolean>(ManagementType.CONTROL, mainDispatcher, backgroundDispatcher, listOf()) {
    override suspend fun loadCachedData() = true

    override suspend fun fetchRemoteData(forced: Boolean): State<Boolean> = State.Data(true)

    override fun buildLoadingItem(): List<BlockListItem> = listOf()

    override fun buildUiModel(domainModel: Boolean): List<BlockListItem> {
        return listOf(LinkButton(R.string.stats_management_edit, NavigationAction.create(this::onClick)))
    }

    private fun onClick() {
        newsCardHandler.dismiss()
        analyticsTrackerWrapper.track(Stat.STATS_INSIGHTS_MANAGEMENT_ACCESSED)
        navigateTo(ViewInsightsManagement)
    }
}
