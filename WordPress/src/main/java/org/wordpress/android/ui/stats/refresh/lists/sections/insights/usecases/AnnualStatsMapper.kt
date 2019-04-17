package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel.YearInsights
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject

class AnnualStatsMapper
@Inject constructor() {
    fun mapYearInBlock(selectedYear: YearInsights): List<BlockListItem> {
        return listOf(
                QuickScanItem(
                        Column(
                                string.stats_insights_year,
                                selectedYear.year
                        ),
                        Column(
                                string.stats_insights_posts,
                                selectedYear.totalPosts.toFormattedString()
                        )
                ),
                QuickScanItem(
                        Column(
                                string.stats_insights_total_comments,
                                selectedYear.totalComments.toFormattedString()
                        ),
                        Column(
                                string.stats_insights_average_comments,
                                selectedYear.avgComments?.toFormattedString() ?: "0"
                        )
                ),
                QuickScanItem(
                        Column(
                                string.stats_insights_total_likes,
                                selectedYear.totalLikes.toFormattedString()
                        ),
                        Column(
                                string.stats_insights_average_likes,
                                selectedYear.avgLikes?.toFormattedString() ?: "0"
                        )
                ),
                QuickScanItem(
                        Column(
                                string.stats_insights_total_words,
                                selectedYear.totalWords.toFormattedString()
                        ),
                        Column(
                                string.stats_insights_average_words,
                                selectedYear.avgWords?.toFormattedString() ?: "0"
                        )
                )
        )
    }

    fun mapYearInViewAll(selectedYear: YearInsights): List<BlockListItem> {
        return listOf(
                ListItemWithIcon(
                        textResource = string.stats_insights_posts,
                        value = selectedYear.totalPosts.toFormattedString()
                ),
                ListItemWithIcon(
                        textResource = string.stats_insights_total_comments,
                        value = selectedYear.totalComments.toFormattedString()
                ),
                ListItemWithIcon(
                        textResource = string.stats_insights_average_comments,
                        value = selectedYear.avgComments?.toFormattedString() ?: "0"
                ),
                ListItemWithIcon(
                        textResource = string.stats_insights_total_likes,
                        value = selectedYear.totalLikes.toFormattedString()
                ),
                ListItemWithIcon(
                        textResource = string.stats_insights_average_likes,
                        value = selectedYear.avgLikes?.toFormattedString() ?: "0"
                ),
                ListItemWithIcon(
                        textResource = string.stats_insights_total_words,
                        value = selectedYear.totalWords.toFormattedString()
                ),
                ListItemWithIcon(
                        textResource = string.stats_insights_average_words,
                        value = selectedYear.avgWords?.toFormattedString() ?: "0"
                )
        )
    }
}
