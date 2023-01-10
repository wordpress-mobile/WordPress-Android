package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel.YearInsights
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import javax.inject.Inject

class AnnualStatsMapper
@Inject constructor(
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val statsUtils: StatsUtils
) {
    fun mapYearInBlock(selectedYear: YearInsights): List<BlockListItem> {
        return listOf(
            QuickScanItem(
                Column(
                    R.string.stats_insights_year,
                    selectedYear.year
                ),
                Column(
                    R.string.stats_insights_posts,
                    statsUtils.toFormattedString(selectedYear.totalPosts)
                )
            ),
            QuickScanItem(
                Column(
                    R.string.stats_insights_total_comments,
                    statsUtils.toFormattedString(selectedYear.totalComments)
                ),
                Column(
                    R.string.stats_insights_average_comments,
                    statsUtils.toFormattedString(selectedYear.avgComments, defaultValue = "0")
                )
            ),
            QuickScanItem(
                Column(
                    R.string.stats_insights_total_likes,
                    statsUtils.toFormattedString(selectedYear.totalLikes)
                ),
                Column(
                    R.string.stats_insights_average_likes,
                    statsUtils.toFormattedString(selectedYear.avgLikes, defaultValue = "0")
                )
            ),
            QuickScanItem(
                Column(
                    R.string.stats_insights_total_words,
                    statsUtils.toFormattedString(selectedYear.totalWords)
                ),
                Column(
                    R.string.stats_insights_average_words,
                    statsUtils.toFormattedString(selectedYear.avgWords, defaultValue = "0")
                )
            )
        )
    }

    fun mapYearInViewAll(selectedYear: YearInsights): List<BlockListItem> {
        return listOf(
            mapItem(
                textResource = R.string.stats_insights_posts,
                value = statsUtils.toFormattedString(selectedYear.totalPosts)
            ),
            mapItem(
                textResource = R.string.stats_insights_total_comments,
                value = statsUtils.toFormattedString(selectedYear.totalComments)
            ),
            mapItem(
                textResource = R.string.stats_insights_average_comments,
                value = statsUtils.toFormattedString(selectedYear.avgComments)
            ),
            mapItem(
                textResource = R.string.stats_insights_total_likes,
                value = statsUtils.toFormattedString(selectedYear.totalLikes)
            ),
            mapItem(
                textResource = R.string.stats_insights_average_likes,
                value = statsUtils.toFormattedString(selectedYear.avgLikes)
            ),
            mapItem(
                textResource = R.string.stats_insights_total_words,
                value = statsUtils.toFormattedString(selectedYear.totalWords)
            ),
            mapItem(
                textResource = R.string.stats_insights_average_words,
                value = statsUtils.toFormattedString(selectedYear.avgWords)
            )
        )
    }

    private fun mapItem(textResource: Int, value: String?): ListItemWithIcon {
        val nonNullValue = value ?: "0"
        return ListItemWithIcon(
            textResource = textResource,
            value = nonNullValue,
            contentDescription = contentDescriptionHelper.buildContentDescription(
                textResource,
                nonNullValue
            )
        )
    }
}
