package org.wordpress.android.ui.stats.refresh

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsViewAllFollowersViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(VIEW_ALL_FOLLOWERS_USE_CASE) useCase: BaseListUseCase
) : StatsViewAllViewModel(mainDispatcher, useCase)
