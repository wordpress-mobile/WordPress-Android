package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class DetailListViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(DETAIL_USE_CASE) private val insightsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsPostProvider: StatsPostProvider
) : StatsListViewModel(mainDispatcher, insightsUseCase, analyticsTracker) {
    fun init(
        site: SiteModel,
        postId: String,
        postType: String,
        postTitle: String,
        postUrl: String?
    ) {
        statsSiteProvider.start(site)
        statsPostProvider.init(postId, postType, postTitle, postUrl)
    }
}
