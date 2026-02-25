package org.wordpress.android.ui.newstats.filedownloads

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.mostviewed.BaseStatsCardViewModel
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailItem
import org.wordpress.android.ui.newstats.mostviewed.StatsCardFetchResult
import org.wordpress.android.ui.newstats.repository.FileDownloadItemData
import org.wordpress.android.ui.newstats.repository.FileDownloadsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class FileDownloadsViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseStatsCardViewModel(
    selectedSiteRepository, accountStore,
    statsRepository, resourceProvider
) {
    override val cardType = StatsCardType.FILE_DOWNLOADS
    override val logTag = "file downloads"

    override suspend fun fetchStats(
        siteId: Long,
        period: StatsPeriod
    ): StatsCardFetchResult {
        return when (
            val result = statsRepository.fetchFileDownloads(
                siteId, period
            )
        ) {
            is FileDownloadsResult.Success -> {
                StatsCardFetchResult.Success(
                    items = result.items.mapIndexed { i, item ->
                        item.toDetailItem(i.toLong())
                    },
                    totalValue = result.totalDownloads,
                    totalValueChange =
                        result.totalDownloadsChange,
                    totalValueChangePercent =
                        result.totalDownloadsChangePercent
                )
            }
            is FileDownloadsResult.Error -> {
                StatsCardFetchResult.Error(
                    messageResId = result.messageResId,
                    isAuthError = result.isAuthError
                )
            }
        }
    }

    private fun FileDownloadItemData.toDetailItem(
        id: Long
    ) = MostViewedDetailItem(
        id = id,
        title = name,
        views = downloads,
        change = MostViewedChange.fromChange(
            downloadsChange, downloadsChangePercent
        )
    )
}
