package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ANNUAL_SITE_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightType.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.PUBLICIZE
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TODAY_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_LIKES
import org.wordpress.android.fluxc.store.StatsStore.InsightType.VIEWS_AND_VISITORS
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel.Status
import org.wordpress.android.util.config.StatsRevampV2FeatureConfig

class InsightsManagementMapperTest : BaseUnitTest() {
    @Mock private lateinit var statsRevampV2FeatureConfig: StatsRevampV2FeatureConfig
    private lateinit var insightsManagementMapper: InsightsManagementMapper
    private val insightTypeCount =
            InsightType.values().count() - 1 // Excluding FOLLOWER_TOTALS for statsRevampV2FeatureConfig
    private val sectionsCount = 3
    @InternalCoroutinesApi
    @Before
    fun setUp() {
        insightsManagementMapper = InsightsManagementMapper(TEST_DISPATCHER, statsRevampV2FeatureConfig)
    }

    @Test
    fun `maps all added insights from StatsStore with correct type`() = test {
        // Given
        val allInsightTypes = InsightType.values().toSet()
        whenever(statsRevampV2FeatureConfig.isEnabled()).thenReturn(true)

        // When
        val result = insightsManagementMapper.buildUIModel(allInsightTypes) {}

        // Then
        assertThat(result).hasSize(insightTypeCount + sectionsCount)
        assertHeader(result[0], R.string.stats_insights_management_general)
        assertInsight(result[1], VIEWS_AND_VISITORS, true)
        assertInsight(result[2], ALL_TIME_STATS, true)
        assertInsight(result[3], MOST_POPULAR_DAY_AND_HOUR, true)
        assertInsight(result[4], ANNUAL_SITE_STATS, true)
        assertInsight(result[5], TODAY_STATS, true)
        assertHeader(result[6], R.string.stats_insights_management_posts_and_pages)
        assertInsight(result[7], LATEST_POST_SUMMARY, true)
        assertInsight(result[8], POSTING_ACTIVITY, true)
        assertInsight(result[9], TAGS_AND_CATEGORIES, true)
        assertHeader(result[10], R.string.stats_insights_management_activity)
        assertInsight(result[11], COMMENTS, true)
        assertInsight(result[12], FOLLOWERS, true)
        assertInsight(result[13], TOTAL_LIKES, true)
        assertInsight(result[14], TOTAL_COMMENTS, true)
        assertInsight(result[15], TOTAL_FOLLOWERS, true)
        assertInsight(result[16], PUBLICIZE, true)
    }

    @Test
    fun `maps subset of added insights with correct type`() = test {
        // Given
        val addedInsightTypes = setOf(ALL_TIME_STATS, PUBLICIZE)
        whenever(statsRevampV2FeatureConfig.isEnabled()).thenReturn(true)

        // When
        val result = insightsManagementMapper.buildUIModel(addedInsightTypes) {}

        // Then
        assertThat(result).hasSize(insightTypeCount + sectionsCount)
        assertHeader(result[0], R.string.stats_insights_management_general)
        assertInsight(result[1], VIEWS_AND_VISITORS, false)
        assertInsight(result[2], ALL_TIME_STATS, true)
        assertInsight(result[3], MOST_POPULAR_DAY_AND_HOUR, false)
        assertInsight(result[4], ANNUAL_SITE_STATS, false)
        assertInsight(result[5], TODAY_STATS, false)
        assertHeader(result[6], R.string.stats_insights_management_posts_and_pages)
        assertInsight(result[7], LATEST_POST_SUMMARY, false)
        assertInsight(result[8], POSTING_ACTIVITY, false)
        assertInsight(result[9], TAGS_AND_CATEGORIES, false)
        assertHeader(result[10], R.string.stats_insights_management_activity)
        assertInsight(result[11], COMMENTS, false)
        assertInsight(result[12], FOLLOWERS, false)
        assertInsight(result[13], TOTAL_LIKES, false)
        assertInsight(result[14], TOTAL_COMMENTS, false)
        assertInsight(result[15], TOTAL_FOLLOWERS, false)
        assertInsight(result[16], PUBLICIZE, true)
    }

    @Test
    fun `maps all removed insights with correct type`() = test {
        // Given
        whenever(statsRevampV2FeatureConfig.isEnabled()).thenReturn(true)

        // When
        val result = insightsManagementMapper.buildUIModel(setOf()) {}

        // Then
        assertThat(result).hasSize(insightTypeCount + sectionsCount)
        assertHeader(result[0], R.string.stats_insights_management_general)
        assertInsight(result[1], VIEWS_AND_VISITORS, false)
        assertInsight(result[2], ALL_TIME_STATS, false)
        assertInsight(result[3], MOST_POPULAR_DAY_AND_HOUR, false)
        assertInsight(result[4], ANNUAL_SITE_STATS, false)
        assertInsight(result[5], TODAY_STATS, false)
        assertHeader(result[6], R.string.stats_insights_management_posts_and_pages)
        assertInsight(result[7], LATEST_POST_SUMMARY, false)
        assertInsight(result[8], POSTING_ACTIVITY, false)
        assertInsight(result[9], TAGS_AND_CATEGORIES, false)
        assertHeader(result[10], R.string.stats_insights_management_activity)
        assertInsight(result[11], COMMENTS, false)
        assertInsight(result[12], FOLLOWERS, false)
        assertInsight(result[13], TOTAL_LIKES, false)
        assertInsight(result[14], TOTAL_COMMENTS, false)
        assertInsight(result[15], TOTAL_FOLLOWERS, false)
        assertInsight(result[16], PUBLICIZE, false)
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
