package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import java.util.Date

abstract class GranularStatelessUseCase<DOMAIN_MODEL>(
    type: StatsTypes,
    mainDispatcher: CoroutineDispatcher,
    val selectedDateProvider: SelectedDateProvider,
    val statsSiteProvider: StatsSiteProvider,
    val statsGranularity: StatsGranularity
) : StatelessUseCase<DOMAIN_MODEL>(type, mainDispatcher) {
    abstract suspend fun loadCachedData(selectedDate: Date, site: SiteModel): DOMAIN_MODEL?

    final override suspend fun loadCachedData(): DOMAIN_MODEL? {
        val selectedDate = selectedDateProvider.getSelectedDate(statsGranularity)
        return selectedDate?.let { loadCachedData(selectedDate, statsSiteProvider.siteModel) }
    }

    abstract suspend fun fetchRemoteData(selectedDate: Date, site: SiteModel, forced: Boolean): State<DOMAIN_MODEL>

    final override suspend fun fetchRemoteData(forced: Boolean): State<DOMAIN_MODEL> {
        return selectedDateProvider.getSelectedDateState(statsGranularity).let { date ->
            when {
                date.error -> State.Error("Missing date")
                date.hasData() -> fetchRemoteData(date.getDate(), statsSiteProvider.siteModel, forced)
                date.loading -> State.Loading()
                else -> State.Loading()
            }
        }
    }

    override fun buildEmptyItem(): List<BlockListItem> {
        return buildLoadingItem() + listOf(BlockListItem.Empty(textResource = R.string.stats_no_data_for_period))
    }
}
