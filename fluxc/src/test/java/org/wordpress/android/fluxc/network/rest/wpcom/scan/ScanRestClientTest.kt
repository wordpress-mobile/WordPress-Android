package org.wordpress.android.fluxc.network.rest.wpcom.scan

import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.Credentials
import org.wordpress.android.fluxc.model.scan.ScanStateModel.Reason
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.FixThreatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.FixThreatsStatusResponse
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import org.wordpress.android.fluxc.store.ScanStore.FetchedScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsErrorType
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsStatusErrorType
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatErrorType
import org.wordpress.android.fluxc.store.ScanStore.ScanStartErrorType
import org.wordpress.android.fluxc.store.ScanStore.ScanStateErrorType
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class ScanRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var threatMapper: ThreatMapper

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var scanRestClient: ScanRestClient
    private val siteId: Long = 12
    private val threatId: Long = 1

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        scanRestClient = ScanRestClient(
                wpComGsonRequestBuilder,
                threatMapper,
                dispatcher,
                null,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `fetch scan state builds correct request url`() = test {
        val successResponseJson =
                UnitTestUtils.getStringFromResourceFile(javaClass, JP_SCAN_DAILY_SCAN_IDLE_WITH_THREATS_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        scanRestClient.fetchScanState(site)

        assertEquals(urlCaptor.firstValue, "$API_BASE_PATH/sites/${site.siteId}/scan/")
    }

    @Test
    fun `fetch scan state dispatches response on success`() = test {
        val successResponseJson =
                UnitTestUtils.getStringFromResourceFile(javaClass, JP_SCAN_DAILY_SCAN_IDLE_WITH_THREATS_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertEquals(site, this@ScanRestClientTest.site)
            assertNull(error)
            assertNotNull(scanStateModel)
            requireNotNull(scanStateModel).apply {
                assertEquals(state, State.fromValue(requireNotNull(scanResponse.state)))
                assertEquals(hasCloud, requireNotNull(scanResponse.hasCloud))
                assertEquals(hasValidCredentials, scanResponse.credentials?.firstOrNull()?.stillValid)
                assertEquals(reason, Reason.NO_REASON)
                assertNotNull(credentials)
                assertNotNull(threats)
                mostRecentStatus?.apply {
                    assertEquals(progress, scanResponse.mostRecentStatus?.progress)
                    assertEquals(startDate, scanResponse.mostRecentStatus?.startDate)
                    assertEquals(duration, scanResponse.mostRecentStatus?.duration)
                    assertEquals(error, scanResponse.mostRecentStatus?.error)
                    assertEquals(isInitial, scanResponse.mostRecentStatus?.isInitial)
                }
                currentStatus?.apply {
                    assertEquals(progress, scanResponse.mostRecentStatus?.progress)
                    assertEquals(startDate, scanResponse.mostRecentStatus?.startDate)
                    assertEquals(isInitial, scanResponse.mostRecentStatus?.isInitial)
                }
                credentials?.forEachIndexed { index, creds ->
                    creds.apply {
                        assertEquals(type, scanResponse.credentials?.get(index)?.type)
                        assertEquals(role, scanResponse.credentials?.get(index)?.role)
                        assertEquals(host, scanResponse.credentials?.get(index)?.host)
                        assertEquals(port, scanResponse.credentials?.get(index)?.port)
                        assertEquals(user, scanResponse.credentials?.get(index)?.user)
                        assertEquals(path, scanResponse.credentials?.get(index)?.path)
                        assertEquals(stillValid, scanResponse.credentials?.get(index)?.stillValid)
                    }
                }
                assertEquals(threats?.size, scanResponse.threats?.size)
            }
        }
    }

    @Test
    fun `fetch scan state dispatches most recent status for idle state`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(javaClass, JP_COMPLETE_SCAN_IDLE_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNotNull(scanStateModel?.mostRecentStatus)
            assertEquals(scanStateModel?.state, State.IDLE)
        }
    }

    @Test
    fun `fetch scan state dispatches empty creds when server creds not setup for site with scan capability`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
                javaClass,
                JP_SCAN_DAILY_SCAN_IDLE_WITH_THREAT_WITHOUT_SERVER_CREDS_JSON
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertEquals(scanStateModel?.credentials, emptyList<Credentials>())
            assertEquals(scanStateModel?.state, State.IDLE)
        }
    }

    @Test
    fun `fetch scan state dispatches empty threats if no threats found for site with scan capability`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(javaClass, JP_COMPLETE_SCAN_IDLE_JSON)

        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertEquals(scanStateModel?.threats, emptyList<Threat>())
            assertEquals(scanStateModel?.state, State.IDLE)
        }
    }

    @Test
    fun `fetch scan state dispatches current progress status for scanning state`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(javaClass, JP_COMPLETE_SCAN_SCANNING_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNotNull(scanStateModel?.currentStatus)
            assertEquals(scanStateModel?.state, State.SCANNING)
        }
    }

    @Test
    fun `fetch scan state dispatches reason, null threats and creds for scan unavailable state`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
                javaClass,
                JP_BACKUP_DAILY_SCAN_UNAVAILABLE_JSON
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNull(scanStateModel?.credentials)
            assertNull(scanStateModel?.threats)
            assertNotNull(scanStateModel?.reason)
            assertEquals(scanStateModel?.state, State.UNAVAILABLE)
        }
    }

    @Test
    fun `fetch scan state dispatches generic error on failure`() = test {
        initFetchScanState(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = scanRestClient.fetchScanState(site)

        assertEmittedScanStateError(payload, ScanStateErrorType.GENERIC_ERROR)
    }

    @Test
    fun `fetch scan state dispatches error on wrong state`() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(javaClass, JP_COMPLETE_SCAN_IDLE_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse.copy(state = "wrong"))

        val payload = scanRestClient.fetchScanState(site)

        assertEmittedScanStateError(payload, ScanStateErrorType.INVALID_RESPONSE)
    }

    @Test
    fun `fetch scan state dispatches error on missing threat id`() = test {
        val successResponseJson =
                UnitTestUtils.getStringFromResourceFile(javaClass, JP_SCAN_DAILY_SCAN_IDLE_WITH_THREATS_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        val threatWithIdNotSet = requireNotNull(scanResponse.threats?.get(0)).copy(id = null)
        initFetchScanState(scanResponse.copy(threats = listOf(threatWithIdNotSet)))

        val payload = scanRestClient.fetchScanState(site)

        assertEmittedScanStateError(payload, ScanStateErrorType.INVALID_RESPONSE)
    }

    @Test
    fun `fetch scan state dispatches error on missing threat signature`() = test {
        val successResponseJson =
                UnitTestUtils.getStringFromResourceFile(javaClass, JP_SCAN_DAILY_SCAN_IDLE_WITH_THREATS_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        val threatWithSignatureNotSet = requireNotNull(scanResponse.threats?.get(0)).copy(signature = null)
        initFetchScanState(scanResponse.copy(threats = listOf(threatWithSignatureNotSet)))

        val payload = scanRestClient.fetchScanState(site)

        assertEmittedScanStateError(payload, ScanStateErrorType.INVALID_RESPONSE)
    }

    @Test
    fun `fetch scan state dispatches error on missing threat first detected`() = test {
        val successResponseJson =
                UnitTestUtils.getStringFromResourceFile(javaClass, JP_SCAN_DAILY_SCAN_IDLE_WITH_THREATS_JSON)
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        val threatWithFirstDetectedNotSet = requireNotNull(scanResponse.threats?.get(0)).copy(firstDetected = null)
        initFetchScanState(scanResponse.copy(threats = listOf(threatWithFirstDetectedNotSet)))

        val payload = scanRestClient.fetchScanState(site)

        assertEmittedScanStateError(payload, ScanStateErrorType.INVALID_RESPONSE)
    }

    @Test
    fun `start scan builds correct request url`() = test {
        val scanStartResponse = ScanStartResponse(success = true)
        initStartScan(scanStartResponse)

        scanRestClient.startScan(site)

        assertEquals(urlCaptor.firstValue, "$API_BASE_PATH/sites/${site.siteId}/scan/enqueue/")
    }

    @Test
    fun `start scan dispatches response on success`() = test {
        val scanStartResponse = ScanStartResponse(success = true)
        initStartScan(scanStartResponse)

        val payload = scanRestClient.startScan(site)

        with(payload) {
            assertEquals(site, this@ScanRestClientTest.site)
            assertNull(error)
        }
    }

    @Test
    fun `start scan dispatches api error on failure from api`() = test {
        val errorResponseJson =
                UnitTestUtils.getStringFromResourceFile(javaClass, JP_BACKUP_DAILY_START_SCAN_ERROR_JSON)
        val startScanResponse = getStartScanResponseFromJsonString(errorResponseJson)
        initStartScan(startScanResponse)

        val payload = scanRestClient.startScan(site)

        with(payload) {
            assertEquals(site, this@ScanRestClientTest.site)
            assertTrue(isError)
            assertEquals(ScanStartErrorType.API_ERROR, error.type)
        }
    }

    @Test
    fun `fix threats dispatches response on success`() = test {
        val fixThreatsResponse = FixThreatsResponse(ok = true)
        initFixThreats(fixThreatsResponse)

        val payload = scanRestClient.fixThreats(siteId, listOf(threatId))

        with(payload) {
            assertEquals(remoteSiteId, siteId)
            assertNull(error)
        }
    }

    @Test
    fun `fix threats dispatches api error on failure from api`() = test {
        val fixThreatsResponse = FixThreatsResponse(ok = false)
        initFixThreats(fixThreatsResponse)

        val payload = scanRestClient.fixThreats(siteId, listOf(threatId))

        with(payload) {
            assertEquals(siteId, this@ScanRestClientTest.siteId)
            assertTrue(isError)
            assertEquals(FixThreatsErrorType.API_ERROR, error.type)
        }
    }

    @Test
    fun `ignore threat dispatches response on success`() = test {
        initIgnoreThreat()

        val payload = scanRestClient.ignoreThreat(siteId, threatId)

        with(payload) {
            assertEquals(remoteSiteId, siteId)
            assertNull(error)
        }
    }

    @Test
    fun `ignore threats dispatches generic error on failure`() = test {
        initIgnoreThreat(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = scanRestClient.ignoreThreat(siteId, threatId)

        with(payload) {
            assertEquals(remoteSiteId, siteId)
            assertTrue(isError)
            assertEquals(IgnoreThreatErrorType.GENERIC_ERROR, error.type)
        }
    }

    @Test
    fun `fetch fix threats status dispatches response on success`() = test {
        initFetchFixThreatsStatus(FixThreatsStatusResponse(ok = true, fixThreatsStatus = listOf()))

        val payload = scanRestClient.fetchFixThreatsStatus(siteId, listOf(threatId))

        with(payload) {
            assertEquals(remoteSiteId, siteId)
            assertNull(error)
        }
    }

    @Test
    fun `fetch fix threats status dispatches api error on failure from api`() = test {
        initFetchFixThreatsStatus(FixThreatsStatusResponse(ok = false, fixThreatsStatus = null))

        val payload = scanRestClient.fetchFixThreatsStatus(siteId, listOf(threatId))

        with(payload) {
            assertEquals(remoteSiteId, siteId)
            assertTrue(isError)
            assertEquals(FixThreatsStatusErrorType.API_ERROR, error.type)
        }
    }

    private fun assertEmittedScanStateError(payload: FetchedScanStatePayload, errorType: ScanStateErrorType) {
        with(payload) {
            assertEquals(site, this@ScanRestClientTest.site)
            assertTrue(isError)
            assertEquals(errorType, error.type)
        }
    }

    private fun getScanStateResponseFromJsonString(json: String): ScanStateResponse {
        val responseType = object : TypeToken<ScanStateResponse>() {}.type
        return Gson().fromJson(json, responseType) as ScanStateResponse
    }

    private fun getStartScanResponseFromJsonString(json: String): ScanStartResponse {
        val responseType = object : TypeToken<ScanStartResponse>() {}.type
        return Gson().fromJson(json, responseType) as ScanStartResponse
    }

    private suspend fun initFetchScanState(
        data: ScanStateResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<ScanStateResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Success(nonNullData)
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(scanRestClient),
                        urlCaptor.capture(),
                        eq(mapOf()),
                        eq(ScanStateResponse::class.java),
                        eq(false),
                        any(),
                        eq(false)
                )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)

        val threatModel = mock<ThreatModel>()
        whenever(threatMapper.map(any())).thenReturn(threatModel)

        return response
    }

    private suspend fun initStartScan(
        data: ScanStartResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<ScanStartResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Success(nonNullData)
        whenever(
                wpComGsonRequestBuilder.syncPostRequest(
                        eq(scanRestClient),
                        urlCaptor.capture(),
                        eq(mapOf()),
                        anyOrNull(),
                        eq(ScanStartResponse::class.java),
                        isNull()
                )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private suspend fun initFixThreats(
        data: FixThreatsResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<FixThreatsResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Success(nonNullData)
        whenever(
                wpComGsonRequestBuilder.syncPostRequest(
                        eq(scanRestClient),
                        urlCaptor.capture(),
                        anyOrNull(),
                        anyOrNull(),
                        eq(FixThreatsResponse::class.java),
                        isNull()
                )
        ).thenReturn(response)
        return response
    }

    private suspend fun initIgnoreThreat(
        error: WPComGsonNetworkError? = null
    ): Response<Any> {
        val response = if (error != null) Response.Error(error) else Success(Any())
        whenever(
                wpComGsonRequestBuilder.syncPostRequest(
                        eq(scanRestClient),
                        urlCaptor.capture(),
                        anyOrNull(),
                        anyOrNull(),
                        eq(Any::class.java),
                        isNull()
                )
        ).thenReturn(response)
        return response
    }

    private suspend fun initFetchFixThreatsStatus(
        data: FixThreatsStatusResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<FixThreatsStatusResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Success(nonNullData)
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(scanRestClient),
                        urlCaptor.capture(),
                        anyOrNull(),
                        eq(FixThreatsStatusResponse::class.java),
                        eq(false),
                        any(),
                        eq(false)
                )
        ).thenReturn(response)
        return response
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val JP_COMPLETE_SCAN_IDLE_JSON = "wp/jetpack/scan/jp-complete-scan-idle.json"
        private const val JP_COMPLETE_SCAN_SCANNING_JSON = "wp/jetpack/scan/jp-complete-scan-scanning.json"
        private const val JP_BACKUP_DAILY_SCAN_UNAVAILABLE_JSON =
                "wp/jetpack/scan/jp-backup-daily-scan-unavailable.json"
        private const val JP_SCAN_DAILY_SCAN_IDLE_WITH_THREATS_JSON =
                "wp/jetpack/scan/jp-scan-daily-scan-idle-with-threat.json"
        private const val JP_SCAN_DAILY_SCAN_IDLE_WITH_THREAT_WITHOUT_SERVER_CREDS_JSON =
                "wp/jetpack/scan/jp-scan-daily-scan-idle-with-threat-without-server-creds.json"
        private const val JP_BACKUP_DAILY_START_SCAN_ERROR_JSON =
                "wp/jetpack/scan/jp-backup-daily-start-scan-error.json"
    }
}
