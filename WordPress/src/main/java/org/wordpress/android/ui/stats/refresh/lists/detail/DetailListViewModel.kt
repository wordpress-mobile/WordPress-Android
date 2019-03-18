package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.BLOCK_DETAIL_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.utils.StatsDateSelector
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class DetailListViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BLOCK_DETAIL_USE_CASE) private val detailUseCase: BaseListUseCase,
    analyticsTracker: AnalyticsTrackerWrapper,
    dateSelectorFactory: StatsDateSelector.Factory
) : StatsListViewModel(mainDispatcher, detailUseCase, analyticsTracker, dateSelectorFactory.build(DETAIL))
