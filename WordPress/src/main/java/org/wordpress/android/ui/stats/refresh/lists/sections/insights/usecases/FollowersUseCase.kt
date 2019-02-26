package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CacheMode
import org.wordpress.android.fluxc.model.stats.FetchMode
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWERS
import org.wordpress.android.fluxc.store.stats.insights.FollowersStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewFollowersStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6
private const val VIEW_ALL_PAGE_SIZE = 10

class FollowersUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val followersStore: FollowersStore,
    private val statsUtilsWrapper: StatsUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val useCaseMode: UseCaseMode
) : StatefulUseCase<Pair<FollowersModel, FollowersModel>, Int>(
        FOLLOWERS,
        mainDispatcher,
        0
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_PAGE_SIZE else BLOCK_ITEM_COUNT
    private lateinit var lastSite: SiteModel

    override suspend fun loadCachedData(site: SiteModel): Pair<FollowersModel, FollowersModel>? {
        lastSite = site
        val wpComFollowers = followersStore.getWpComFollowers(site, CacheMode.Top(itemsToLoad))
        val emailFollowers = followersStore.getEmailFollowers(site, CacheMode.Top(itemsToLoad))
        if (wpComFollowers != null && emailFollowers != null) {
            return wpComFollowers to emailFollowers
        }
        return null
    }

    override suspend fun fetchRemoteData(
        site: SiteModel,
        forced: Boolean
    ): State<Pair<FollowersModel, FollowersModel>> {
        return fetchData(site, forced, FetchMode.Paged(itemsToLoad, false))
    }

    private suspend fun fetchData(
        site: SiteModel,
        forced: Boolean,
        fetchMode: FetchMode.Paged
    ): State<Pair<FollowersModel, FollowersModel>> {
        lastSite = site

        val deferredWpComResponse = GlobalScope.async { followersStore.fetchWpComFollowers(site, fetchMode, forced) }
        val deferredEmailResponse = GlobalScope.async { followersStore.fetchEmailFollowers(site, fetchMode, forced) }

        val wpComResponse = deferredWpComResponse.await()
        val emailResponse = deferredEmailResponse.await()
        val wpComModel = wpComResponse.model
        val emailModel = emailResponse.model
        val error = wpComResponse.error ?: emailResponse.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            wpComModel != null && emailModel != null &&
                    (wpComModel.followers.isNotEmpty() || emailModel.followers.isNotEmpty()) -> State.Data(
                    wpComModel to emailModel
            )
            else -> State.Empty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_view_followers))

    override fun buildStatefulUiModel(
        domainModel: Pair<FollowersModel, FollowersModel>,
        uiState: Int
    ): List<BlockListItem> {
        val wpComModel = domainModel.first
        val emailModel = domainModel.second
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_view_followers))
        if (domainModel.first.followers.isNotEmpty() || domainModel.second.followers.isNotEmpty()) {
            items.add(
                    TabsItem(
                            listOf(
                                    R.string.stats_followers_wordpress_com,
                                    R.string.stats_followers_email
                            ),
                            uiState
                    ) {
                        onUiState(it)
                    }
            )
            if (uiState == 0) {
                items.addAll(buildTab(wpComModel, R.string.stats_followers_wordpress_com))
            } else {
                items.addAll(buildTab(emailModel, R.string.stats_followers_email))
            }

            if (wpComModel.hasMore && uiState == 0 || emailModel.hasMore && uiState == 1) {
                val buttonText = if (useCaseMode == VIEW_ALL)
                        R.string.stats_insights_load_more
                    else
                        R.string.stats_insights_view_more
                items.add(
                        Link(
                                text = buttonText,
                                navigateAction = NavigationAction.create(this::onLinkClick)
                        )
                )
            }
        } else {
            items.add(Empty())
        }
        return items
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

    private fun onLinkClick() {
        if (useCaseMode == VIEW_ALL) {
            GlobalScope.launch {
                val state = fetchData(lastSite, true, FetchMode.Paged(itemsToLoad, true))
                evaluateState(state)
            }
        } else {
            analyticsTracker.track(AnalyticsTracker.Stat.STATS_FOLLOWERS_VIEW_MORE_TAPPED)
            navigateTo(ViewFollowersStats())
        }
    }

    class FollowersUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val followersStore: FollowersStore,
        private val statsUtilsWrapper: StatsUtilsWrapper,
        private val resourceProvider: ResourceProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                FollowersUseCase(
                        mainDispatcher,
                        followersStore,
                        statsUtilsWrapper,
                        resourceProvider,
                        analyticsTracker,
                        useCaseMode
                )
    }
}
