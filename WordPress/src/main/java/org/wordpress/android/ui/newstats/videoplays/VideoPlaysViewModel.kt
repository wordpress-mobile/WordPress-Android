package org.wordpress.android.ui.newstats.videoplays

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.mostviewed.BaseStatsCardViewModel
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailItem
import org.wordpress.android.ui.newstats.mostviewed.StatsCardFetchResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.VideoPlayItemData
import org.wordpress.android.ui.newstats.repository.VideoPlaysResult
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class VideoPlaysViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseStatsCardViewModel(
    selectedSiteRepository, accountStore,
    statsRepository, resourceProvider
) {
    override val cardType = StatsCardType.VIDEO_PLAYS
    override val logTag = "video plays"

    override suspend fun fetchStats(
        siteId: Long,
        period: StatsPeriod
    ): StatsCardFetchResult {
        return when (
            val result = statsRepository.fetchVideoPlays(
                siteId, period
            )
        ) {
            is VideoPlaysResult.Success -> {
                StatsCardFetchResult.Success(
                    items = result.items.mapIndexed { i, item ->
                        item.toDetailItem(i.toLong())
                    },
                    totalValue = result.totalViews,
                    totalValueChange = result.totalViewsChange,
                    totalValueChangePercent =
                        result.totalViewsChangePercent
                )
            }
            is VideoPlaysResult.Error -> {
                StatsCardFetchResult.Error(
                    messageResId = result.messageResId,
                    isAuthError = result.isAuthError
                )
            }
        }
    }

    private fun VideoPlayItemData.toDetailItem(
        id: Long
    ) = MostViewedDetailItem(
        id = id,
        title = title,
        views = views,
        change = MostViewedChange.fromChange(
            viewsChange, viewsChangePercent
        )
    )
}
