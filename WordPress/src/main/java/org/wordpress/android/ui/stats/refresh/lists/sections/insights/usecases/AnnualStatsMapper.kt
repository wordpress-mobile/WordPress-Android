package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel.YearInsights
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject

class AnnualStatsMapper
@Inject constructor(private val contentDescriptionHelper: ContentDescriptionHelper) {
    fun mapYearInBlock(selectedYear: YearInsights): List<BlockListItem> {
        return listOf(
                QuickScanItem(
                        Column(
                                R.string.stats_insights_year,
                                selectedYear.year
                        ),
                        Column(
                                R.string.stats_insights_posts,
                                selectedYear.totalPosts.toFormattedString()
                        )
                ),
                QuickScanItem(
                        Column(
                                R.string.stats_insights_total_comments,
                                selectedYear.totalComments.toFormattedString()
                        ),
                        Column(
                                R.string.stats_insights_average_comments,
                                selectedYear.avgComments?.toFormattedString() ?: "0"
                        )
                ),
                QuickScanItem(
                        Column(
                                R.string.stats_insights_total_likes,
                                selectedYear.totalLikes.toFormattedString()
                        ),
                        Column(
                                R.string.stats_insights_average_likes,
                                selectedYear.avgLikes?.toFormattedString() ?: "0"
                        )
                ),
                QuickScanItem(
                        Column(
                                R.string.stats_insights_total_words,
                                selectedYear.totalWords.toFormattedString()
                        ),
                        Column(
                                R.string.stats_insights_average_words,
                                selectedYear.avgWords?.toFormattedString() ?: "0"
                        )
                )
        )
    }

    fun mapYearInViewAll(selectedYear: YearInsights): List<BlockListItem> {
        return listOf(
                mapItem(
                        textResource = R.string.stats_insights_posts,
                        value = selectedYear.totalPosts.toFormattedString()
                ),
                mapItem(
                        textResource = R.string.stats_insights_total_comments,
                        value = selectedYear.totalComments.toFormattedString()
                ),
                mapItem(
                        textResource = R.string.stats_insights_average_comments,
                        value = selectedYear.avgComments?.toFormattedString()
                ),
                mapItem(
                        textResource = R.string.stats_insights_total_likes,
                        value = selectedYear.totalLikes.toFormattedString()
                ),
                mapItem(
                        textResource = R.string.stats_insights_average_likes,
                        value = selectedYear.avgLikes?.toFormattedString()
                ),
                mapItem(
                        textResource = R.string.stats_insights_total_words,
                        value = selectedYear.totalWords.toFormattedString()
                ),
                mapItem(
                        textResource = R.string.stats_insights_average_words,
                        value = selectedYear.avgWords?.toFormattedString()
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
