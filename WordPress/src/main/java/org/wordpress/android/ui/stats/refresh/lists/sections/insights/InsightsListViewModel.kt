package org.wordpress.android.ui.stats.refresh.lists.sections.insights

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.INSIGHTS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import javax.inject.Inject
import javax.inject.Named

class InsightsListViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(INSIGHTS_USE_CASE) private val insightsUseCase: BaseListUseCase
) : StatsListViewModel(mainDispatcher, insightsUseCase)
