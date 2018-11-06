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
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem.Tab
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.BlockListItem.UserItem
import javax.inject.Inject

class FollowersUseCase
@Inject constructor(private val insightsStore: InsightsStore, private val statsUtilsWrapper: StatsUtilsWrapper) {
    suspend fun loadFollowers(site: SiteModel, forced: Boolean = false): InsightsItem {
        val deferredWpComResponse = GlobalScope.async {insightsStore.fetchWpComFollowers(site, forced)}
        val deferredEmailResponse = GlobalScope.async {insightsStore.fetchEmailFollowers(site, forced)}
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
        val wpComFollowerItems = wpComModel?.followers?.toUserItems() ?: listOf()
        val emailFollowersItems = emailModel?.followers?.toUserItems() ?: listOf()
        items.add(
                TabsItem(
                        listOf(
                                Tab(R.string.wordpress_dot_com, wpComFollowerItems),
                                Tab(R.string.email, emailFollowersItems)
                        )
                )
        )

        items.add(Link(text = string.stats_insights_view_more) {})
        return ListInsightItem(items)
    }

    private fun List<FollowerModel>.toUserItems(): List<UserItem> {
        return this.mapIndexed { index, follower ->
            UserItem(
                    follower.avatar,
                    follower.label,
                    statsUtilsWrapper.getSinceLabelLowerCase(follower.dateSubscribed),
                    index < this.size -1
            )
        }
    }
}
