package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import com.android.volley.RequestQueue
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
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
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackRestClient.JetpackInstallResponse
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstalledPayload

@RunWith(MockitoJUnitRunner::class)
class ActivityLogRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var successMethodCaptor: KArgumentCaptor<(JetpackInstallResponse) -> Unit>
    private lateinit var errorMethodCaptor: KArgumentCaptor<(WPComGsonNetworkError) -> Unit>

    private lateinit var jetpackRestClient: JetpackRestClient
    private val username = "John Smith"
    private val password = "password123"
    private val siteUrl = "http://wordpress.org"

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        successMethodCaptor = argumentCaptor()
        errorMethodCaptor = argumentCaptor()
        jetpackRestClient = JetpackRestClient(dispatcher,
                wpComGsonRequestBuilder,
                null,
                requestQueue,
                accessToken,
                userAgent)
    }

    @Test
    fun `returns success on successful jetpack install`() = runBlocking<Unit> {
        initRequest()
        val success = true

        var jetpackInstalledPayload: JetpackInstalledPayload? = null
        launch { jetpackInstalledPayload = jetpackRestClient.installJetpack(site) }
        while (successMethodCaptor.allValues.isEmpty()) {
            delay(10)
        }
        successMethodCaptor.lastValue.invoke(JetpackInstallResponse(success))
        delay(10)

        checkUrlAndLogin()
        assertThat(jetpackInstalledPayload).isNotNull()
        assertThat(jetpackInstalledPayload?.success).isEqualTo(success)
    }

    fun checkUrlAndLogin() {
        val url = "https://public-api.wordpress.com/rest/v1/jetpack-install/http%3A%2F%2Fwordpress.org/"
        assertThat(urlCaptor.lastValue).isEqualTo(url)
        assertThat(paramsCaptor.lastValue).containsEntry("user", username).containsEntry("password", password)
    }

    @Test
    fun `returns error with type`() = runBlocking<Unit> {
        initRequest()

        var jetpackErrorPayload: JetpackInstalledPayload? = null
        launch { jetpackErrorPayload = jetpackRestClient.installJetpack(site) }
        while (errorMethodCaptor.allValues.isEmpty()) {
            delay(10)
        }
        errorMethodCaptor.lastValue.invoke(WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))
        delay(10)

        checkUrlAndLogin()
        assertThat(jetpackErrorPayload).isNotNull()
        assertThat(jetpackErrorPayload?.success).isEqualTo(false)
        assertThat(jetpackErrorPayload?.error?.type).isEqualTo(JetpackInstallErrorType.GENERIC_ERROR)
    }

    fun initRequest() {
        whenever(site.username).thenReturn(username)
        whenever(site.password).thenReturn(password)
        whenever(site.url).thenReturn(siteUrl)
        whenever(wpComGsonRequestBuilder.buildPostRequest(
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(JetpackInstallResponse::class.java),
                successMethodCaptor.capture(),
                errorMethodCaptor.capture()
        )).thenReturn(mock())
    }
}
