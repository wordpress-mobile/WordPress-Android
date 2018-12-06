package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.DAY_STATS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import javax.inject.Inject
import javax.inject.Named

class DaysListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(DAY_STATS_USE_CASE) statsUseCase: BaseListUseCase
) : StatsListViewModel(mainDispatcher, statsUseCase)
