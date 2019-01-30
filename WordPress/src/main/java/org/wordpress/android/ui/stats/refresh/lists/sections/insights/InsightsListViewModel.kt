package org.wordpress.android.ui.stats.refresh.lists.sections.insights

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.INSIGHTS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.UiModelMapper
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

class InsightsListViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(INSIGHTS_USE_CASE) private val insightsUseCase: BaseListUseCase,
    networkUtilsWrapper: NetworkUtilsWrapper,
    uiModelMapper: UiModelMapper
) : StatsListViewModel(mainDispatcher, insightsUseCase, networkUtilsWrapper, uiModelMapper::mapInsights)
