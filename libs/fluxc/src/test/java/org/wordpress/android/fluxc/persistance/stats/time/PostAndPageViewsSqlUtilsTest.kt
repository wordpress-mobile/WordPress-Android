package org.wordpress.android.fluxc.persistance.stats.time

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.POSTS_AND_PAGES_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.DAY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.MONTH
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.WEEK
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.YEAR
import org.wordpress.android.fluxc.store.DAY_POST_AND_PAGE_VIEWS_RESPONSE
import org.wordpress.android.fluxc.store.MONTH_POST_AND_PAGE_VIEWS_RESPONSE
import org.wordpress.android.fluxc.store.WEEK_POST_AND_PAGE_VIEWS_RESPONSE
import org.wordpress.android.fluxc.store.YEAR_POST_AND_PAGE_VIEWS_RESPONSE
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class PostAndPageViewsSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    @Mock lateinit var site: SiteModel
    private lateinit var timeStatsSqlUtils: TimeStatsSqlUtils

    @Before
    fun setUp() {
        timeStatsSqlUtils = TimeStatsSqlUtils(statsSqlUtils)
    }

    @Test
    fun `returns post and page day views from stats utils`() {
        whenever(statsSqlUtils.select(site, POSTS_AND_PAGES_VIEWS, DAY, PostAndPageViewsResponse::class.java))
                .thenReturn(
                        DAY_POST_AND_PAGE_VIEWS_RESPONSE
                )

        val result = timeStatsSqlUtils.selectPostAndPageViews(site, DAYS)

        assertEquals(result, DAY_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `inserts post and page day views to stats utils`() {
        timeStatsSqlUtils.insert(site, DAY_POST_AND_PAGE_VIEWS_RESPONSE, DAYS)

        verify(statsSqlUtils).insert(site, POSTS_AND_PAGES_VIEWS, DAY, DAY_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `returns post and page week views from stats utils`() {
        whenever(statsSqlUtils.select(site, POSTS_AND_PAGES_VIEWS, WEEK, PostAndPageViewsResponse::class.java))
                .thenReturn(
                        WEEK_POST_AND_PAGE_VIEWS_RESPONSE
                )

        val result = timeStatsSqlUtils.selectPostAndPageViews(site, WEEKS)

        assertEquals(result, WEEK_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `inserts post and page week views to stats utils`() {
        timeStatsSqlUtils.insert(site, WEEK_POST_AND_PAGE_VIEWS_RESPONSE, WEEKS)

        verify(statsSqlUtils).insert(site, POSTS_AND_PAGES_VIEWS, WEEK, WEEK_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `returns post and page month views from stats utils`() {
        whenever(statsSqlUtils.select(site, POSTS_AND_PAGES_VIEWS, MONTH, PostAndPageViewsResponse::class.java))
                .thenReturn(
                        MONTH_POST_AND_PAGE_VIEWS_RESPONSE
                )

        val result = timeStatsSqlUtils.selectPostAndPageViews(site, MONTHS)

        assertEquals(result, MONTH_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `inserts post and page month views to stats utils`() {
        timeStatsSqlUtils.insert(site, MONTH_POST_AND_PAGE_VIEWS_RESPONSE, MONTHS)

        verify(statsSqlUtils).insert(site, POSTS_AND_PAGES_VIEWS, MONTH, MONTH_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `returns post and page year views from stats utils`() {
        whenever(statsSqlUtils.select(site, POSTS_AND_PAGES_VIEWS, YEAR, PostAndPageViewsResponse::class.java))
                .thenReturn(
                        YEAR_POST_AND_PAGE_VIEWS_RESPONSE
                )

        val result = timeStatsSqlUtils.selectPostAndPageViews(site, YEARS)

        assertEquals(result, YEAR_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `inserts post and page year views to stats utils`() {
        timeStatsSqlUtils.insert(site, YEAR_POST_AND_PAGE_VIEWS_RESPONSE, YEARS)

        verify(statsSqlUtils).insert(site, POSTS_AND_PAGES_VIEWS, YEAR, YEAR_POST_AND_PAGE_VIEWS_RESPONSE)
    }
}
