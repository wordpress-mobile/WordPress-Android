package org.wordpress.android.ui.stats.refresh

import android.text.Spannable
import android.text.SpannableStringBuilder
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.ui.stats.StatsUtils
import org.wordpress.android.ui.stats.refresh.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale
import javax.inject.Inject

class LatestPostSummaryDomain
@Inject constructor(
    private val insightsStore: InsightsStore,
    private val resourceProvider: ResourceProvider
) {
    suspend fun latestPostSummary(site: SiteModel, forced: Boolean = false): InsightsItem {
        val response = insightsStore.fetchLatestPostInsights(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> Failed(R.string.stats_insights_latest_post_summary, error.message ?: error.type.name)
            else -> latestPostSummaryItem(model)
        }
    }

    private fun latestPostSummaryItem(model: InsightsLatestPostModel?): ListInsightItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_insights_latest_post_summary))
        if (model != null) {
            val message = buildMessage(model)
            items.add(Text(message))
            if (model.postViewsCount > 0 || model.postCommentCount > 0 || model.postLikeCount > 0) {
                val headers = listOf(R.string.stats_views, R.string.stats_likes, R.string.stats_comments)
                val values = listOf(
                        model.postViewsCount.toFormattedString(),
                        model.postLikeCount.toFormattedString(),
                        model.postCommentCount.toFormattedString()
                )
                items.add(Columns(headers = headers, values = values))
            }
        } else {
            val emptyMessage = resourceProvider.getString(R.string.stats_insights_latest_post_empty)
            items.add(Text(SpannableStringBuilder(emptyMessage)))
        }
        return ListInsightItem(items)
    }

    private fun buildMessage(model: InsightsLatestPostModel): Spannable {
        val sinceLabel = StatsUtils.getSinceLabel(resourceProvider, model.postDate)
                .toLowerCase(Locale.getDefault())
        val postTitle = StringEscapeUtils.unescapeHtml4(model.postTitle)
        val message = if (model.postViewsCount == 0) {
            resourceProvider.getString(
                    string.stats_insights_latest_post_with_no_engagement,
                    sinceLabel,
                    postTitle
            )
        } else {
            resourceProvider.getString(
                    string.stats_insights_latest_post_message,
                    sinceLabel,
                    postTitle
            )
        }
        return SpannableStringBuilder(message)
    }
}
