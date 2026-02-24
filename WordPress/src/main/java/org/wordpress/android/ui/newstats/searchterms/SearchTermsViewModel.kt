package org.wordpress.android.ui.newstats.searchterms

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.mostviewed.BaseStatsCardViewModel
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailItem
import org.wordpress.android.ui.newstats.mostviewed.StatsCardFetchResult
import org.wordpress.android.ui.newstats.repository.SearchTermItemData
import org.wordpress.android.ui.newstats.repository.SearchTermsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class SearchTermsViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseStatsCardViewModel(
    selectedSiteRepository, accountStore,
    statsRepository, resourceProvider
) {
    override val cardType = StatsCardType.SEARCH_TERMS
    override val logTag = "search terms"

    override suspend fun fetchStats(
        siteId: Long,
        period: StatsPeriod
    ): StatsCardFetchResult {
        return when (
            val result = statsRepository.fetchSearchTerms(
                siteId, period
            )
        ) {
            is SearchTermsResult.Success -> {
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
            is SearchTermsResult.Error -> {
                StatsCardFetchResult.Error(
                    messageResId = result.messageResId,
                    isAuthError = result.isAuthError
                )
            }
        }
    }

    private fun SearchTermItemData.toDetailItem(
        id: Long
    ) = MostViewedDetailItem(
        id = id,
        title = name,
        views = views,
        change = MostViewedChange.fromChange(
            viewsChange, viewsChangePercent
        )
    )
}
