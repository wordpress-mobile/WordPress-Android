package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewMore
import javax.inject.Inject

class LatestPostSummaryViewModel
@Inject constructor(
    private val insightsStore: InsightsStore,
    private val latestPostSummaryMapper: LatestPostSummaryMapper
) {
    private val mutableNavigationTarget = MutableLiveData<NavigationTarget>()
    val navigationTarget: LiveData<NavigationTarget> = mutableNavigationTarget

    suspend fun loadLatestPostSummary(site: SiteModel, forced: Boolean = false): InsightsItem {
        val response = insightsStore.fetchLatestPostInsights(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> Failed(R.string.stats_insights_latest_post_summary, error.message ?: error.type.name)
            else -> loadLatestPostSummaryItem(model)
        }
    }

    private fun loadLatestPostSummaryItem(model: InsightsLatestPostModel?): ListInsightItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_insights_latest_post_summary))
        items.add(latestPostSummaryMapper.buildMessageItem(model))
        if (model != null && model.hasData()) {
            items.add(
                    latestPostSummaryMapper.buildColumnItem(
                            model.postViewsCount,
                            model.postLikeCount,
                            model.postCommentCount
                    )
            )
            if (model.dayViews.isNotEmpty()) {
                items.add(latestPostSummaryMapper.buildBarChartItem(model.dayViews))
            }
        }
        items.add(buildLink(model))
        return ListInsightItem(items)
    }

    private fun InsightsLatestPostModel.hasData() =
            this.postViewsCount > 0 || this.postCommentCount > 0 || this.postLikeCount > 0

    private fun buildLink(model: InsightsLatestPostModel?): Link {
        return when {
            model == null -> Link(R.drawable.ic_create_blue_medium_24dp, R.string.stats_insights_create_post) {
                mutableNavigationTarget.value = AddNewPost
            }
            model.hasData() -> Link(text = R.string.stats_insights_view_more) {
                mutableNavigationTarget.value = ViewMore(
                        model.siteId,
                        model.postId.toString(),
                        model.postTitle,
                        model.postURL
                )
            }
            else -> Link(R.drawable.ic_share_blue_medium_24dp, R.string.stats_insights_share_post) {
                mutableNavigationTarget.value = SharePost(model.postURL, model.postTitle)
            }
        }
    }
}
