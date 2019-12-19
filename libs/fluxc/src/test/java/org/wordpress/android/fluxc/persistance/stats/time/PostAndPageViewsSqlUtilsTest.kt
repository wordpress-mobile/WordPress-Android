package org.wordpress.android.fluxc.persistance.stats.time

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
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
import org.wordpress.android.fluxc.persistence.StatsRequestSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.POSTS_AND_PAGES_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.DAY
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.PostsAndPagesSqlUtils
import org.wordpress.android.fluxc.store.stats.time.POST_AND_PAGE_VIEWS_RESPONSE
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
        whenever(statsUtils.getFormattedDate(eq(DATE), isNull())).thenReturn(DATE_VALUE)
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
                        POST_AND_PAGE_VIEWS_RESPONSE
                )

        val result = timeStatsSqlUtils.select(site, DAYS, DATE)

        assertEquals(result, POST_AND_PAGE_VIEWS_RESPONSE)
    }

    @Test
    fun `inserts post and page day views to stats utils`() {
        timeStatsSqlUtils.insert(
                site,
                POST_AND_PAGE_VIEWS_RESPONSE, DAYS, DATE
        )

        verify(statsSqlUtils).insert(site, POSTS_AND_PAGES_VIEWS, DAY, POST_AND_PAGE_VIEWS_RESPONSE,
                true, DATE_VALUE)
    }
}
