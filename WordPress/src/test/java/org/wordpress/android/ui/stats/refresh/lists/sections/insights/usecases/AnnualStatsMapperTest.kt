package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel.YearInsights
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.QUICK_SCAN_ITEM

@RunWith(MockitoJUnitRunner::class)
class AnnualStatsMapperTest {
    private val annualStatsMapper = AnnualStatsMapper()
    @Test
    fun `maps year insights for the card`() {
        val mappedYear = YearInsights(
                2.567,
                1.5,
                578.1,
                53678.8,
                155,
                89,
                746,
                12,
                237462847,
                "2019"
        )
        val result = annualStatsMapper.mapYearInBlock(mappedYear)
        assertThat(result).hasSize(4)
        assertQuickScanItem(result[0], R.string.stats_insights_year, "2019", R.string.stats_insights_posts, "12")
        assertQuickScanItem(
                result[1],
                R.string.stats_insights_total_comments,
                "155",
                R.string.stats_insights_average_comments,
                "2.6"
        )
        assertQuickScanItem(
                result[2],
                R.string.stats_insights_total_likes,
                "746",
                R.string.stats_insights_average_likes,
                "578.1"
        )
        assertQuickScanItem(
                result[3],
                R.string.stats_insights_total_words,
                "237M",
                R.string.stats_insights_average_words,
                "53k"
        )
    }

    @Test
    fun `maps year insights for the view all`() {
        val mappedYear = YearInsights(
                2.567,
                1.5,
                578.1,
                53678.8,
                155,
                89,
                746,
                12,
                237462847,
                "2019"
        )
        val result = annualStatsMapper.mapYearInViewAll(mappedYear)
        assertThat(result).hasSize(7)
        assertListItem(result[0], R.string.stats_insights_posts, "12")
        assertListItem(result[1], R.string.stats_insights_total_comments, "155")
        assertListItem(result[2], R.string.stats_insights_average_comments, "2.6")
        assertListItem(result[3], R.string.stats_insights_total_likes, "746")
        assertListItem(result[4], R.string.stats_insights_average_likes, "578.1")
        assertListItem(result[5], R.string.stats_insights_total_words, "237M")
        assertListItem(result[6], R.string.stats_insights_average_words, "53k")
    }

    private fun assertQuickScanItem(
        blockListItem: BlockListItem,
        startLabel: Int,
        startValue: String,
        endLabel: Int,
        endValue: String
    ) {
        assertThat(blockListItem.type).isEqualTo(QUICK_SCAN_ITEM)
        val item = blockListItem as QuickScanItem
        assertThat(item.startColumn.label).isEqualTo(startLabel)
        assertThat(item.startColumn.value).isEqualTo(startValue)
        assertThat(item.endColumn.label).isEqualTo(endLabel)
        assertThat(item.endColumn.value).isEqualTo(endValue)
    }

    private fun assertListItem(
        blockListItem: BlockListItem,
        label: Int,
        value: String
    ) {
        assertThat(blockListItem.type).isEqualTo(LIST_ITEM_WITH_ICON)
        val item = blockListItem as ListItemWithIcon
        assertThat(item.textResource).isEqualTo(label)
        assertThat(item.value).isEqualTo(value)
    }
}
