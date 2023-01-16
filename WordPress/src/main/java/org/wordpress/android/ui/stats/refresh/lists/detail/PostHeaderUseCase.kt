package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsConstants.ITEM_TYPE_ATTACHMENT
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewAttachment
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ReferredItem
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.utils.ListItemInteraction.Companion
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class PostHeaderUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val statsPostProvider: StatsPostProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : StatelessUseCase<String>(
    PostDetailType.POST_HEADER,
    mainDispatcher,
    backgroundDispatcher,
    listOf()
) {
    override suspend fun loadCachedData(): String? {
        return statsPostProvider.postTitle
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<String> {
        val title = statsPostProvider.postTitle
        return if (title != null) State.Data(title) else State.Empty()
    }

    override fun buildUiModel(domainModel: String): List<BlockListItem> {
        val postId = statsPostProvider.postId
        val postUrl = statsPostProvider.postUrl
        val itemType = statsPostProvider.postType
        val navigationAction = if (postId != null && postUrl != null && itemType != null) {
            val clickParams = ClickParams(postId, postUrl, itemType)
            Companion.create(clickParams, this::click)
        } else {
            null
        }
        return listOf(ReferredItem(R.string.showing_stats_for, domainModel, navigationAction))
    }

    private fun click(clickParams: ClickParams) {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_DETAIL_POST_TAPPED)
        if (clickParams.itemType == ITEM_TYPE_ATTACHMENT) {
            navigateTo(ViewAttachment(clickParams.postId, clickParams.postUrl))
        } else {
            navigateTo(ViewPost(clickParams.postId, clickParams.postUrl))
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf()

    data class ClickParams(val postId: Long, val postUrl: String, val itemType: String)
}
