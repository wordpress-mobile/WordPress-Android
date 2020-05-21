package org.wordpress.android.ui.stats.refresh.lists.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType
import org.wordpress.android.fluxc.store.stats.PostDetailStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewDayAverageStats
import org.wordpress.android.ui.stats.refresh.lists.detail.PostDetailMapper.ExpandedYearUiState
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6
private const val VIEW_ALL_ITEM_COUNT = 1000

class PostAverageViewsPerDayUseCase(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsPostProvider: StatsPostProvider,
    private val postDetailStore: PostDetailStore,
    private val postDetailMapper: PostDetailMapper,
    private val useCaseMode: UseCaseMode
) : BaseStatsUseCase<PostDetailStatsModel, ExpandedYearUiState>(
        PostDetailType.AVERAGE_VIEWS_PER_DAY,
        mainDispatcher,
        backgroundDispatcher,
        ExpandedYearUiState()
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
            model != null && model.hasData() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: PostDetailStatsModel, uiState: ExpandedYearUiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_detail_average_views_per_day))
        }
        val header = Header(
                R.string.stats_months_and_years_period_label,
                R.string.stats_months_and_years_views_label
        )
        items.add(
                header
        )
        val shownYears = domainModel.yearsAverage.sortedByDescending { it.year }.takeLast(itemsToLoad)
        val yearList = postDetailMapper.mapYears(
                shownYears,
                uiState,
                header,
                this::onUiState
        )

        items.addAll(yearList)
        if (useCaseMode == BLOCK && domainModel.yearsAverage.size > itemsToLoad) {
            items.add(
                    Link(
                            text = R.string.stats_insights_view_more,
                            navigateAction = NavigationAction.create(this::onLinkClick)
                    )
            )
        }
        return items
    }

    private fun PostDetailStatsModel?.hasData(): Boolean {
        return this != null && this.yearsAverage.isNotEmpty() && this.yearsAverage.any { it.value > 0 }
    }

    private fun onLinkClick() {
        navigateTo(ViewDayAverageStats)
    }

    override fun buildLoadingItem(): List<BlockListItem> {
        return listOf(Title(R.string.stats_detail_average_views_per_day))
    }

    class PostAverageViewsPerDayUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val statsSiteProvider: StatsSiteProvider,
        private val statsPostProvider: StatsPostProvider,
        private val postDetailMapper: PostDetailMapper,
        private val postDetailStore: PostDetailStore
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                PostAverageViewsPerDayUseCase(
                        mainDispatcher,
                        backgroundDispatcher,
                        statsSiteProvider,
                        statsPostProvider,
                        postDetailStore,
                        postDetailMapper,
                        useCaseMode
                )
    }
}
