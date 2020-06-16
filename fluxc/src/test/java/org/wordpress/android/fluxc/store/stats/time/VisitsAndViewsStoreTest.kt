package org.wordpress.android.fluxc.store.stats.time

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VisitAndViewsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VisitAndViewsRestClient.VisitsAndViewsResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.VisitsAndViewsSqlUtils
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

private const val ITEMS_TO_LOAD = 8
private val LIMIT_MODE = LimitMode.Top(ITEMS_TO_LOAD)
private const val FORMATTED_DATE = "2019-10-10"

@RunWith(MockitoJUnitRunner::class)
class VisitsAndViewsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: VisitAndViewsRestClient
    @Mock lateinit var sqlUtils: VisitsAndViewsSqlUtils
    @Mock lateinit var statsUtils: StatsUtils
    @Mock lateinit var currentTimeProvider: CurrentTimeProvider
    @Mock lateinit var mapper: TimeStatsMapper
    @Mock lateinit var appLogWrapper: AppLogWrapper
    private lateinit var store: VisitsAndViewsStore
    @Before
    fun setUp() {
        store = VisitsAndViewsStore(
                restClient,
                sqlUtils,
                mapper,
                statsUtils,
                currentTimeProvider,
                initCoroutineEngine(),
                appLogWrapper
        )
        val currentDate = Date(0)
        whenever(currentTimeProvider.currentDate).thenReturn(currentDate)
        val timeZone = "GMT"
        whenever(site.timezone).thenReturn(timeZone)
        whenever(
                statsUtils.getFormattedDate(
                        eq(currentDate),
                        any()
                )
        ).thenReturn(FORMATTED_DATE)
    }

    @Test
    fun `returns data per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                VISITS_AND_VIEWS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchVisits(site, DAYS, FORMATTED_DATE, ITEMS_TO_LOAD, forced)).thenReturn(
                fetchInsightsPayload
        )
        whenever(mapper.map(VISITS_AND_VIEWS_RESPONSE, LIMIT_MODE)).thenReturn(VISITS_AND_VIEWS_MODEL)

        val responseModel = store.fetchVisits(site, DAYS, LIMIT_MODE, forced)

        assertThat(responseModel.model).isEqualTo(VISITS_AND_VIEWS_MODEL)
        verify(sqlUtils).insert(site, VISITS_AND_VIEWS_RESPONSE, DAYS, FORMATTED_DATE, ITEMS_TO_LOAD)
    }

    @Test
    fun `returns cached data per site`() = test {
        whenever(sqlUtils.hasFreshRequest(site, DAYS, FORMATTED_DATE, ITEMS_TO_LOAD)).thenReturn(true)
        whenever(sqlUtils.select(site, DAYS, FORMATTED_DATE)).thenReturn(VISITS_AND_VIEWS_RESPONSE)
        val model = mock<VisitsAndViewsModel>()
        whenever(mapper.map(VISITS_AND_VIEWS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val forced = false
        val responseModel = store.fetchVisits(site, DAYS, LIMIT_MODE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        assertThat(responseModel.cached).isTrue()
        verify(sqlUtils, never()).insert(any(), any(), any(), any<String>(), isNull())
    }

    @Test
    fun `returns error when invalid data`() = test {
        val forced = true
        val fetchInsightsPayload = FetchStatsPayload(
                VISITS_AND_VIEWS_RESPONSE
        )
        whenever(restClient.fetchVisits(site, DAYS, FORMATTED_DATE, ITEMS_TO_LOAD, forced)).thenReturn(
                fetchInsightsPayload
        )
        val emptyModel = VisitsAndViewsModel("", emptyList())
        whenever(mapper.map(VISITS_AND_VIEWS_RESPONSE, LIMIT_MODE)).thenReturn(emptyModel)

        val responseModel = store.fetchVisits(site, DAYS, LIMIT_MODE, forced)

        assertThat(responseModel.error.type).isEqualTo(INVALID_DATA_ERROR.type)
        assertThat(responseModel.error.message).isEqualTo(INVALID_DATA_ERROR.message)
    }

    @Test
    fun `returns error when data call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<VisitsAndViewsResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchVisits(site, DAYS, FORMATTED_DATE, ITEMS_TO_LOAD, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchVisits(site, DAYS, LIMIT_MODE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns data from db`() {
        whenever(sqlUtils.select(site, DAYS, FORMATTED_DATE)).thenReturn(VISITS_AND_VIEWS_RESPONSE)
        val model = mock<VisitsAndViewsModel>()
        whenever(mapper.map(VISITS_AND_VIEWS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val result = store.getVisits(site, DAYS, LIMIT_MODE)

        assertThat(result).isEqualTo(model)
    }
}
