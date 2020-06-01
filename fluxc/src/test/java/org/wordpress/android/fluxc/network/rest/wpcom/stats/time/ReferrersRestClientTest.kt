package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse.Group
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ReferrersRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var statsUtils: StatsUtils
    private val gson: Gson = GsonBuilder().create()
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: ReferrersRestClient
    private val siteId: Long = 12
    private val pageSize = 5
    private val currentStringDate = "2018-10-10"
    private val currentDate = Date(0)

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = ReferrersRestClient(
                dispatcher,
                wpComGsonRequestBuilder,
                null,
                requestQueue,
                accessToken,
                userAgent,
                gson,
                statsUtils
        )
        whenever(statsUtils.getFormattedDate(eq(currentDate), isNull())).thenReturn(currentStringDate)
    }

    @Test
    fun `returns post & page day views success response`() = test {
        testSuccessResponse(DAYS)
    }

    @Test
    fun `returns post & page day views error response`() = test {
        testErrorResponse(DAYS)
    }

    @Test
    fun `returns post & page week views success response`() = test {
        testSuccessResponse(WEEKS)
    }

    @Test
    fun `returns post & page week views error response`() = test {
        testErrorResponse(WEEKS)
    }

    @Test
    fun `returns post & page month views success response`() = test {
        testSuccessResponse(MONTHS)
    }

    @Test
    fun `returns post & page month views error response`() = test {
        testErrorResponse(MONTHS)
    }

    @Test
    fun `returns post & page year views success response`() = test {
        testSuccessResponse(YEARS)
    }

    @Test
    fun `returns post & page year views error response`() = test {
        testErrorResponse(YEARS)
    }

    @Test
    fun `maps group with views`() {
        val group = "{\"group\":\"WordPress.com Reader\"," +
                "\"name\":\"WordPress.com Reader\"," +
                "\"url\":\"https:\\/\\/wordpress.com\\/\"," +
                "\"icon\":\"https:\\/\\/secure.gravatar.com\\/blavatar\\/236c008da9dc0edb4b3464ecebb3fc1d?s=48\"," +
                "\"total\":16," +
                "\"follow_data\":null," +
                "\"results\":" +
                "{\"views\":16}" +
                "}"
        val parsedGroup = gson.fromJson<Group>(group, Group::class.java)

        parsedGroup.build(gson)

        assertThat(parsedGroup.views).isEqualTo(16)
        assertThat(parsedGroup.referrers).isNull()
    }

    @Test
    fun `maps group with referrers`() {
        val groupId = "group1"
        val groupName = "Group Name"
        val groupViews = 96
        val firstReferrerName = "referrer1"
        val secondReferrerName = "referrer2"
        val firstUrl = "url1.com"
        val secondUrl = "url2.com"
        val firstViews = 91
        val secondViews = 5
        val group = "{\"group\":\"$groupId\",\n" +
                "\"name\":\"$groupName\",\n" +
                "\"icon\":null,\"total\":$groupViews,\"follow_data\":null," +
                "\"results\":" +
                "[{\"name\":\"$firstReferrerName\",\"url\":\"$firstUrl\",\"views\":$firstViews}," +
                "{\"name\":\"$secondReferrerName\",\"url\":\"$secondUrl\",\"views\":$secondViews}]}"
        val parsedGroup = gson.fromJson<Group>(group, Group::class.java)

        parsedGroup.build(gson)

        assertThat(parsedGroup.groupId).isEqualTo(groupId)
        assertThat(parsedGroup.views).isNull()
        assertThat(parsedGroup.referrers).hasSize(2)
        parsedGroup.referrers?.get(0)?.apply {
            assertThat(this.name).isEqualTo(firstReferrerName)
            assertThat(this.url).isEqualTo(firstUrl)
            assertThat(this.views).isEqualTo(firstViews)
            assertThat(this.icon).isNull()
        }
        parsedGroup.referrers?.get(1)?.apply {
            assertThat(this.name).isEqualTo(secondReferrerName)
            assertThat(this.url).isEqualTo(secondUrl)
            assertThat(this.views).isEqualTo(secondViews)
            assertThat(this.icon).isNull()
        }
    }

    private suspend fun testSuccessResponse(granularity: StatsGranularity) {
        val response = mock<ReferrersResponse>()
        initReferrersResponse(response)

        val responseModel = restClient.fetchReferrers(site, granularity, currentDate, pageSize, false)

        assertThat(responseModel.response).isNotNull()
        assertThat(responseModel.response).isEqualTo(response)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/12/stats/referrers/")
        assertThat(paramsCaptor.lastValue).isEqualTo(
                mapOf(
                        "max" to pageSize.toString(),
                        "period" to granularity.toString(),
                        "date" to currentStringDate
                )
        )
    }

    private suspend fun testErrorResponse(granularity: StatsGranularity) {
        val errorMessage = "message"
        initReferrersResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )

        val responseModel = restClient.fetchReferrers(site, granularity, currentDate, pageSize, false)

        assertThat(responseModel.error).isNotNull()
        assertThat(responseModel.error.type).isEqualTo(API_ERROR)
        assertThat(responseModel.error.message).isEqualTo(errorMessage)
    }

    private suspend fun initReferrersResponse(
        data: ReferrersResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<ReferrersResponse> {
        return initResponse(ReferrersResponse::class.java, data ?: mock(), error)
    }

    private suspend fun <T> initResponse(
        kclass: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null,
        cachingEnabled: Boolean = false
    ): Response<T> {
        val response = if (error != null) Response.Error<T>(error) else Success(data)
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(kclass),
                        eq(cachingEnabled),
                        any(),
                        eq(false)
                )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }
}
