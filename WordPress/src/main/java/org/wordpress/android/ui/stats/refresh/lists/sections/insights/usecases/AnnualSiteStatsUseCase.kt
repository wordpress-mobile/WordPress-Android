package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ANNUAL_SITE_STATS
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.ANNUAL_STATS
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val VISIBLE_ITEMS = 1

@Suppress("LongParameterList")
class AnnualSiteStatsUseCase(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val mostPopularStore: MostPopularInsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val selectedDateProvider: SelectedDateProvider,
    private val annualStatsMapper: AnnualStatsMapper,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val popupMenuHandler: ItemPopupMenuHandler,
    private val useCaseMode: UseCaseMode
) : StatelessUseCase<YearsInsightsModel>(ANNUAL_SITE_STATS, mainDispatcher, backgroundDispatcher) {
    override suspend fun loadCachedData(): YearsInsightsModel? {
        val dbModel = mostPopularStore.getYearsInsights(statsSiteProvider.siteModel)
        return if (dbModel?.years?.isNotEmpty() == true) dbModel else null
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<YearsInsightsModel> {
        val response = mostPopularStore.fetchYearsInsights(statsSiteProvider.siteModel, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.years.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_this_year_site_stats))

    override fun buildUiModel(domainModel: YearsInsightsModel): List<BlockListItem> {
        val periodFromProvider = selectedDateProvider.getSelectedDate(ANNUAL_STATS)
        val availablePeriods = domainModel.years
        val availableDates = availablePeriods.map { yearToDate(it.year) }
        val selectedPeriod = periodFromProvider ?: availableDates.last()
        val index = availableDates.indexOf(selectedPeriod)

        selectedDateProvider.selectDate(selectedPeriod, availableDates, ANNUAL_STATS)

        val items = mutableListOf<BlockListItem>()

        when (useCaseMode) {
            UseCaseMode.BLOCK -> {
                items.add(buildTitle())
                items.addAll(annualStatsMapper.mapYearInBlock(domainModel.years.last()))
                if (domainModel.years.size > VISIBLE_ITEMS) {
                    items.add(
                            Link(
                                    text = R.string.stats_insights_view_more,
                                    navigateAction = ListItemInteraction.create(this::onViewMoreClicked)
                            )
                    )
                }
            }
            UseCaseMode.VIEW_ALL -> {
                items.addAll(
                        annualStatsMapper.mapYearInViewAll(
                                domainModel.years.getOrNull(index) ?: domainModel.years.last()
                        )
                )
            }
            UseCaseMode.BLOCK_DETAIL -> Unit // Do nothing
        }
        return items
    }

    private fun onViewMoreClicked() {
        navigateTo(NavigationTarget.ViewAnnualStats)
    }

    private fun yearToDate(year: String): Date {
        val calendar = Calendar.getInstance(localeManagerWrapper.getLocale())
        calendar.timeInMillis = 0
        calendar.set(Calendar.YEAR, Integer.valueOf(year))
        calendar.set(Calendar.MONTH, calendar.getMaximum(Calendar.MONTH))
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getMaximum(Calendar.DAY_OF_MONTH))
        return calendar.time
    }

    private fun buildTitle() = Title(R.string.stats_insights_this_year_site_stats, menuAction = this::onMenuClick)

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }

    class AnnualSiteStatsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val mostPopularStore: MostPopularInsightsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val annualStatsMapper: AnnualStatsMapper,
        private val localeManagerWrapper: LocaleManagerWrapper,
        private val selectedDateProvider: SelectedDateProvider,
        private val popupMenuHandler: ItemPopupMenuHandler
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                AnnualSiteStatsUseCase(
                        mainDispatcher,
                        backgroundDispatcher,
                        mostPopularStore,
                        statsSiteProvider,
                        selectedDateProvider,
                        annualStatsMapper,
                        localeManagerWrapper,
                        popupMenuHandler,
                        useCaseMode
                )
    }
}
