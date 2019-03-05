package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWERS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewFollowersStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
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
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 6

class FollowersUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsUtilsWrapper: StatsUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : StatefulUseCase<Pair<FollowersModel, FollowersModel>, Int>(
        FOLLOWERS,
        mainDispatcher,
        0
) {
    override suspend fun loadCachedData(): Pair<FollowersModel, FollowersModel>? {
        val wpComFollowers = insightsStore.getWpComFollowers(statsSiteProvider.siteModel, PAGE_SIZE)
        val emailFollowers = insightsStore.getEmailFollowers(statsSiteProvider.siteModel, PAGE_SIZE)
        if (wpComFollowers != null && emailFollowers != null) {
            return wpComFollowers to emailFollowers
        }
        return null
    }

    override suspend fun fetchRemoteData(
        forced: Boolean
    ): State<Pair<FollowersModel, FollowersModel>> {
        val deferredWpComResponse = GlobalScope.async {
            insightsStore.fetchWpComFollowers(
                    statsSiteProvider.siteModel,
                    PAGE_SIZE,
                    forced
            )
        }
        val deferredEmailResponse = GlobalScope.async {
            insightsStore.fetchEmailFollowers(
                    statsSiteProvider.siteModel,
                    PAGE_SIZE,
                    forced
            )
        }
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

            if (wpComModel.hasMore || emailModel.hasMore) {
                items.add(
                        Link(
                                text = string.stats_insights_view_more,
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
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_FOLLOWERS_VIEW_MORE_TAPPED)
        navigateTo(ViewFollowersStats(statsSiteProvider.siteModel))
    }
}
