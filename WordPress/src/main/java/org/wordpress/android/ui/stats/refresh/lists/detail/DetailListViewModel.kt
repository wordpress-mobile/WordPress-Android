package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class DetailListViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(DETAIL_USE_CASE) private val detailUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsPostProvider: StatsPostProvider,
    selectedDateProvider: SelectedDateProvider
) : StatsListViewModel(mainDispatcher, detailUseCase, analyticsTracker) {
    val selectedDateChanged = selectedDateProvider.granularSelectedDateChanged(DETAIL)
    fun init(
        site: SiteModel,
        postId: Long,
        postType: String,
        postTitle: String,
        postUrl: String?
    ) {
        statsSiteProvider.start(site)
        statsPostProvider.init(postId, postType, postTitle, postUrl)
    }

    fun onDateChanged() {
        launch {
            detailUseCase.onDateChanged()
        }
    }

    fun refresh() {
        launch {
            detailUseCase.refreshData(true)
        }
    }
}
