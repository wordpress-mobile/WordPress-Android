package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ReferredItem
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import javax.inject.Inject
import javax.inject.Named

class PostHeaderUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val statsPostProvider: StatsPostProvider
) : StatelessUseCase<String>(
        PostDetailTypes.POST_HEADER,
        mainDispatcher
) {
    override suspend fun loadCachedData(): String? {
        return statsPostProvider.postTitle
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<String> {
        val title = statsPostProvider.postTitle
        return if (title != null) State.Data(title) else State.Empty()
    }

    override fun buildUiModel(domainModel: String): List<BlockListItem> {
        return listOf(ReferredItem(string.showing_stats_for, domainModel))
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf()
}
