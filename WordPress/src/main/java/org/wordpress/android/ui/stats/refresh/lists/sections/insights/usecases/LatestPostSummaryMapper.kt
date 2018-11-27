package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text.Clickable
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class LatestPostSummaryMapper
@Inject constructor(
    private val statsUtilsWrapper: StatsUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val statsDateFormatter: StatsDateFormatter
) {
    fun buildMessageItem(model: InsightsLatestPostModel?): Text {
        if (model == null) {
            return Text(resourceProvider.getString(string.stats_insights_latest_post_empty))
        }
        val sinceLabel = statsUtilsWrapper.getSinceLabelLowerCase(model.postDate)
        val postTitle = StringEscapeUtils.unescapeHtml4(model.postTitle)
        val message = if (model.postViewsCount == 0 && model.postLikeCount == 0) {
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
        return Text(message, listOf(Clickable(postTitle) {
            statsUtilsWrapper.openPostInReaderOrInAppWebview(
                    it,
                    model.siteId,
                    model.postId.toString(),
                    model.postURL
            )
        }))
    }

    fun buildColumnItem(postViewsCount: Int, postLikeCount: Int, postCommentCount: Int): Columns {
        val headers = listOf(R.string.stats_views, R.string.stats_likes, R.string.stats_comments)
        val values = listOf(
                postViewsCount.toFormattedString(),
                postLikeCount.toFormattedString(),
                postCommentCount.toFormattedString()
        )
        return Columns(headers = headers, values = values)
    }

    fun buildBarChartItem(dayViews: List<Pair<String, Int>>): BarChartItem {
        val barEntries = dayViews.subList(Math.max(0, dayViews.size - 30), dayViews.size)
                .map { pair -> statsDateFormatter.parseDate(pair.first) to pair.second }
        return BarChartItem(barEntries)
    }
}
