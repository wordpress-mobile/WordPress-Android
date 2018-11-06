package org.wordpress.android.ui.stats.refresh

import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem.Tab
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.BlockListItem.UserItem
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class FollowersUseCase
@Inject constructor(
    private val insightsStore: InsightsStore,
    private val statsUtilsWrapper: StatsUtilsWrapper,
    private val resourceProvider: ResourceProvider
) {
    suspend fun loadFollowers(site: SiteModel, forced: Boolean = false): InsightsItem {
        val deferredWpComResponse = GlobalScope.async { insightsStore.fetchWpComFollowers(site, forced) }
        val deferredEmailResponse = GlobalScope.async { insightsStore.fetchEmailFollowers(site, forced) }
        val wpComResponse = deferredWpComResponse.await()
        val emailResponse = deferredEmailResponse.await()
        val wpComModel = wpComResponse.model
        val emailModel = emailResponse.model
        val error = wpComResponse.error ?: emailResponse.error

        return when {
            error != null -> Failed(R.string.stats_view_followers, error.message ?: error.type.name)
            wpComModel != null || emailModel != null -> loadFollowers(wpComModel, emailModel)
            else -> throw IllegalArgumentException("Unexpected empty body")
        }
    }

    private fun loadFollowers(wpComModel: FollowersModel?, emailModel: FollowersModel?): ListInsightItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_view_followers))
        items.add(
                TabsItem(
                        listOf(
                                buildTab(wpComModel, R.string.stats_followers_wordpress_com),
                                buildTab(emailModel, R.string.stats_followers_email)
                        )
                )
        )

        items.add(Link(text = string.stats_insights_view_more) {})
        return ListInsightItem(items)
    }

    private fun buildTab(model: FollowersModel?, label: Int): Tab {
        val mutableItems = mutableListOf<BlockListItem>()
        if (model != null && model.followers.isNotEmpty()) {
            mutableItems.add(
                    Information(
                            resourceProvider.getString(
                                    string.stats_followers_count_message,
                                    resourceProvider.getString(label),
                                    model.totalCount
                            )
                    )
            )
            mutableItems.add(Label(R.string.stats_follower_label, R.string.stats_follower_since_label))
            model.followers.toUserItems().let { mutableItems.addAll(it) }
        } else {
            mutableItems.add(Empty)
        }
        return Tab(label, mutableItems)
    }

    private fun List<FollowerModel>.toUserItems(): List<UserItem> {
        return this.mapIndexed { index, follower ->
            UserItem(
                    follower.avatar,
                    follower.label,
                    statsUtilsWrapper.getSinceLabelLowerCase(follower.dateSubscribed),
                    index < this.size - 1
            )
        }
    }
}
