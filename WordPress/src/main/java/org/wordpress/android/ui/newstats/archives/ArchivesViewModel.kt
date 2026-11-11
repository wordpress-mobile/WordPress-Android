package org.wordpress.android.ui.newstats.archives

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.mostviewed.BaseStatsCardViewModel
import org.wordpress.android.ui.newstats.mostviewed.StatsCardFetchResult
import org.wordpress.android.ui.newstats.repository.ArchivesResult
import org.wordpress.android.ui.newstats.repository.StatsCacheBucket
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class ArchivesViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseStatsCardViewModel(
    selectedSiteRepository, accountStore,
    statsRepository, resourceProvider
) {
    override val logTag = "archives"
    override val cacheBucket = StatsCacheBucket.ARCHIVES

    override suspend fun fetchStats(
        siteId: Long,
        period: StatsPeriod,
        forceRefresh: Boolean
    ): StatsCardFetchResult {
        return when (
            val result = statsRepository.fetchArchives(
                siteId, period, forceRefresh
            )
        ) {
            is ArchivesResult.Success -> {
                StatsCardFetchResult.Success(
                    items = result.sections.mapIndexed { i, section ->
                        section.toDetailItem(
                            i.toLong(), resourceProvider, CARD_MAX_ENTRIES_PER_SECTION
                        )
                    },
                    totalValue = result.totalViews,
                    totalValueChange = result.totalViewsChange,
                    totalValueChangePercent =
                        result.totalViewsChangePercent
                )
            }
            is ArchivesResult.Error -> {
                StatsCardFetchResult.Error(
                    messageResId = result.messageResId,
                    isAuthError = result.isAuthError,
                    isNotAvailable = result.isNotAvailable
                )
            }
        }
    }

    companion object {
        // How many archive pages are listed under an expanded group on the card, and -- in the
        // "Taxonomies" group, which is a level deeper -- how many taxonomies and how many terms
        // under each. The "show all" screen lists every entry the group was fetched with.
        private const val CARD_MAX_ENTRIES_PER_SECTION = 10
    }
}
