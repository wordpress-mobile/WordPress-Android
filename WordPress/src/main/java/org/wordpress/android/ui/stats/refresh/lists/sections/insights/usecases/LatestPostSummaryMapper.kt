package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text.Clickable
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.LatestPostSummaryUseCase.LinkClickParams
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class LatestPostSummaryMapper
@Inject constructor(
    private val statsUtilsWrapper: StatsUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val statsUtils: StatsUtils
) {
    fun buildMessageItem(
        model: InsightsLatestPostModel?,
        navigationAction: (params: LinkClickParams) -> Unit
    ): Text {
        if (model == null) {
            return Text(resourceProvider.getString(R.string.stats_insights_latest_post_empty))
        }
        val sinceLabel = statsUtilsWrapper.getSinceLabelLowerCase(model.postDate)
        val postTitle = if (model.postTitle.isNotBlank()) {
            StringEscapeUtils.unescapeHtml4(model.postTitle)
        } else {
            resourceProvider.getString(R.string.untitled_in_parentheses)
        }
        val message = if (model.postViewsCount == 0 && model.postLikeCount == 0) {
            resourceProvider.getString(
                    R.string.stats_insights_latest_post_with_no_engagement,
                    sinceLabel,
                    postTitle
            )
        } else {
            resourceProvider.getString(
                    R.string.stats_insights_latest_post_message,
                    sinceLabel,
                    postTitle
            )
        }
        return Text(
                text = message,
                links = listOf(
                        Clickable(
                                postTitle,
                                navigationAction = NavigationAction.create(
                                        LinkClickParams(model.postId, model.postURL),
                                        action = navigationAction
                                )
                        )
                )
        )
    }

    fun buildBarChartItem(dayViews: List<Pair<String, Int>>): BarChartItem {
        val barEntries = dayViews.subList(Math.max(0, dayViews.size - 30), dayViews.size)
                .map { pair -> BarChartItem.Bar(statsDateFormatter.printDate(pair.first), pair.first, pair.second) }

        val contentDescriptions = statsUtils.getBarChartEntryContentDescriptions(
                R.string.stats_views,
                barEntries)

        return BarChartItem(barEntries, entryContentDescriptions = contentDescriptions)
    }
}
