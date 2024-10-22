package org.wordpress.android.fluxc.store.stats.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.PostsAndPagesSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val ITEMS_TO_LOAD = 8
private val DATE = Date(0)

@RunWith(MockitoJUnitRunner::class)
class PostAndPageViewsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: PostAndPageViewsRestClient
    @Mock lateinit var sqlUtils: PostsAndPagesSqlUtils
    @Mock lateinit var mapper: TimeStatsMapper
    private lateinit var store: PostAndPageViewsStore
    @Before
    fun setUp() {
        store = PostAndPageViewsStore(
                restClient,
                sqlUtils,
                mapper,
                initCoroutineEngine()
        )
    }

    @Test
    fun `returns post and page views per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                POST_AND_PAGE_VIEWS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchPostAndPageViews(site, DAYS, DATE, ITEMS_TO_LOAD + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<PostAndPageViewsModel>()
        whenever(mapper.map(POST_AND_PAGE_VIEWS_RESPONSE, LimitMode.Top(ITEMS_TO_LOAD))).thenReturn(model)

        val responseModel = store.fetchPostAndPageViews(site, DAYS, LimitMode.Top(ITEMS_TO_LOAD), DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, POST_AND_PAGE_VIEWS_RESPONSE, DAYS, DATE, ITEMS_TO_LOAD)
    }

    @Test
    fun `returns cached data per site`() = test {
        whenever(sqlUtils.hasFreshRequest(site, DAYS, DATE, ITEMS_TO_LOAD)).thenReturn(true)
        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(POST_AND_PAGE_VIEWS_RESPONSE)
        val model = mock<PostAndPageViewsModel>()
        whenever(mapper.map(POST_AND_PAGE_VIEWS_RESPONSE, LimitMode.Top(ITEMS_TO_LOAD))).thenReturn(model)

        val forced = false
        val responseModel = store.fetchPostAndPageViews(site, DAYS, LimitMode.Top(ITEMS_TO_LOAD), DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        assertThat(responseModel.cached).isTrue()
        verify(sqlUtils, never()).insert(any(), any(), any(), any<Date>(), isNull())
    }

    @Test
    fun `returns error when post and page day views call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<PostAndPageViewsResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchPostAndPageViews(site, DAYS, DATE, ITEMS_TO_LOAD + 1, forced)).thenReturn(
                errorPayload
        )

        val responseModel = store.fetchPostAndPageViews(site, DAYS, LimitMode.Top(ITEMS_TO_LOAD), DATE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns post and page day views from db`() {
        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(POST_AND_PAGE_VIEWS_RESPONSE)
        val model = mock<PostAndPageViewsModel>()
        whenever(mapper.map(POST_AND_PAGE_VIEWS_RESPONSE, LimitMode.Top(ITEMS_TO_LOAD))).thenReturn(model)

        val result = store.getPostAndPageViews(site, DAYS, LimitMode.Top(ITEMS_TO_LOAD), DATE)

        assertThat(result).isEqualTo(model)
    }
}
