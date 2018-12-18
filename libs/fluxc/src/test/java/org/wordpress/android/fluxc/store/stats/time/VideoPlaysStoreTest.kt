package org.wordpress.android.fluxc.store.stats.time

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.model.stats.time.VideoPlaysModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VideoPlaysRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VideoPlaysRestClient.VideoPlaysResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val PAGE_SIZE = 8
private val DATE = Date(0)

@RunWith(MockitoJUnitRunner::class)
class VideoPlaysStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: VideoPlaysRestClient
    @Mock lateinit var sqlUtils: TimeStatsSqlUtils
    @Mock lateinit var mapper: TimeStatsMapper
    private lateinit var store: VideoPlaysStore
    @Before
    fun setUp() {
        store = VideoPlaysStore(
                restClient,
                sqlUtils,
                mapper,
                Unconfined
        )
    }

    @Test
    fun `returns video plays per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                VIDEO_PLAYS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchVideoPlays(site, DAYS, DATE, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<VideoPlaysModel>()
        whenever(mapper.map(VIDEO_PLAYS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchVideoPlays(site, PAGE_SIZE, DAYS, DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, VIDEO_PLAYS_RESPONSE, DAYS, DATE)
    }

    @Test
    fun `returns error when video plays call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<VideoPlaysResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchVideoPlays(site, DAYS, DATE, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchVideoPlays(site, PAGE_SIZE, DAYS, DATE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns video plays from db`() {
        whenever(sqlUtils.selectVideoPlays(site, DAYS, DATE)).thenReturn(VIDEO_PLAYS_RESPONSE)
        val model = mock<VideoPlaysModel>()
        whenever(mapper.map(VIDEO_PLAYS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getVideoPlays(site, DAYS, PAGE_SIZE, DATE)

        assertThat(result).isEqualTo(model)
    }
}
