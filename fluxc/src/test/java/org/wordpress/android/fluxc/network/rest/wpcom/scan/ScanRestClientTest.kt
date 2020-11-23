package org.wordpress.android.fluxc.network.rest.wpcom.scan

import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.Credentials
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.scan.ScanStateResponse.Threat
import org.wordpress.android.fluxc.store.ScanStore.FetchedScanStatePayload
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

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var scanRestClient: ScanRestClient
    private val siteId: Long = 12

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        scanRestClient = ScanRestClient(
            wpComGsonRequestBuilder,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent
        )
    }

    @Test
    fun fetchScanState_buildsCorrectRequestUrl() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-daily-scan-with-threat.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        scanRestClient.fetchScanState(site)

        assertEquals(urlCaptor.firstValue, "https://public-api.wordpress.com/wpcom/v2/sites/${site.siteId}/scan/")
    }

    @Test
    fun fetchScanState_dispatchesResponseOnSuccess() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-daily-scan-with-threat.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertEquals(this.site, site)
            assertNull(this.error)
            assertNotNull(this.scanStateModel)
            requireNotNull(this.scanStateModel).apply {
                assertEquals(this.state, State.fromValue(requireNotNull(scanResponse?.state)))
                assertEquals(this.hasCloud, requireNotNull(scanResponse?.hasCloud))
                assertNull(this.reason)
                assertNotNull(this.credentials)
                assertNotNull(this.threats)
                this.mostRecentStatus?.apply {
                    assertEquals(this.progress, scanResponse?.mostRecentStatus?.progress)
                    assertEquals(this.startDate, scanResponse?.mostRecentStatus?.startDate)
                    assertEquals(this.duration, scanResponse?.mostRecentStatus?.duration)
                    assertEquals(this.error, scanResponse?.mostRecentStatus?.error)
                    assertEquals(this.isInitial, scanResponse?.mostRecentStatus?.isInitial)
                }
                this.currentStatus?.apply {
                    assertEquals(this.progress, scanResponse?.mostRecentStatus?.progress)
                    assertEquals(this.startDate, scanResponse?.mostRecentStatus?.startDate)
                    assertEquals(this.isInitial, scanResponse?.mostRecentStatus?.isInitial)
                }
                this.credentials?.forEachIndexed { index, creds ->
                    creds.apply {
                        assertEquals(this.type, scanResponse?.credentials?.get(index)?.type)
                        assertEquals(this.role, scanResponse?.credentials?.get(index)?.role)
                        assertEquals(this.host, scanResponse?.credentials?.get(index)?.host)
                        assertEquals(this.port, scanResponse?.credentials?.get(index)?.port)
                        assertEquals(this.user, scanResponse?.credentials?.get(index)?.user)
                        assertEquals(this.path, scanResponse?.credentials?.get(index)?.path)
                        assertEquals(this.stillValid, scanResponse?.credentials?.get(index)?.stillValid)
                    }
                }
                this.threats?.forEachIndexed { index, threat ->
                    threat.apply {
                        assertEquals(this.id, scanResponse?.threats?.get(index)?.id)
                        assertEquals(this.signature, scanResponse?.threats?.get(index)?.signature)
                        assertEquals(this.description, scanResponse?.threats?.get(index)?.description)
                        assertEquals(this.status, scanResponse?.threats?.get(index)?.status)
                        assertEquals(this.firstDetected, scanResponse?.threats?.get(index)?.firstDetected)
                        assertEquals(this.fixedOn, scanResponse?.threats?.get(index)?.fixedOn)
                        this.fixable?.apply {
                            assertEquals(this.fixer, scanResponse?.threats?.get(index)?.fixable?.fixer)
                            assertEquals(this.target, scanResponse?.threats?.get(index)?.fixable?.target)
                        }
                        this.extension?.apply {
                            assertEquals(this.type, scanResponse?.threats?.get(index)?.extension?.type)
                            assertEquals(this.slug, scanResponse?.threats?.get(index)?.extension?.slug)
                            assertEquals(this.name, scanResponse?.threats?.get(index)?.extension?.name)
                            assertEquals(this.version, scanResponse?.threats?.get(index)?.extension?.version)
                            assertEquals(this.isPremium, scanResponse?.threats?.get(index)?.extension?.isPremium)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun fetchScanState_dispatchesMostRecentStatusForIdleState() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-complete-scan-idle.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNotNull(this.scanStateModel?.mostRecentStatus)
            assertEquals(this.scanStateModel?.state, State.IDLE)
        }
    }

    @Test
    fun fetchScanState_dispatchesEmptyCredentialsWhenServerCredentialsNotSetupForSiteWithScanCapability() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-daily-scan-with-threat-without-server-creds.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertEquals(this.scanStateModel?.credentials, emptyList<Credentials>())
            assertEquals(this.scanStateModel?.state, State.IDLE)
        }
    }

    @Test
    fun fetchScanState_dispatchesEmptyThreatsWhenNoThreatsFoundForSiteWithScanCapability() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-complete-scan-idle.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertEquals(this.scanStateModel?.threats, emptyList<Threat>())
            assertEquals(this.scanStateModel?.state, State.IDLE)
        }
    }

    @Test
    fun fetchScanState_dispatchesCurrentProgressStatusForScanningState() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-complete-scanning.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNotNull(this.scanStateModel?.currentStatus)
            assertEquals(this.scanStateModel?.state, State.SCANNING)
        }
    }

    @Test
    fun fetchScanState_dispatchesReasonForScanUnavailableState() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-daily-backup-scan-unavailable.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNotNull(this.scanStateModel?.reason)
            assertEquals(this.scanStateModel?.state, State.UNAVAILABLE)
        }
    }

    @Test
    fun fetchScanState_dispatchesNullThreatsForScanUnavailableState() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-daily-backup-scan-unavailable.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNull(this.scanStateModel?.threats)
            assertEquals(this.scanStateModel?.state, State.UNAVAILABLE)
        }
    }

    @Test
    fun fetchScanState_dispatchesNullCredentialsForScanUnavailableState() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-daily-backup-scan-unavailable.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)
        initFetchScanState(scanResponse)

        val payload = scanRestClient.fetchScanState(site)

        with(payload) {
            assertNull(this.scanStateModel?.credentials)
            assertEquals(this.scanStateModel?.state, State.UNAVAILABLE)
        }
    }

    @Test
    fun fetchScanState_dispatchesGenericErrorOnFailure() = test {
        initFetchScanState(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = scanRestClient.fetchScanState(site)

        assertEmittedScanStatusError(payload, ScanStateErrorType.GENERIC_ERROR)
    }

    @Test
    fun fetchScanState_dispatchesErrorOnWrongState() = test {
        val successResponseJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass,
            "wp/jetpack/scan/jp-complete-scan-idle.json"
        )
        val scanResponse = getScanStateResponseFromJsonString(successResponseJson)

        initFetchScanState(scanResponse?.copy(state = "wrong"))

        val payload = scanRestClient.fetchScanState(site)

        assertEmittedScanStatusError(payload, ScanStateErrorType.INVALID_RESPONSE)
    }

    private fun assertEmittedScanStatusError(payload: FetchedScanStatePayload, errorType: ScanStateErrorType) {
        with(payload) {
            assertEquals(this.site, site)
            assertTrue(this.isError)
            assertEquals(errorType, this.error.type)
        }
    }

    private fun getScanStateResponseFromJsonString(json: String): ScanStateResponse? {
        val responseType = object : TypeToken<ScanStateResponse>() {}.type
        return Gson().fromJson(json, responseType) as? ScanStateResponse
    }

    private suspend fun initFetchScanState(
        data: ScanStateResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<ScanStateResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error<ScanStateResponse>(error) else Success(nonNullData)
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
        return response
    }
}
