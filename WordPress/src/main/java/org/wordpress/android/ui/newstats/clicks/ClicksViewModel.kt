package org.wordpress.android.ui.newstats.clicks

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.mostviewed.BaseStatsCardViewModel
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailItem
import org.wordpress.android.ui.newstats.mostviewed.StatsCardFetchResult
import org.wordpress.android.ui.newstats.repository.ClickItemData
import org.wordpress.android.ui.newstats.repository.ClicksResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class ClicksViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseStatsCardViewModel(
    selectedSiteRepository, accountStore,
    statsRepository, resourceProvider
) {
    override val cardType = StatsCardType.CLICKS
    override val logTag = "clicks"

    override suspend fun fetchStats(
        siteId: Long,
        period: StatsPeriod
    ): StatsCardFetchResult {
        return when (
            val result = statsRepository.fetchClicks(
                siteId, period
            )
        ) {
            is ClicksResult.Success -> {
                StatsCardFetchResult.Success(
                    items = result.items.mapIndexed { i, item ->
                        item.toDetailItem(i.toLong())
                    },
                    totalValue = result.totalClicks,
                    totalValueChange = result.totalClicksChange,
                    totalValueChangePercent =
                        result.totalClicksChangePercent
                )
            }
            is ClicksResult.Error -> {
                StatsCardFetchResult.Error(
                    messageResId = result.messageResId,
                    isAuthError = result.isAuthError
                )
            }
        }
    }

    private fun ClickItemData.toDetailItem(
        id: Long
    ) = MostViewedDetailItem(
        id = id,
        title = name,
        views = clicks,
        change = MostViewedChange.fromChange(
            clicksChange, clicksChangePercent
        )
    )
}
