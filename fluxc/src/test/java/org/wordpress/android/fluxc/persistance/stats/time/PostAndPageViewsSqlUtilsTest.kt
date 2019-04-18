package org.wordpress.android.fluxc.persistance.stats.time

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.StatsRequestSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.POSTS_AND_PAGES_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.DAY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.MONTH
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.WEEK
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.YEAR
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.PostsAndPagesSqlUtils
import org.wordpress.android.fluxc.store.stats.time.DAY_POST_AND_PAGE_VIEWS_RESPONSE
import org.wordpress.android.fluxc.store.stats.time.MONTH_POST_AND_PAGE_VIEWS_RESPONSE
import org.wordpress.android.fluxc.store.stats.time.WEEK_POST_AND_PAGE_VIEWS_RESPONSE
import org.wordpress.android.fluxc.store.stats.time.YEAR_POST_AND_PAGE_VIEWS_RESPONSE
import java.util.Date
import kotlin.test.assertEquals

private val DATE = Date(0)
private const val DATE_VALUE = "2018-10-10"

@RunWith(MockitoJUnitRunner::class)
class PostAndPageViewsSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    @Mock lateinit var statsUtils: StatsUtils
    @Mock lateinit var site: SiteModel
    @Mock lateinit var statsRequestSqlUtils: StatsRequestSqlUtils
    private lateinit var timeStatsSqlUtils: PostsAndPagesSqlUtils

    @Before
    fun setUp() {
        timeStatsSqlUtils = PostsAndPagesSqlUtils(statsSqlUtils, statsUtils, statsRequestSqlUtils)
        whenever(statsUtils.getFormattedDate(eq(DATE))).thenReturn(DATE_VALUE)
    }

    @Test
    fun `returns post and page day views from stats utils`() {
        whenever(
                statsSqlUtils.select(
                        site,
                        POSTS_AND_PAGES_VIEWS,
                        DAY,
                        PostAndPageViewsResponse::class.java,
                        DATE_VALUE
                )
        )
                .thenReturn(
                        DAY_POST_AND_PAGE_VIEWS_RESPONSE
                )

        val result = timeStatsSqlUtils.select(site, DAYS, DATE)

        assertEquals(result, DAY_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `inserts post and page day views to stats utils`() {
        timeStatsSqlUtils.insert(
                site,
                DAY_POST_AND_PAGE_VIEWS_RESPONSE, DAYS, DATE
        )

        verify(statsSqlUtils).insert(site, POSTS_AND_PAGES_VIEWS, DAY, DAY_POST_AND_PAGE_VIEWS_RESPONSE,
                true, DATE_VALUE)
    }

    @Test
    fun `returns post and page week views from stats utils`() {
        whenever(
                statsSqlUtils.select(
                        site,
                        POSTS_AND_PAGES_VIEWS,
                        WEEK,
                        PostAndPageViewsResponse::class.java,
                        DATE_VALUE
                )
        )
                .thenReturn(
                        WEEK_POST_AND_PAGE_VIEWS_RESPONSE
                )

        val result = timeStatsSqlUtils.select(site, WEEKS, DATE)

        assertEquals(result, WEEK_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `inserts post and page week views to stats utils`() {
        timeStatsSqlUtils.insert(
                site,
                WEEK_POST_AND_PAGE_VIEWS_RESPONSE, WEEKS, DATE
        )

        verify(statsSqlUtils).insert(site, POSTS_AND_PAGES_VIEWS, WEEK, WEEK_POST_AND_PAGE_VIEWS_RESPONSE,
                true, DATE_VALUE)
    }

    @Test
    fun `returns post and page month views from stats utils`() {
        whenever(
                statsSqlUtils.select(
                        site,
                        POSTS_AND_PAGES_VIEWS,
                        MONTH,
                        PostAndPageViewsResponse::class.java,
                        DATE_VALUE
                )
        )
                .thenReturn(
                        MONTH_POST_AND_PAGE_VIEWS_RESPONSE
                )

        val result = timeStatsSqlUtils.select(site, MONTHS, DATE)

        assertEquals(result, MONTH_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `inserts post and page month views to stats utils`() {
        timeStatsSqlUtils.insert(
                site,
                MONTH_POST_AND_PAGE_VIEWS_RESPONSE, MONTHS, DATE
        )

        verify(statsSqlUtils).insert(site, POSTS_AND_PAGES_VIEWS, MONTH, MONTH_POST_AND_PAGE_VIEWS_RESPONSE,
                true, DATE_VALUE)
    }

    @Test
    fun `returns post and page year views from stats utils`() {
        whenever(
                statsSqlUtils.select(
                        site,
                        POSTS_AND_PAGES_VIEWS,
                        YEAR,
                        PostAndPageViewsResponse::class.java,
                        DATE_VALUE
                )
        )
                .thenReturn(
                        YEAR_POST_AND_PAGE_VIEWS_RESPONSE
                )

        val result = timeStatsSqlUtils.select(site, YEARS, DATE)

        assertEquals(result, YEAR_POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `inserts post and page year views to stats utils`() {
        timeStatsSqlUtils.insert(
                site,
                YEAR_POST_AND_PAGE_VIEWS_RESPONSE, YEARS, DATE
        )

        verify(statsSqlUtils).insert(site, POSTS_AND_PAGES_VIEWS, YEAR, YEAR_POST_AND_PAGE_VIEWS_RESPONSE,
                true, DATE_VALUE)
    }
}
