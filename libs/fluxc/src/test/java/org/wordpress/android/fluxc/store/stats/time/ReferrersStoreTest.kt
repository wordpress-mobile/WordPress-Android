package org.wordpress.android.fluxc.store.stats.time

import com.nhaarman.mockitokotlin2.any
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
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.referrers.ReferrersRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.referrers.ReferrersRestClient.FetchReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.referrers.ReportReferrerAsSpamResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.ReferrersSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.ReportReferrerAsSpamPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val ITEMS_TO_LOAD = 8
private val DATE = Date(0)
private val LIMIT_MODE = LimitMode.Top(ITEMS_TO_LOAD)

@RunWith(MockitoJUnitRunner::class)
class ReferrersStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: ReferrersRestClient
    @Mock lateinit var sqlUtils: ReferrersSqlUtils
    @Mock lateinit var mapper: TimeStatsMapper
    private lateinit var store: ReferrersStore
    private val domain: String = "example.referral.com"
    @Before
    fun setUp() {
        store = ReferrersStore(
                restClient,
                sqlUtils,
                mapper,
                initCoroutineEngine()
        )
    }

    @Test
    fun `returns referrers per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                REFERRERS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchReferrers(site, DAYS, DATE, ITEMS_TO_LOAD + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, LimitMode.Top(ITEMS_TO_LOAD))).thenReturn(model)

        val responseModel = store.fetchReferrers(site, DAYS, LimitMode.Top(ITEMS_TO_LOAD), DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, REFERRERS_RESPONSE, DAYS, DATE, ITEMS_TO_LOAD)
    }

    @Test
    fun `returns cached data per site`() = test {
        whenever(sqlUtils.hasFreshRequest(site, DAYS, DATE, ITEMS_TO_LOAD)).thenReturn(true)
        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(REFERRERS_RESPONSE)
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, LimitMode.Top(ITEMS_TO_LOAD))).thenReturn(model)

        val forced = false
        val responseModel = store.fetchReferrers(site, DAYS, LimitMode.Top(ITEMS_TO_LOAD), DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        assertThat(responseModel.cached).isTrue()
        verify(sqlUtils, never()).insert(any(), any(), any(), any<Date>(), isNull())
    }

    @Test
    fun `returns error when referrers call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<FetchReferrersResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchReferrers(site, DAYS, DATE, ITEMS_TO_LOAD + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchReferrers(site, DAYS, LimitMode.Top(ITEMS_TO_LOAD), DATE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns referrers from db`() {
        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(REFERRERS_RESPONSE)
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, LimitMode.Top(ITEMS_TO_LOAD))).thenReturn(model)

        val result = store.getReferrers(site, DAYS, LimitMode.Top(ITEMS_TO_LOAD), DATE)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns successful when report referrer as spam`() = test {
        val date = Date()
        val restResponse = ReportReferrerAsSpamPayload(ReportReferrerAsSpamResponse(true))
        whenever(restClient.reportReferrerAsSpam(site, domain)).thenReturn(restResponse)
        whenever(sqlUtils.select(site, YEARS, date)).thenReturn(REFERRERS_RESPONSE)

        val result = store.reportReferrerAsSpam(
                site,
                domain,
                YEARS,
                LIMIT_MODE,
                date
        )

        assertThat(result.model?.success).isEqualTo(true)
    }

    @Test
    fun `report referrer as spam doesnt mark spam when cache fails`() = test {
        val date = Date()
        val restResponse = ReportReferrerAsSpamPayload(ReportReferrerAsSpamResponse(true))
        whenever(restClient.reportReferrerAsSpam(site, domain)).thenReturn(restResponse)
        whenever(sqlUtils.select(site, YEARS, date)).thenReturn(null)

        val result = store.reportReferrerAsSpam(
                site,
                domain,
                YEARS,
                LIMIT_MODE,
                date
        )

        assertThat(result.model?.success).isEqualTo(true)
    }

    @Test
    fun `returns error when report referrer as spam causes network error`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = ReportReferrerAsSpamPayload<ReportReferrerAsSpamResponse>(StatsError(type, message))
        whenever(restClient.reportReferrerAsSpam(site, domain)).thenReturn(errorPayload)

        val result = store.reportReferrerAsSpam(
                site,
                domain,
                YEARS,
                LIMIT_MODE,
                Date()
        )

        assertNotNull(result.error)
        val error = result.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns successful when unreport referrer as spam`() = test {
        val restResponse = ReportReferrerAsSpamPayload(ReportReferrerAsSpamResponse(true))
        whenever(restClient.unreportReferrerAsSpam(site, domain)).thenReturn(restResponse)

        val result = store.unreportReferrerAsSpam(
                site,
                domain,
                YEARS,
                LIMIT_MODE,
                Date()
        )

        assertThat(result.model?.success).isEqualTo(true)
    }

    @Test
    fun `returns error when unreport referrer as spam causes network error`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = ReportReferrerAsSpamPayload<ReportReferrerAsSpamResponse>(StatsError(type, message))
        whenever(restClient.unreportReferrerAsSpam(site, domain)).thenReturn(errorPayload)

        val result = store.unreportReferrerAsSpam(
                site,
                domain,
                YEARS,
                LIMIT_MODE,
                Date()
        )

        assertNotNull(result.error)
        val error = result.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `set spam to true`() = test {
        val groupResult = store.setSelectForSpam(REFERRERS_RESPONSE, "url_group_2.com", true)

        // Asserting group 1 is set with spam as false and group 2 is set with spam as true
        assertFalse(groupResult.groups.entries.toTypedArray()[0].value.groups[0].spam!!)
        assertTrue(groupResult.groups.entries.toTypedArray()[0].value.groups[1].spam!!)

        val referrerResult = store.setSelectForSpam(REFERRERS_RESPONSE, "john.com", true)
        assertTrue(
                ((referrerResult.groups.entries.toTypedArray()[0].value.groups[0].referrers as List<*>)
                [0] as FetchReferrersResponse.Referrer).spam!!)

        val childResult = store.setSelectForSpam(REFERRERS_RESPONSE, "child.com", true)
        assertTrue(
                ((childResult.groups.entries.toTypedArray()[0].value.groups[0].referrers as List<*>)
                        [0] as FetchReferrersResponse.Referrer).children?.get(0)?.spam!!)
    }

    @Test
    fun `set spam to false`() = test {
        val groupResultWithSpam = store.setSelectForSpam(REFERRERS_RESPONSE, "url_group_2.com", true)
        val groupResult = store.setSelectForSpam(groupResultWithSpam, "url_group_2.com", false)

        // Asserting group 1 and group 2 is set with spam to false
        assertFalse(groupResult.groups.entries.toTypedArray()[0].value.groups[0].spam!!)
        assertFalse(groupResult.groups.entries.toTypedArray()[0].value.groups[1].spam!!)

        val referrerResultWitSpam = store.setSelectForSpam(REFERRERS_RESPONSE, "john.com", true)
        val referrerResult = store.setSelectForSpam(referrerResultWitSpam, "john.com", false)
        assertFalse(
                ((referrerResult.groups.entries.toTypedArray()[0].value.groups[0].referrers as List<*>)
                [0] as FetchReferrersResponse.Referrer).spam!!)

        val childResultWithSpam = store.setSelectForSpam(REFERRERS_RESPONSE, "child.com", true)
        val childResult = store.setSelectForSpam(childResultWithSpam, "child.com", false)
        assertFalse(
                ((childResult.groups.entries.toTypedArray()[0].value.groups[0].referrers as List<*>)
                        [0] as FetchReferrersResponse.Referrer).children?.get(0)?.spam!!)
    }
}
