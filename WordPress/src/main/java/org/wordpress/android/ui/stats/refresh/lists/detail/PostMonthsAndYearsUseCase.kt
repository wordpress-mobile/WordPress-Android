package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Year
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes
import org.wordpress.android.fluxc.store.stats.PostDetailStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewMonthsAndYearsStats
import org.wordpress.android.ui.stats.refresh.lists.detail.PostMonthsAndYearsUseCase.UiState
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.LocaleManagerWrapper
import java.text.DateFormatSymbols
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6
private const val VIEW_ALL_ITEM_COUNT = 1000

class PostMonthsAndYearsUseCase(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsPostProvider: StatsPostProvider,
    private val postDetailStore: PostDetailStore,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val useCaseMode: UseCaseMode
) : BaseStatsUseCase<PostDetailStatsModel, UiState>(
        PostDetailTypes.MONTHS_AND_YEARS,
        mainDispatcher,
        UiState()
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override suspend fun loadCachedData(): PostDetailStatsModel? {
        return statsPostProvider.postId?.let { postId ->
            postDetailStore.getPostDetail(
                    statsSiteProvider.siteModel,
                    postId
            )
        }
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<PostDetailStatsModel> {
        val response = statsPostProvider.postId?.let { postId ->
            postDetailStore.fetchPostDetail(statsSiteProvider.siteModel, postId, forced)
        }
        val model = response?.model
        val error = response?.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.yearsTotal.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: PostDetailStatsModel, uiState: UiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (useCaseMode == BLOCK) {
            items.add(Title(string.stats_detail_months_and_years))
            items.add(Divider)
        }
        items.add(
                Header(
                        string.stats_months_and_years_period_label,
                        string.stats_months_and_years_views_label
                )
        )
        val yearList = mutableListOf<BlockListItem>()
        val shownYears = domainModel.yearsTotal.sortedByDescending { it.year }.takeLast(itemsToLoad)
        shownYears.forEachIndexed { index, year ->
            if (year.months.isNotEmpty()) {
                val isExpanded = year.year == uiState.expandedYear
                yearList.add(
                        ExpandableItem(
                                mapYear(year, index, shownYears), isExpanded = isExpanded
                        ) { changedExpandedState ->
                            onUiState(uiState.copy(expandedYear = if (changedExpandedState) year.year else null))
                        })
                if (isExpanded) {
                    yearList.addAll(year.months.sortedByDescending { it.month }.map { month ->
                        ListItemWithIcon(
                                text = DateFormatSymbols(localeManagerWrapper.getLocale()).shortMonths[month.month - 1],
                                value = month.count.toFormattedString(locale = localeManagerWrapper.getLocale()),
                                textStyle = LIGHT,
                                showDivider = false
                        )
                    })
                    yearList.add(Divider)
                }
            } else {
                yearList.add(
                        mapYear(year, index, shownYears)
                )
            }
        }

        items.addAll(yearList)
        if (useCaseMode == BLOCK && domainModel.yearsTotal.size > itemsToLoad) {
            items.add(
                    Link(
                            text = string.stats_insights_view_more,
                            navigateAction = NavigationAction.create(this::onLinkClick)
                    )
            )
        }
        return items
    }

    private fun mapYear(
        year: Year,
        index: Int,
        shownYears: List<Year>
    ): ListItemWithIcon {
        return ListItemWithIcon(
                text = year.year.toString(),
                value = year.value.toFormattedString(locale = localeManagerWrapper.getLocale()),
                showDivider = index < shownYears.size - 1
        )
    }

    private fun onLinkClick() {
        navigateTo(ViewMonthsAndYearsStats())
    }

    override fun buildLoadingItem(): List<BlockListItem> {
        return listOf(Title(string.stats_detail_months_and_years))
    }

    data class UiState(val expandedYear: Int? = null)

    class PostMonthsAndYearsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val statsSiteProvider: StatsSiteProvider,
        private val statsPostProvider: StatsPostProvider,
        private val localeManagerWrapper: LocaleManagerWrapper,
        private val postDetailStore: PostDetailStore
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                PostMonthsAndYearsUseCase(
                        mainDispatcher,
                        statsSiteProvider,
                        statsPostProvider,
                        postDetailStore,
                        localeManagerWrapper,
                        useCaseMode
                )
    }
}
