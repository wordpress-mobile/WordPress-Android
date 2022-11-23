package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import com.android.volley.RequestQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackRestClient.JetpackInstallResponse
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class JetpackRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>

    private lateinit var jetpackRestClient: JetpackRestClient
    private val username = "John Smith"
    private val password = "password123"
    private val siteUrl = "http://wordpress.org"

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        jetpackRestClient = JetpackRestClient(dispatcher,
                wpComGsonRequestBuilder,
                null,
                requestQueue,
                accessToken,
                userAgent,
                jetpackTunnelGsonRequestBuilder)
    }

    @Test
    fun `returns success on successful jetpack install`() = test {
        initRequest(Success(JetpackInstallResponse(true)))
        val success = true

        val jetpackInstalledPayload = jetpackRestClient.installJetpack(site)

        checkUrlAndLogin()
        assertThat(jetpackInstalledPayload).isNotNull
        assertThat(jetpackInstalledPayload.success).isEqualTo(success)
    }

    private fun checkUrlAndLogin() {
        val url = "https://public-api.wordpress.com/rest/v1/jetpack-install/http%3A%2F%2Fwordpress.org/"
        assertThat(urlCaptor.lastValue).isEqualTo(url)
        assertThat(paramsCaptor.lastValue).containsEntry("user", username).containsEntry("password", password)
    }

    @Test
    fun `returns error with type`() = test {
        initRequest(Error(WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR))))

        val jetpackErrorPayload = jetpackRestClient.installJetpack(site)

        checkUrlAndLogin()
        assertThat(jetpackErrorPayload).isNotNull
        assertThat(jetpackErrorPayload.success).isEqualTo(false)
        assertThat(jetpackErrorPayload.error?.type).isEqualTo(JetpackInstallErrorType.GENERIC_ERROR)
    }

    suspend fun initRequest(response: WPComGsonRequestBuilder.Response<JetpackInstallResponse>) {
        whenever(site.username).thenReturn(username)
        whenever(site.password).thenReturn(password)
        whenever(site.url).thenReturn(siteUrl)
        whenever(wpComGsonRequestBuilder.syncPostRequest(
                eq(jetpackRestClient),
                urlCaptor.capture(),
                eq(null),
                paramsCaptor.capture(),
                eq(JetpackInstallResponse::class.java),
                isNull()
        )).thenReturn(response)
    }
}
