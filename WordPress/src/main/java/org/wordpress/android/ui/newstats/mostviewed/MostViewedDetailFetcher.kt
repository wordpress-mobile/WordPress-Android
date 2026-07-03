package org.wordpress.android.ui.newstats.mostviewed

import org.wordpress.android.R
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.ClickItemData
import org.wordpress.android.ui.newstats.repository.ClicksResult
import org.wordpress.android.ui.newstats.repository.FileDownloadItemData
import org.wordpress.android.ui.newstats.repository.FileDownloadsResult
import org.wordpress.android.ui.newstats.repository.MostViewedResult
import org.wordpress.android.ui.newstats.repository.SearchTermItemData
import org.wordpress.android.ui.newstats.repository.SearchTermsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.VideoPlayItemData
import org.wordpress.android.ui.newstats.repository.VideoPlaysResult
import javax.inject.Inject

/**
 * Which data source a self-fetching Most Viewed detail screen should load. These are the sources
 * whose detail screen shows the full (unbounded, max = 0) list, so they re-fetch it themselves
 * rather than receiving it through the launching Intent (which risked TransactionTooLargeException).
 */
enum class MostViewedDetailSource {
    REFERRERS,
    CLICKS,
    SEARCH_TERMS,
    VIDEO_PLAYS,
    FILE_DOWNLOADS
}

/**
 * Fetches the full detail list for a [MostViewedDetailSource] and maps it to the common
 * [StatsCardFetchResult] shape used by [MostViewedDetailViewModel].
 *
 * The per-item mapping mirrors the corresponding card ViewModels (e.g. `ClicksViewModel`); the card
 * shows the first N items while the detail screen shows them all.
 */
class MostViewedDetailFetcher @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend fun fetch(
        source: MostViewedDetailSource,
        siteId: Long,
        period: StatsPeriod,
        accessToken: String
    ): StatsCardFetchResult {
        statsRepository.init(accessToken)
        return when (source) {
            MostViewedDetailSource.REFERRERS -> statsRepository.fetchReferrersDetail(siteId, period).toFetchResult()
            MostViewedDetailSource.CLICKS -> statsRepository.fetchClicks(siteId, period).toFetchResult()
            MostViewedDetailSource.SEARCH_TERMS -> statsRepository.fetchSearchTerms(siteId, period).toFetchResult()
            MostViewedDetailSource.VIDEO_PLAYS -> statsRepository.fetchVideoPlays(siteId, period).toFetchResult()
            MostViewedDetailSource.FILE_DOWNLOADS -> statsRepository.fetchFileDownloads(siteId, period).toFetchResult()
        }
    }

    private fun MostViewedResult.toFetchResult() = when (this) {
        is MostViewedResult.Success -> StatsCardFetchResult.Success(
            items = items.map { it.toDetailItem() },
            totalValue = totalViews,
            totalValueChange = totalViewsChange,
            totalValueChangePercent = totalViewsChangePercent
        )
        is MostViewedResult.Error -> StatsCardFetchResult.Error(R.string.stats_error_api)
    }

    private fun ClicksResult.toFetchResult() = when (this) {
        is ClicksResult.Success -> StatsCardFetchResult.Success(
            items = items.mapIndexed { i, item -> item.toDetailItem(i.toLong()) },
            totalValue = totalClicks,
            totalValueChange = totalClicksChange,
            totalValueChangePercent = totalClicksChangePercent
        )
        is ClicksResult.Error -> StatsCardFetchResult.Error(messageResId, isAuthError)
    }

    private fun SearchTermsResult.toFetchResult() = when (this) {
        is SearchTermsResult.Success -> StatsCardFetchResult.Success(
            items = items.mapIndexed { i, item -> item.toDetailItem(i.toLong()) },
            totalValue = totalViews,
            totalValueChange = totalViewsChange,
            totalValueChangePercent = totalViewsChangePercent
        )
        is SearchTermsResult.Error -> StatsCardFetchResult.Error(messageResId, isAuthError)
    }

    private fun VideoPlaysResult.toFetchResult() = when (this) {
        is VideoPlaysResult.Success -> StatsCardFetchResult.Success(
            items = items.mapIndexed { i, item -> item.toDetailItem(i.toLong()) },
            totalValue = totalViews,
            totalValueChange = totalViewsChange,
            totalValueChangePercent = totalViewsChangePercent
        )
        is VideoPlaysResult.Error -> StatsCardFetchResult.Error(messageResId, isAuthError)
    }

    private fun FileDownloadsResult.toFetchResult() = when (this) {
        is FileDownloadsResult.Success -> StatsCardFetchResult.Success(
            items = items.mapIndexed { i, item -> item.toDetailItem(i.toLong()) },
            totalValue = totalDownloads,
            totalValueChange = totalDownloadsChange,
            totalValueChangePercent = totalDownloadsChangePercent
        )
        is FileDownloadsResult.Error -> StatsCardFetchResult.Error(messageResId, isAuthError)
    }

    private fun ClickItemData.toDetailItem(id: Long) = MostViewedDetailItem(
        id = id,
        title = name,
        views = clicks,
        change = MostViewedChange.fromChange(clicksChange, clicksChangePercent)
    )

    private fun SearchTermItemData.toDetailItem(id: Long) = MostViewedDetailItem(
        id = id,
        title = name,
        views = views,
        change = MostViewedChange.fromChange(viewsChange, viewsChangePercent)
    )

    private fun VideoPlayItemData.toDetailItem(id: Long) = MostViewedDetailItem(
        id = id,
        title = title,
        views = views,
        change = MostViewedChange.fromChange(viewsChange, viewsChangePercent)
    )

    private fun FileDownloadItemData.toDetailItem(id: Long) = MostViewedDetailItem(
        id = id,
        title = name,
        views = downloads,
        change = MostViewedChange.fromChange(downloadsChange, downloadsChangePercent)
    )
}
