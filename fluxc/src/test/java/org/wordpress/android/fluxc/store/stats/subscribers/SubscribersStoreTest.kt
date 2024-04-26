package org.wordpress.android.fluxc.store.stats.subscribers

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersMapper
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.SubscribersRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.SubscribersRestClient.SubscribersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.SubscribersSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val QUANTITY = 8
private val LIMIT_MODE = LimitMode.Top(QUANTITY)
private const val FORMATTED_DATE = "2024-04-22"

@RunWith(MockitoJUnitRunner::class)
class SubscribersStoreTest {
    @Mock
    lateinit var site: SiteModel

    @Mock
    lateinit var restClient: SubscribersRestClient

    @Mock
    lateinit var sqlUtils: SubscribersSqlUtils

    @Mock
    lateinit var statsUtils: StatsUtils

    @Mock
    lateinit var currentTimeProvider: CurrentTimeProvider

    @Mock
    lateinit var mapper: SubscribersMapper

    @Mock
    lateinit var appLogWrapper: AppLogWrapper
    private lateinit var store: SubscribersStore

    @Before
    fun setUp() {
        store = SubscribersStore(
            restClient,
            sqlUtils,
            mapper,
            statsUtils,
            currentTimeProvider,
            initCoroutineEngine(),
            appLogWrapper
        )
        val currentDate = Date(0)
        whenever(currentTimeProvider.currentDate()).thenReturn(currentDate)
        val timeZone = "GMT"
        whenever(site.timezone).thenReturn(timeZone)
        whenever(statsUtils.getFormattedDate(eq(currentDate), any())).thenReturn(FORMATTED_DATE)
    }

    @Test
    fun `returns data per site`() = test {
        val fetchSubscribersPayload = FetchStatsPayload(SUBSCRIBERS_RESPONSE)
        val forced = true
        whenever(restClient.fetchSubscribers(site, DAYS, QUANTITY, FORMATTED_DATE, forced))
            .thenReturn(fetchSubscribersPayload)
        whenever(mapper.map(SUBSCRIBERS_RESPONSE, LIMIT_MODE)).thenReturn(SUBSCRIBERS_MODEL)

        val responseModel = store.fetchSubscribers(site, DAYS, LIMIT_MODE, forced)

        Assertions.assertThat(responseModel.model).isEqualTo(SUBSCRIBERS_MODEL)
        verify(sqlUtils).insert(site, SUBSCRIBERS_RESPONSE, DAYS, FORMATTED_DATE, QUANTITY)
    }

    @Test
    fun `returns cached data per site`() = test {
        whenever(sqlUtils.hasFreshRequest(site, DAYS, FORMATTED_DATE, QUANTITY)).thenReturn(true)
        whenever(sqlUtils.select(site, DAYS, FORMATTED_DATE)).thenReturn(SUBSCRIBERS_RESPONSE)
        val model = mock<SubscribersModel>()
        whenever(mapper.map(SUBSCRIBERS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val forced = false
        val responseModel = store.fetchSubscribers(site, DAYS, LIMIT_MODE, forced)

        Assertions.assertThat(responseModel.model).isEqualTo(model)
        Assertions.assertThat(responseModel.cached).isTrue()
        verify(sqlUtils, never()).insert(any(), any(), any(), any<String>(), isNull())
    }

    @Test
    fun `returns error when invalid data`() = test {
        val forced = true
        val fetchInsightsPayload = FetchStatsPayload(SUBSCRIBERS_RESPONSE)
        whenever(restClient.fetchSubscribers(site, DAYS, QUANTITY, FORMATTED_DATE, forced))
            .thenReturn(fetchInsightsPayload)
        val emptyModel = SubscribersModel("", emptyList())
        whenever(mapper.map(SUBSCRIBERS_RESPONSE, LIMIT_MODE)).thenReturn(emptyModel)

        val responseModel = store.fetchSubscribers(site, DAYS, LIMIT_MODE, forced)

        Assertions.assertThat(responseModel.error.type).isEqualTo(INVALID_DATA_ERROR.type)
        Assertions.assertThat(responseModel.error.message).isEqualTo(INVALID_DATA_ERROR.message)
    }

    @Test
    fun `returns error when data call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<SubscribersResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchSubscribers(site, DAYS, QUANTITY, FORMATTED_DATE, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchSubscribers(site, DAYS, LIMIT_MODE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns data from db`() {
        whenever(sqlUtils.select(site, DAYS, FORMATTED_DATE)).thenReturn(SUBSCRIBERS_RESPONSE)
        val model = mock<SubscribersModel>()
        whenever(mapper.map(SUBSCRIBERS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val result = store.getSubscribers(site, DAYS, LIMIT_MODE)

        Assertions.assertThat(result).isEqualTo(model)
    }
}
