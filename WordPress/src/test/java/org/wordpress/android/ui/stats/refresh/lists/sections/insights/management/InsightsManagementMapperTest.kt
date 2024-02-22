package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ANNUAL_SITE_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWER_TOTALS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightType.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.PUBLICIZE
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TAGS_AND_CATEGORIES
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel.Status
import org.wordpress.android.util.config.StatsTrafficTabFeatureConfig

@ExperimentalCoroutinesApi
class InsightsManagementMapperTest : BaseUnitTest() {
    @Mock
    private lateinit var trafficTabFeatureConfig: StatsTrafficTabFeatureConfig

    private lateinit var insightsManagementMapper: InsightsManagementMapper
    private var insightTypeCount = 9 // POSTS_AND_PAGES_INSIGHTS.size + ACTIVITY_INSIGHTS.size + GENERAL_INSIGHTS.size
    private val sectionsCount = 3

    @Before
    fun setUp() {
        insightsManagementMapper = InsightsManagementMapper(
            testDispatcher(),
            trafficTabFeatureConfig
        )
    }

    @Test
    fun `maps all added insights from StatsStore with correct type`() = test {
        // Given
        val allInsightTypes = InsightType.values().toSet()

        // When
        val result = insightsManagementMapper.buildUIModel(allInsightTypes) {}

        // Then
        assertThat(result).hasSize(insightTypeCount + sectionsCount)
        assertHeader(result[0], R.string.stats_insights_management_general)
        assertInsight(result[1], ALL_TIME_STATS, true)
        assertInsight(result[2], MOST_POPULAR_DAY_AND_HOUR, true)
        assertInsight(result[3], ANNUAL_SITE_STATS, true)
        assertHeader(result[4], R.string.stats_insights_management_posts_and_pages)
        assertInsight(result[5], LATEST_POST_SUMMARY, true)
        assertInsight(result[6], POSTING_ACTIVITY, true)
        assertInsight(result[7], TAGS_AND_CATEGORIES, true)
        assertHeader(result[8], R.string.stats_insights_management_activity)
        assertInsight(result[9], FOLLOWERS, true)
        assertInsight(result[10], FOLLOWER_TOTALS, true)
        assertInsight(result[11], PUBLICIZE, true)
    }

    @Test
    fun `maps subset of added insights with correct type`() = test {
        // Given
        val addedInsightTypes = setOf(ALL_TIME_STATS, PUBLICIZE)

        // When
        val result = insightsManagementMapper.buildUIModel(addedInsightTypes) {}

        // Then
        assertThat(result).hasSize(insightTypeCount + sectionsCount)
        assertHeader(result[0], R.string.stats_insights_management_general)
        assertInsight(result[1], ALL_TIME_STATS, true)
        assertInsight(result[2], MOST_POPULAR_DAY_AND_HOUR, false)
        assertInsight(result[3], ANNUAL_SITE_STATS, false)
        assertHeader(result[4], R.string.stats_insights_management_posts_and_pages)
        assertInsight(result[5], LATEST_POST_SUMMARY, false)
        assertInsight(result[6], POSTING_ACTIVITY, false)
        assertInsight(result[7], TAGS_AND_CATEGORIES, false)
        assertHeader(result[8], R.string.stats_insights_management_activity)
        assertInsight(result[9], FOLLOWERS, false)
        assertInsight(result[10], FOLLOWER_TOTALS, false)
        assertInsight(result[11], PUBLICIZE, true)
    }

    @Test
    fun `maps all removed insights with correct type`() = test {
        // When
        val result = insightsManagementMapper.buildUIModel(setOf()) {}

        // Then
        assertThat(result).hasSize(insightTypeCount + sectionsCount)
        assertHeader(result[0], R.string.stats_insights_management_general)
        assertInsight(result[1], ALL_TIME_STATS, false)
        assertInsight(result[2], MOST_POPULAR_DAY_AND_HOUR, false)
        assertInsight(result[3], ANNUAL_SITE_STATS, false)
        assertHeader(result[4], R.string.stats_insights_management_posts_and_pages)
        assertInsight(result[5], LATEST_POST_SUMMARY, false)
        assertInsight(result[6], POSTING_ACTIVITY, false)
        assertInsight(result[7], TAGS_AND_CATEGORIES, false)
        assertHeader(result[8], R.string.stats_insights_management_activity)
        assertInsight(result[9], FOLLOWERS, false)
        assertInsight(result[10], FOLLOWER_TOTALS, false)
        assertInsight(result[11], PUBLICIZE, false)
    }

    private fun assertHeader(item: InsightListItem, text: Int) {
        assertThat(item).isInstanceOf(Header::class.java)
        assertThat((item as Header).text).isEqualTo(text)
    }

    private fun assertInsight(item: InsightListItem, insightType: InsightType, added: Boolean) {
        assertThat(item).isInstanceOf(InsightModel::class.java)
        assertThat((item as InsightModel).insightType).isEqualTo(insightType)
        assertThat(item.status).isEqualTo(if (added) Status.ADDED else Status.REMOVED)
    }
}
