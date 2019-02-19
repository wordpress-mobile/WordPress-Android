package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.MONTH_STATS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class MonthsListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(MONTH_STATS_USE_CASE) statsUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper
) : StatsListViewModel(mainDispatcher, statsUseCase, analyticsTracker)
