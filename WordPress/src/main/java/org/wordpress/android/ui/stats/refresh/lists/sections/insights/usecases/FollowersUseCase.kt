package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.PagedMode
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWERS
import org.wordpress.android.fluxc.store.stats.insights.FollowersStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewFollowersStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LoadingItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase.FollowersUiState
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6
private const val VIEW_ALL_PAGE_SIZE = 10

class FollowersUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val followersStore: FollowersStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsUtilsWrapper: StatsUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val useCaseMode: UseCaseMode
) : StatefulUseCase<Pair<FollowersModel, FollowersModel>, FollowersUiState>(
        FOLLOWERS,
        mainDispatcher,
        FollowersUiState(isLoading = true)
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_PAGE_SIZE else BLOCK_ITEM_COUNT

    override suspend fun loadCachedData(): Pair<FollowersModel, FollowersModel>? {
        val cacheMode = if (useCaseMode == VIEW_ALL) LimitMode.All else LimitMode.Top(itemsToLoad)
        val wpComFollowers = followersStore.getWpComFollowers(statsSiteProvider.siteModel, cacheMode)
        val emailFollowers = followersStore.getEmailFollowers(statsSiteProvider.siteModel, cacheMode)
        if (wpComFollowers != null && emailFollowers != null) {
            return wpComFollowers to emailFollowers
        }
        return null
    }

    override suspend fun fetchRemoteData(
        forced: Boolean
    ): State<Pair<FollowersModel, FollowersModel>> {
        return fetchData(forced, PagedMode(itemsToLoad, false))
    }

    private suspend fun fetchData(
        forced: Boolean,
        fetchMode: PagedMode
    ): State<Pair<FollowersModel, FollowersModel>> {
        runBlocking(mainDispatcher) {
            updateUiState { it.copy(isLoading = true) }
        }
        val deferredWpComResponse = GlobalScope.async(bgDispatcher) {
            followersStore.fetchWpComFollowers(
                    statsSiteProvider.siteModel,
                    fetchMode,
                    forced
            )
        }
        val deferredEmailResponse = GlobalScope.async(bgDispatcher) {
            followersStore.fetchEmailFollowers(
                    statsSiteProvider.siteModel,
                    fetchMode,
                    forced
            )
        }

        val wpComResponse = deferredWpComResponse.await()
        val emailResponse = deferredEmailResponse.await()
        val wpComModel = wpComResponse.model
        val emailModel = emailResponse.model
        val error = wpComResponse.error ?: emailResponse.error
        runBlocking(mainDispatcher) {
            updateUiState { it.copy(isLoading = false) }
        }
        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            wpComModel != null && emailModel != null &&
                    (wpComModel.followers.isNotEmpty() || emailModel.followers.isNotEmpty()) -> State.Data(
                    wpComModel to emailModel,
                    cached = wpComResponse.cached && emailResponse.cached
            )
            else -> State.Empty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_view_followers))

    override fun buildStatefulUiModel(
        domainModel: Pair<FollowersModel, FollowersModel>,
        uiState: FollowersUiState
    ): List<BlockListItem> {
        val wpComModel = domainModel.first
        val emailModel = domainModel.second
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(string.stats_view_followers))
        }

        if (domainModel.first.followers.isNotEmpty() || domainModel.second.followers.isNotEmpty()) {
            items.add(
                    TabsItem(
                            listOf(
                                    R.string.stats_followers_wordpress_com,
                                    R.string.stats_followers_email
                            ),
                            uiState.selectedTab
                    ) { selectedTab ->
                        updateUiState { it.copy(selectedTab = selectedTab) }
                    }
            )
            if (uiState.selectedTab == 0) {
                items.addAll(buildTab(wpComModel, R.string.stats_followers_wordpress_com))
            } else {
                items.addAll(buildTab(emailModel, R.string.stats_followers_email))
            }

            if (wpComModel.hasMore && uiState.selectedTab == 0 || emailModel.hasMore && uiState.selectedTab == 1) {
                if (useCaseMode == BLOCK) {
                    val buttonText = R.string.stats_insights_view_more
                    items.add(
                            Link(
                                    text = buttonText,
                                    navigateAction = NavigationAction.create(uiState.selectedTab, this::onLinkClick)
                            )
                    )
                } else if (wpComModel.followers.size >= VIEW_ALL_PAGE_SIZE && uiState.selectedTab == 0 ||
                        emailModel.followers.size >= VIEW_ALL_PAGE_SIZE && uiState.selectedTab == 1) {
                    items.add(LoadingItem(this::loadMore, isLoading = uiState.isLoading))
                }
            }
        } else {
            items.add(Empty())
        }
        return items
    }

    private fun loadMore() {
        GlobalScope.launch(bgDispatcher) {
            val state = fetchData(true, PagedMode(itemsToLoad, true))
            evaluateState(state)
        }
    }

    private fun buildTab(model: FollowersModel, label: Int): List<BlockListItem> {
        val mutableItems = mutableListOf<BlockListItem>()
        if (model.followers.isNotEmpty()) {
            mutableItems.add(
                    Information(
                            resourceProvider.getString(
                                    string.stats_followers_count_message,
                                    resourceProvider.getString(label),
                                    model.totalCount
                            )
                    )
            )
            mutableItems.add(Header(R.string.stats_follower_label, R.string.stats_follower_since_label))
            model.followers.toUserItems().let { mutableItems.addAll(it) }
        } else {
            mutableItems.add(Empty())
        }
        return mutableItems
    }

    private fun List<FollowerModel>.toUserItems(): List<ListItemWithIcon> {
        return this.mapIndexed { index, follower ->
            ListItemWithIcon(
                    iconUrl = follower.avatar,
                    iconStyle = AVATAR,
                    text = follower.label,
                    value = statsUtilsWrapper.getSinceLabelLowerCase(follower.dateSubscribed),
                    showDivider = index < this.size - 1
            )
        }
    }

    private fun onLinkClick(selectedTab: Int) {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_FOLLOWERS_VIEW_MORE_TAPPED)
        navigateTo(ViewFollowersStats(selectedTab))
    }

    data class FollowersUiState(val selectedTab: Int = 0, val isLoading: Boolean = false)

    class FollowersUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
        private val followersStore: FollowersStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val statsUtilsWrapper: StatsUtilsWrapper,
        private val resourceProvider: ResourceProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                FollowersUseCase(
                        mainDispatcher,
                        bgDispatcher,
                        followersStore,
                        statsSiteProvider,
                        statsUtilsWrapper,
                        resourceProvider,
                        analyticsTracker,
                        useCaseMode
                )
    }
}
