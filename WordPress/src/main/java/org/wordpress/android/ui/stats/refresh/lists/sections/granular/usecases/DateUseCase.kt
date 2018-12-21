package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.DATE
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BackgroundInformation
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class DateUseCase
constructor(
    statsGranularity: StatsGranularity,
    selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val resourceProvider: ResourceProvider,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : GranularStatelessUseCase<Date>(DATE, mainDispatcher, selectedDateProvider, statsGranularity) {
    override fun buildLoadingItem(): List<BlockListItem> =
            listOf(
                    BackgroundInformation(
                            text = statsDateFormatter.printGranularDate(
                                    selectedDateProvider.getCurrentDate(),
                                    statsGranularity
                            )
                    )
            )

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel) {
        onModel(selectedDate)
    }

    override suspend fun fetchRemoteData(selectedDate: Date, site: SiteModel, forced: Boolean) {
        onModel(selectedDate)
    }

    override fun buildUiModel(domainModel: Date): List<BlockListItem> {
        return listOf(getUiModel(domainModel))
    }

    private fun getUiModel(domainModel: Date): BackgroundInformation {
        return BackgroundInformation(
                resourceProvider.getString(
                        string.stats_for,
                        statsDateFormatter.printGranularDate(
                                domainModel,
                                statsGranularity
                        )
                )
        )
    }

    class DateUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val selectedDateProvider: SelectedDateProvider,
        private val resourceProvider: ResourceProvider,
        private val statsDateFormatter: StatsDateFormatter
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                DateUseCase(
                        granularity,
                        selectedDateProvider,
                        statsDateFormatter,
                        resourceProvider,
                        mainDispatcher
                )
    }
}
