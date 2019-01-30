package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.DAY_STATS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.UiModelMapper
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

class DaysListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(DAY_STATS_USE_CASE) statsUseCase: BaseListUseCase,
    networkUtilsWrapper: NetworkUtilsWrapper,
    uiModelMapper: UiModelMapper
) : StatsListViewModel(mainDispatcher, statsUseCase, networkUtilsWrapper, uiModelMapper::mapTimeStats)
