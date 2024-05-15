package org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.PagedMode
import org.wordpress.android.fluxc.store.StatsStore.SubscriberType.SUBSCRIBERS
import org.wordpress.android.fluxc.store.stats.insights.FollowersStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LoadingItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases.SubscribersUseCase.SubscribersUiState
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class SubscribersUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val followersStore: FollowersStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsSinceLabelFormatter: StatsSinceLabelFormatter,
    private val statsUtils: StatsUtils,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val useCaseMode: UseCaseMode,
    private val contentDescriptionHelper: ContentDescriptionHelper
) : BaseStatsUseCase<FollowersModel, SubscribersUiState>(
    SUBSCRIBERS,
    mainDispatcher,
    bgDispatcher,
    SubscribersUiState(isLoading = true)
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_PAGE_SIZE else BLOCK_ITEM_COUNT

    override suspend fun loadCachedData(): FollowersModel? {
        val cacheMode = if (useCaseMode == VIEW_ALL) LimitMode.All else LimitMode.Top(itemsToLoad)
        return followersStore.getFollowers(statsSiteProvider.siteModel, cacheMode)
    }

    override suspend fun fetchRemoteData(forced: Boolean) = fetchData(forced, PagedMode(itemsToLoad, false))

    private suspend fun fetchData(forced: Boolean, fetchMode: PagedMode): State<FollowersModel> {
        withContext(mainDispatcher) { updateUiState { it.copy(isLoading = true) } }
        val response = followersStore.fetchFollowers(statsSiteProvider.siteModel, fetchMode, forced)

        val model = response.model
        val error = response.error
        withContext(mainDispatcher) { updateUiState { it.copy(isLoading = false) } }
        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.followers.isNotEmpty() -> State.Data(model, cached = response.cached)
            else -> State.Empty()
        }
    }

    override fun buildLoadingItem() = listOf(Title(R.string.stats_view_subscribers))

    override fun buildEmptyItem() = listOf(buildTitle(), Empty())

    override fun buildUiModel(domainModel: FollowersModel, uiState: SubscribersUiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == VIEW_ALL) {
            items.add(Title(R.string.stats_view_total_subscribers))
            items.add(
                BlockListItem.ValueWithChartItem(
                    value = statsUtils.toFormattedString(domainModel.totalCount, MILLION),
                    extraBottomMargin = true
                )
            )
        }

        if (useCaseMode == UseCaseMode.BLOCK) {
            items.add(buildTitle())
        }

        if (domainModel.followers.isEmpty()) {
            items.add(Empty())
        } else {
            val header = Header(R.string.stats_name_label, R.string.stats_subscriber_since_label)
            items.add(header)
            val followers = if (useCaseMode == VIEW_ALL) {
                domainModel.followers
            } else {
                domainModel.followers.take(itemsToLoad)
            }

            followers.toUserItems(header).let { items.addAll(it) }

            if (domainModel.hasMore || domainModel.followers.size < domainModel.totalCount) {
                if (useCaseMode != VIEW_ALL) {
                    val buttonText = R.string.stats_insights_view_more
                    items.add(
                        BlockListItem.Link(
                            text = buttonText,
                            navigateAction = ListItemInteraction.create(this::onLinkClick)
                        )
                    )
                } else if (domainModel.followers.size >= VIEW_ALL_PAGE_SIZE) {
                    items.add(LoadingItem(this::loadMore, isLoading = uiState.isLoading))
                }
            }
        }
        return items
    }

    private fun buildTitle() = Title(R.string.stats_view_subscribers)

    private fun loadMore() = launch {
        val state = fetchData(true, PagedMode(itemsToLoad, true))
        evaluateState(state)
    }

    private fun List<FollowerModel>.toUserItems(header: Header): List<BlockListItem.ListItemWithIcon> {
        return this.map { follower ->
            val value = statsSinceLabelFormatter.getSinceLabelLowerCase(follower.dateSubscribed)
            BlockListItem.ListItemWithIcon(
                iconUrl = follower.avatar,
                iconStyle = BlockListItem.ListItemWithIcon.IconStyle.AVATAR,
                text = follower.label,
                value = value,
                showDivider = false,
                contentDescription = contentDescriptionHelper.buildContentDescription(header, follower.label, value)
            )
        }
    }

    private fun onLinkClick() {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_SUBSCRIBERS_VIEW_MORE_TAPPED)
        navigateTo(NavigationTarget.SubscribersStats)
    }

    data class SubscribersUiState(val isLoading: Boolean = false)

    class SubscribersUseCaseFactory @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val followersStore: FollowersStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val statsSinceLabelFormatter: StatsSinceLabelFormatter,
        private val statsUtils: StatsUtils,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val contentDescriptionHelper: ContentDescriptionHelper
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) = SubscribersUseCase(
            mainDispatcher,
            backgroundDispatcher,
            followersStore,
            statsSiteProvider,
            statsSinceLabelFormatter,
            statsUtils,
            analyticsTracker,
            useCaseMode,
            contentDescriptionHelper
        )
    }

    companion object {
        private const val BLOCK_ITEM_COUNT = 6
        private const val VIEW_ALL_PAGE_SIZE = 10
    }
}
